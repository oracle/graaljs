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
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.nodes.access.CreateIterResultObjectNode;
import com.oracle.truffle.js.nodes.access.GetMethodNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSWrapForValidIterator;
import com.oracle.truffle.js.runtime.builtins.JSWrapForValidIteratorObject;
import com.oracle.truffle.js.runtime.objects.IteratorRecord;
import com.oracle.truffle.js.runtime.objects.Undefined;

public final class WrapForValidIteratorPrototypeBuiltins extends JSBuiltinsContainer.SwitchEnum<WrapForValidIteratorPrototypeBuiltins.WrapForIterator> {
    public static final JSBuiltinsContainer BUILTINS = new WrapForValidIteratorPrototypeBuiltins();

    public static final TruffleString PROTOTYPE_NAME = Strings.constant("%WrapForValidIteratorPrototype%");

    protected WrapForValidIteratorPrototypeBuiltins() {
        super(PROTOTYPE_NAME, WrapForIterator.class);
    }

    public enum WrapForIterator implements BuiltinEnum<WrapForIterator> {
        next(0),
        return_(0);

        private final int length;

        WrapForIterator(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, WrapForIterator builtinEnum) {
        switch (builtinEnum) {
            case next:
                return WrapForValidIteratorPrototypeBuiltinsFactory.WrapForIteratorNextNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
            case return_:
                return WrapForValidIteratorPrototypeBuiltinsFactory.WrapForIteratorReturnNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
        }
        return null;
    }

    @ImportStatic({JSWrapForValidIterator.class})
    public abstract static class WrapForIteratorNextNode extends JSBuiltinNode {
        @Child private JSFunctionCallNode callNode;

        public WrapForIteratorNextNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);

            callNode = JSFunctionCallNode.createCall();
        }

        @Specialization
        protected Object next(JSWrapForValidIteratorObject thisObj) {
            IteratorRecord iterated = thisObj.getIterated();
            return callNode.executeCall(JSArguments.createZeroArg(iterated.getIterator(), iterated.getNextMethod()));
        }

        @Specialization(guards = "!isWrapForIterator(thisObj)")
        protected final Object incompatible(Object thisObj) {
            throw Errors.createTypeErrorIncompatibleReceiver(getBuiltin().getName(), thisObj);
        }
    }

    @ImportStatic({JSWrapForValidIterator.class, Strings.class})
    public abstract static class WrapForIteratorReturnNode extends JSBuiltinNode {

        public WrapForIteratorReturnNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected Object doReturn(JSWrapForValidIteratorObject thisObj,
                        @Cached("create(getContext(), RETURN)") GetMethodNode getReturnNode,
                        @Cached("createCall()") JSFunctionCallNode methodCallNode,
                        @Cached("create(getContext())") CreateIterResultObjectNode createIterResultObjectNode) {
            Object returnMethod = getReturnNode.executeWithTarget(thisObj.getIterated().getIterator());
            if (returnMethod == Undefined.instance) {
                return createIterResultObjectNode.execute(Undefined.instance, true);
            } else {
                return methodCallNode.executeCall(JSArguments.createZeroArg(thisObj.getIterated().getIterator(), returnMethod));
            }
        }

        @Specialization(guards = "!isWrapForIterator(thisObj)")
        protected final Object incompatible(Object thisObj) {
            throw Errors.createTypeErrorIncompatibleReceiver(getBuiltin().getName(), thisObj);
        }
    }
}
