/*
 * Copyright (c) 2022, 2025, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The Universal Permissive License (UPL), Version 1.0
 *
 * Subject to the condition set forth below, permission is hereby granted to any
 * person obtaining a copy of this software, associated documentation and/or
 * data (collectively the "Software"), free of charge and under any and all
 * copyright rights in the Software, and any and all patent rights owned or
 * freely licensable by each licensor hereunder covering either (i) the
 * unmodified Software as contributed to or provided by such licensor, or (ii)
 * the Larger Works (as defined below), to deal in both
 *
 * (a) the Software, and
 *
 * (b) any piece of software and/or hardware listed in the lrgrwrks.txt file if
 * one is included with the Software each a "Larger Work" to which the Software
 * is contributed by such licensors),
 *
 * without restriction, including without limitation the rights to copy, create
 * derivative works of, display, perform, and distribute the Software and make,
 * use, sell, offer for sale, import, export, have made, and have sold the
 * Software and the Larger Work(s), and to sublicense the foregoing rights on
 * either these or other terms.
 *
 * This license is subject to the following condition:
 *
 * The above copyright notice and either this complete permission notice or at a
 * minimum a reference to the UPL must be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.oracle.truffle.js.builtins;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Exclusive;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.exception.AbstractTruffleException;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.InlinedBranchProfile;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.nodes.access.CreateIterResultObjectNode;
import com.oracle.truffle.js.nodes.access.IteratorCloseNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSIteratorHelperObject;
import com.oracle.truffle.js.runtime.objects.IteratorRecord;
import com.oracle.truffle.js.runtime.objects.Undefined;

public class IteratorHelperPrototypeBuiltins extends JSBuiltinsContainer.SwitchEnum<IteratorHelperPrototypeBuiltins.HelperIteratorPrototype> {

    public static final JSBuiltinsContainer BUILTINS = new IteratorHelperPrototypeBuiltins();

    public static final TruffleString PROTOTYPE_NAME = Strings.constant("IteratorHelper.prototype");

    public static final TruffleString TO_STRING_TAG = Strings.constant("Iterator Helper");

    protected IteratorHelperPrototypeBuiltins() {
        super(PROTOTYPE_NAME, IteratorHelperPrototypeBuiltins.HelperIteratorPrototype.class);
    }

    public enum HelperIteratorPrototype implements BuiltinEnum<IteratorHelperPrototypeBuiltins.HelperIteratorPrototype> {
        next(0),
        return_(0);

        private final int length;

        HelperIteratorPrototype(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, IteratorHelperPrototypeBuiltins.HelperIteratorPrototype builtinEnum) {
        switch (builtinEnum) {
            case next:
                return IteratorHelperPrototypeBuiltinsFactory.IteratorHelperNextNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
            case return_:
                return IteratorHelperPrototypeBuiltinsFactory.IteratorHelperReturnNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
        }
        return null;
    }

    protected static boolean isJSIteratorHelper(Object object) {
        return object instanceof JSIteratorHelperObject;
    }

    @ImportStatic({IteratorHelperPrototypeBuiltins.class, IteratorPrototypeBuiltins.class, JSFunction.GeneratorState.class})
    public abstract static class IteratorHelperReturnNode extends JSBuiltinNode {
        @Child private CreateIterResultObjectNode createIterResultObjectNode;

        protected IteratorHelperReturnNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
            this.createIterResultObjectNode = CreateIterResultObjectNode.create(context);
        }

        @Specialization(guards = {"thisObj.getGeneratorState() == Executing"})
        public Object executing(@SuppressWarnings("unused") JSIteratorHelperObject thisObj) {
            throw Errors.createTypeError("generator is already executing");
        }

        @Specialization(guards = {"thisObj.getGeneratorState() == SuspendedStart"})
        public Object suspendedStart(VirtualFrame frame, JSIteratorHelperObject thisObj,
                        @Cached("create(getContext())") @Shared IteratorCloseNode outerIteratorCloseNode) {
            thisObj.setGeneratorState(JSFunction.GeneratorState.Completed);
            var args = thisObj.getIteratorArgs();
            IteratorRecord outerIterator = args.iterated;
            if (outerIterator != null) {
                outerIteratorCloseNode.executeVoid(outerIterator.getIterator());
            }
            return createIterResultObjectNode.execute(frame, Undefined.instance, true);
        }

        @Specialization(guards = {"thisObj.getGeneratorState() == SuspendedYield"})
        public Object suspendedYield(VirtualFrame frame, JSIteratorHelperObject thisObj,
                        @Cached("create(getContext())") @Shared IteratorCloseNode outerIteratorCloseNode,
                        @Cached("create(getContext())") @Exclusive IteratorCloseNode innerIteratorCloseNode) {
            thisObj.setGeneratorState(JSFunction.GeneratorState.Executing);

            try {
                var args = thisObj.getIteratorArgs();
                IteratorRecord outerIterator = args.iterated;
                IteratorRecord innerIterator = null;

                if (args instanceof IteratorFunctionBuiltins.ConcatArgs concatArgs) {
                    assert concatArgs.innerAlive;
                    innerIterator = concatArgs.innerIterator;
                } else if (args instanceof IteratorPrototypeBuiltins.IteratorFlatMapNode.IteratorFlatMapArgs flatMapArgs) {
                    assert flatMapArgs.innerAlive;
                    innerIterator = flatMapArgs.innerIterator;
                }

                if (innerIterator != null) {
                    try {
                        innerIteratorCloseNode.executeVoid(innerIterator.getIterator());
                    } catch (AbstractTruffleException e) {
                        if (outerIterator != null) {
                            outerIteratorCloseNode.executeAbrupt(outerIterator.getIterator());
                        }
                        throw e;
                    }
                }

                if (outerIterator != null) {
                    outerIteratorCloseNode.executeVoid(outerIterator.getIterator());
                }
            } finally {
                thisObj.setGeneratorState(JSFunction.GeneratorState.Completed);
            }
            return createIterResultObjectNode.execute(frame, Undefined.instance, true);
        }

        @Specialization(guards = {"thisObj.getGeneratorState() == Completed"})
        public Object completed(VirtualFrame frame, @SuppressWarnings("unused") JSIteratorHelperObject thisObj) {
            return createIterResultObjectNode.execute(frame, Undefined.instance, true);
        }

        @Specialization(guards = "!isJSIteratorHelper(thisObj)")
        protected final Object unsupported(Object thisObj) {
            throw Errors.createTypeErrorIncompatibleReceiver(getBuiltin().getName(), thisObj);
        }
    }

    @ImportStatic({IteratorHelperPrototypeBuiltins.class})
    public abstract static class IteratorHelperNextNode extends JSBuiltinNode {
        @Child private CreateIterResultObjectNode createIterResultObjectNode;
        @Child private JSFunctionCallNode callNode;

        protected IteratorHelperNextNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);

            callNode = JSFunctionCallNode.createCall();
        }

        @Specialization
        public Object next(VirtualFrame frame, JSIteratorHelperObject thisObj,
                        @Cached InlinedBranchProfile executingProfile,
                        @Cached InlinedBranchProfile completedProfile) {
            var state = thisObj.getGeneratorState();
            if (state == JSFunction.GeneratorState.Executing) {
                executingProfile.enter(this);
                throw Errors.createTypeError("generator is already executing");
            }
            if (state == JSFunction.GeneratorState.Completed) {
                completedProfile.enter(this);
                if (createIterResultObjectNode == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    createIterResultObjectNode = insert(CreateIterResultObjectNode.create(getContext()));
                }
                return createIterResultObjectNode.execute(frame, Undefined.instance, true);
            }

            thisObj.setGeneratorState(JSFunction.GeneratorState.Executing);

            try {
                Object next = thisObj.getNextImpl();
                return callNode.executeCall(JSArguments.createZeroArg(thisObj, next));
            } catch (AbstractTruffleException ex) {
                thisObj.setGeneratorState(JSFunction.GeneratorState.Completed);
                throw ex;
            }
        }

        @Specialization(guards = "!isJSIteratorHelper(thisObj)")
        protected final Object unsupported(Object thisObj) {
            throw Errors.createTypeErrorIncompatibleReceiver(getBuiltin().getName(), thisObj);
        }

        @Override
        public boolean isSplitImmediately() {
            return true;
        }
    }
}
