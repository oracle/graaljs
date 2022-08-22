/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.exception.AbstractTruffleException;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.nodes.access.CreateIterResultObjectNode;
import com.oracle.truffle.js.nodes.access.HasHiddenKeyCacheNode;
import com.oracle.truffle.js.nodes.access.IteratorCloseNode;
import com.oracle.truffle.js.nodes.access.PropertyGetNode;
import com.oracle.truffle.js.nodes.access.PropertySetNode;
import com.oracle.truffle.js.nodes.function.InternalCallNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSArray;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.objects.IteratorRecord;
import com.oracle.truffle.js.runtime.objects.Undefined;

public class IteratorHelperPrototypeBuiltins extends JSBuiltinsContainer.SwitchEnum<IteratorHelperPrototypeBuiltins.HelperIteratorPrototype> {

    public static final JSBuiltinsContainer BUILTINS = new IteratorHelperPrototypeBuiltins();

    public static final TruffleString PROTOTYPE_NAME = Strings.constant("IteratorHelper.prototype");

    public static final TruffleString CLASS_NAME = Strings.constant("IteratorHelper");

    public static final TruffleString TO_STRING_TAG = Strings.constant("Iterator Helper");


    public static final HiddenKey NEXT_ID = new HiddenKey("next");
    public static final HiddenKey ARGS_ID = new HiddenKey("target");


    protected IteratorHelperPrototypeBuiltins() {
        super(JSArray.ITERATOR_PROTOTYPE_NAME, IteratorHelperPrototypeBuiltins.HelperIteratorPrototype.class);
    }

    public enum HelperIteratorPrototype implements BuiltinEnum<IteratorHelperPrototypeBuiltins.HelperIteratorPrototype> {
        next(1),
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
                return IteratorHelperPrototypeBuiltinsFactory.IteratorHelperNextNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case return_:
                return IteratorHelperPrototypeBuiltinsFactory.IteratorHelperReturnNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
        }
        return null;
    }

    @ImportStatic({IteratorHelperPrototypeBuiltins.class, IteratorPrototypeBuiltins.class, JSFunction.GeneratorState.class})
    public abstract static class IteratorHelperReturnNode extends JSBuiltinNode {
        @Child private PropertyGetNode getTargetNode;
        @Child private PropertyGetNode getGeneratorStateNode;
        @Child private PropertySetNode setGeneratorStateNode;
        @Child private IteratorCloseNode iteratorCloseNode;
        @Child private CreateIterResultObjectNode createIterResultObjectNode;
        @Child private HasHiddenKeyCacheNode hasNextImplNode;
        @Child private HasHiddenKeyCacheNode hasInnerNode;


        protected IteratorHelperReturnNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);

            getTargetNode = PropertyGetNode.create(ARGS_ID, context);
            getGeneratorStateNode = PropertyGetNode.createGetHidden(JSFunction.GENERATOR_STATE_ID, context);
            setGeneratorStateNode = PropertySetNode.createSetHidden(JSFunction.GENERATOR_STATE_ID, context);
            iteratorCloseNode = IteratorCloseNode.create(context);
            createIterResultObjectNode = CreateIterResultObjectNode.create(context);
            hasNextImplNode = HasHiddenKeyCacheNode.create(ARGS_ID);

            hasInnerNode = HasHiddenKeyCacheNode.create(IteratorPrototypeBuiltins.FLATMAP_ALIVE_ID);
        }

        protected boolean hasImpl(Object thisObj) {
            return hasNextImplNode.executeHasHiddenKey(thisObj);
        }
        protected boolean hasInner(Object thisObj) {
            return hasInnerNode.executeHasHiddenKey(thisObj);
        }
        protected JSFunction.GeneratorState getState(Object thisObject) {
            return (JSFunction.GeneratorState) getGeneratorStateNode.getValue(thisObject);
        }

        @Specialization(guards = {"hasImpl(thisObj)", "getState(thisObj) == Executing"})
        public Object executing(Object thisObj) {
            throw Errors.createTypeError("generator is already executing");
        }
        @Specialization(guards = {"hasImpl(thisObj)", "getState(thisObj) == SuspendedStart"})
        public Object suspendedStart(VirtualFrame frame, Object thisObj) {
            setGeneratorStateNode.setValue(thisObj, JSFunction.GeneratorState.Completed);
            return createIterResultObjectNode.execute(frame, Undefined.instance, true);
        }

        @Specialization(guards = {"hasImpl(thisObj)", "hasInner(thisObj)"})
        public Object closeInner(VirtualFrame frame, Object thisObj,
                                 @Cached("createGetHidden(FLATMAP_ALIVE_ID, getContext())") PropertyGetNode isAliveNode,
                                 @Cached("createGetHidden(FLATMAP_INNER_ID, getContext())") PropertyGetNode getInnerNode,
                                 @Cached("create()") BranchProfile aliveProfile) {
            setGeneratorStateNode.setValue(thisObj, JSFunction.GeneratorState.Executing);

            try {
                if (isAliveNode.getValueBoolean(thisObj)) {
                    IteratorRecord iterated = (IteratorRecord) getInnerNode.getValue(thisObj);
                    iteratorCloseNode.executeAbrupt(iterated.getIterator());
                } else {
                    aliveProfile.enter();
                }
            } catch (UnexpectedResultException ex) {
                throw Errors.shouldNotReachHere(ex);
            }
            return close(frame, thisObj);
        }

        @Specialization(guards = {"hasImpl(thisObj)", "!hasInner(thisObj)"})
        public Object close(VirtualFrame frame, Object thisObj) {
            setGeneratorStateNode.setValue(thisObj, JSFunction.GeneratorState.Executing);

            IteratorRecord iterated = ((IteratorPrototypeBuiltins.IteratorArgs) getTargetNode.getValue(thisObj)).target;

            try {
                iteratorCloseNode.executeVoid(iterated.getIterator());
            } finally {
                setGeneratorStateNode.setValue(thisObj, JSFunction.GeneratorState.Completed);
            }
            return createIterResultObjectNode.execute(frame, Undefined.instance, true);
        }


        @Specialization(guards = "!hasImpl(thisObj)")
        public Object unsupported(Object thisObj) {
            throw Errors.createTypeErrorIncompatibleReceiver(thisObj);
        }
    }

    public abstract static class IteratorHelperNextNode extends JSBuiltinNode {
        @Child private PropertyGetNode getDoneNode;
        @Child private PropertyGetNode getNextImplNode;
        @Child private PropertyGetNode getGeneratorStateNode;
        @Child private PropertySetNode setGeneratorStateNode;
        @Child private HasHiddenKeyCacheNode hasNextImplNode;
        @Child private CreateIterResultObjectNode createIterResultObjectNode;
        @Child private InternalCallNode callNode;

        protected IteratorHelperNextNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);

            getGeneratorStateNode = PropertyGetNode.createGetHidden(JSFunction.GENERATOR_STATE_ID, context);
            setGeneratorStateNode = PropertySetNode.createSetHidden(JSFunction.GENERATOR_STATE_ID, context);
            getNextImplNode = PropertyGetNode.createGetHidden(NEXT_ID, context);
            hasNextImplNode = HasHiddenKeyCacheNode.create(NEXT_ID);
            getDoneNode = PropertyGetNode.create(Strings.DONE, false, context);
            callNode = InternalCallNode.create();
        }

        protected boolean hasImpl(Object thisObj) {
            return hasNextImplNode.executeHasHiddenKey(thisObj);
        }

        @Specialization(guards = "hasImpl(thisObj)")
        public Object next(VirtualFrame frame, Object thisObj,
                           @Cached("create()") BranchProfile executingProfile,
                           @Cached("create()") BranchProfile completedProfile) {
            Object state = getGeneratorStateNode.getValue(thisObj);
            if (state == JSFunction.GeneratorState.Executing) {
                executingProfile.enter();
                throw Errors.createTypeError("generator is already executing");
            }
            if (state == JSFunction.GeneratorState.Completed) {
                completedProfile.enter();
                if (createIterResultObjectNode == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    createIterResultObjectNode = insert(CreateIterResultObjectNode.create(getContext()));
                }
                return createIterResultObjectNode.execute(frame, Undefined.instance, true);
            }

            setGeneratorStateNode.setValue(thisObj, JSFunction.GeneratorState.Executing);

            Object result;
            try {
                result = callNode.execute((CallTarget) getNextImplNode.getValue(thisObj), frame.getArguments());
            } catch (AbstractTruffleException ex) {
                setGeneratorStateNode.setValue(thisObj, JSFunction.GeneratorState.Completed);
                throw ex;
            }

            try {
                if (getDoneNode.getValueBoolean(result)) {
                    setGeneratorStateNode.setValue(thisObj, JSFunction.GeneratorState.Completed);
                } else {
                    setGeneratorStateNode.setValue(thisObj, JSFunction.GeneratorState.SuspendedYield);
                }
            } catch (UnexpectedResultException e) {
                throw Errors.shouldNotReachHere();
            }

            return result;
        }

        @Specialization(guards = "!hasImpl(thisObj)")
        public Object unsupported(Object thisObj) {
            throw Errors.createTypeErrorIncompatibleReceiver(thisObj);
        }
    }
}
