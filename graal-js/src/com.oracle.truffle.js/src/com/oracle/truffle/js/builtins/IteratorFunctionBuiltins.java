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

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.InlinedBranchProfile;
import com.oracle.truffle.js.builtins.IteratorPrototypeBuiltins.IteratorArgs;
import com.oracle.truffle.js.builtins.IteratorPrototypeBuiltins.IteratorFromGeneratorNode;
import com.oracle.truffle.js.nodes.access.GetIteratorFlattenableNode;
import com.oracle.truffle.js.nodes.access.GetIteratorFromMethodNode;
import com.oracle.truffle.js.nodes.access.GetMethodNode;
import com.oracle.truffle.js.nodes.access.IsObjectNode;
import com.oracle.truffle.js.nodes.access.IteratorCompleteNode;
import com.oracle.truffle.js.nodes.access.IteratorNextNode;
import com.oracle.truffle.js.nodes.access.IteratorValueNode;
import com.oracle.truffle.js.nodes.binary.InstanceofNode.OrdinaryHasInstanceNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSIterator;
import com.oracle.truffle.js.runtime.builtins.JSIteratorHelperObject;
import com.oracle.truffle.js.runtime.builtins.JSWrapForValidIterator;
import com.oracle.truffle.js.runtime.objects.IteratorRecord;
import com.oracle.truffle.js.runtime.objects.Undefined;

/**
 * Contains builtins for {@linkplain JSIterator} function (constructor).
 */
public final class IteratorFunctionBuiltins extends JSBuiltinsContainer.SwitchEnum<IteratorFunctionBuiltins.IteratorFunction> {

    public static final JSBuiltinsContainer BUILTINS = new IteratorFunctionBuiltins();

    IteratorFunctionBuiltins() {
        super(JSIterator.CLASS_NAME, IteratorFunction.class);
    }

    public enum IteratorFunction implements BuiltinEnum<IteratorFunction> {
        from(1),

        concat(0);

        private final int length;

        IteratorFunction(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }

        @Override
        public int getECMAScriptVersion() {
            if (this == concat) {
                return JSConfig.StagingECMAScriptVersion;
            }
            return BuiltinEnum.super.getECMAScriptVersion();
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, IteratorFunction builtinEnum) {
        switch (builtinEnum) {
            case from:
                return IteratorFunctionBuiltinsFactory.JSIteratorFromNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
            case concat:
                return IteratorFunctionBuiltinsFactory.IteratorConcatNodeGen.create(context, builtin, args().varArgs().createArgumentNodes(context));
        }

        return null;
    }

    public abstract static class JSIteratorFromNode extends JSBuiltinNode {
        @Child private GetIteratorFlattenableNode getIteratorFlattenableNode;

        @Child private OrdinaryHasInstanceNode ordinaryHasInstanceNode;

        protected JSIteratorFromNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
            this.getIteratorFlattenableNode = GetIteratorFlattenableNode.create(false, false, context);
            this.ordinaryHasInstanceNode = OrdinaryHasInstanceNode.create(context);
        }

        @Specialization
        protected Object iteratorFrom(Object arg) {
            IteratorRecord iteratorRecord = getIteratorFlattenableNode.execute(arg);

            JSRealm realm = getRealm();
            boolean hasInstance = ordinaryHasInstanceNode.executeBoolean(iteratorRecord.getIterator(), realm.getIteratorConstructor());
            if (hasInstance) {
                return iteratorRecord.getIterator();
            }

            return JSWrapForValidIterator.create(getContext(), realm, iteratorRecord);
        }

    }

    private record Iterable(Object openMethod, Object iterable) {
    }

    static final class ConcatArgs extends IteratorArgs {

        private final Iterable[] iterables;
        private int iterableIndex;
        boolean innerAlive;
        IteratorRecord innerIterator;

        ConcatArgs(Iterable[] iterables) {
            super(null);
            this.iterables = iterables;
        }
    }

    @ImportStatic({Symbol.class})
    public abstract static class IteratorConcatNode extends IteratorFromGeneratorNode<ConcatArgs> {

        protected IteratorConcatNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin, JSContext.BuiltinFunctionKey.IteratorConcat,
                            c -> createIteratorFromGeneratorFunctionImpl(c, IteratorConcatNextNode.create(c)));
        }

        @Specialization
        protected final Object iteratorConcat(Object[] items,
                        @Cached IsObjectNode isObjectNode,
                        @Cached(parameters = {"getContext()", "SYMBOL_ITERATOR"}) GetMethodNode getIteratorMethodNode,
                        @Cached InlinedBranchProfile errorBranch) {
            Iterable[] iterables = new Iterable[items.length];
            for (int i = 0; i < items.length; i++) {
                Object item = items[i];
                if (!isObjectNode.executeBoolean(item)) {
                    errorBranch.enter(this);
                    throw Errors.createTypeErrorNotAnObject(item);
                }
                Object method = getIteratorMethodNode.executeWithTarget(item);
                if (method == Undefined.instance) {
                    errorBranch.enter(this);
                    throw Errors.createTypeErrorNotIterable(item, this);
                }
                iterables[i] = new Iterable(method, item);
            }
            return createIteratorHelperObject(new ConcatArgs(iterables));
        }
    }

    protected abstract static class IteratorConcatNextNode extends IteratorFromGeneratorNode.IteratorFromGeneratorImplNode<ConcatArgs> {

        protected IteratorConcatNextNode(JSContext context) {
            super(context);
        }

        @Specialization
        protected final Object next(VirtualFrame frame, JSIteratorHelperObject thisObj,
                        @Cached GetIteratorFromMethodNode getIteratorFromMethodNode,
                        @Cached IteratorNextNode iteratorNextNode,
                        @Cached IteratorCompleteNode iteratorCompleteNode,
                        @Cached IteratorValueNode iteratorValueNode) {
            ConcatArgs args = getArgs(thisObj);
            var iterables = args.iterables;
            int iterableIndex = args.iterableIndex;

            while (iterableIndex < iterables.length) {
                IteratorRecord iterator = args.innerIterator;
                if (!args.innerAlive) {
                    var iterable = iterables[iterableIndex];
                    args.innerIterator = iterator = getIteratorFromMethodNode.execute(this, iterable.iterable, iterable.openMethod);
                    args.innerAlive = true;
                }

                assert args.innerAlive && iterator != null;
                Object result = iteratorNextNode.execute(iterator);
                boolean done = iteratorCompleteNode.execute(result);
                if (done) {
                    iteratorValueNode.execute(result);
                    args.innerAlive = false;
                    args.innerIterator = null;
                    args.iterableIndex = ++iterableIndex;
                } else {
                    return generatorYield(thisObj, result);
                    // Note: Abrupt completion is handled by IteratorHelperReturnNode.
                }
            }
            return createResultDone(frame, thisObj);
        }

        @Override
        public IteratorFromGeneratorNode.IteratorFromGeneratorImplNode<ConcatArgs> copyUninitialized() {
            return create(context);
        }

        static IteratorConcatNextNode create(JSContext context) {
            return IteratorFunctionBuiltinsFactory.IteratorConcatNextNodeGen.create(context);
        }
    }
}
