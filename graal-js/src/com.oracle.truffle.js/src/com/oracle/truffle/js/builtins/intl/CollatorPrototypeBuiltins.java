/*
 * Copyright (c) 2018, 2025, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.builtins.intl;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.api.profiles.InlinedBranchProfile;
import com.oracle.truffle.js.builtins.JSBuiltinsContainer;
import com.oracle.truffle.js.builtins.intl.CollatorPrototypeBuiltinsFactory.JSCollatorGetCompareNodeGen;
import com.oracle.truffle.js.builtins.intl.CollatorPrototypeBuiltinsFactory.JSCollatorResolvedOptionsNodeGen;
import com.oracle.truffle.js.nodes.access.PropertyGetNode;
import com.oracle.truffle.js.nodes.access.PropertySetNode;
import com.oracle.truffle.js.nodes.cast.JSToStringNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JavaScriptRootNode;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSFunctionData;
import com.oracle.truffle.js.runtime.builtins.intl.JSCollator;
import com.oracle.truffle.js.runtime.builtins.intl.JSCollatorObject;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.Undefined;

public final class CollatorPrototypeBuiltins extends JSBuiltinsContainer.SwitchEnum<CollatorPrototypeBuiltins.CollatorPrototype> {

    public static final JSBuiltinsContainer BUILTINS = new CollatorPrototypeBuiltins();

    protected CollatorPrototypeBuiltins() {
        super(JSCollator.PROTOTYPE_NAME, CollatorPrototype.class);
    }

    public enum CollatorPrototype implements BuiltinEnum<CollatorPrototype> {

        resolvedOptions(0),
        compare(0);

        private final int length;

        CollatorPrototype(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }

        @Override
        public boolean isGetter() {
            return this == compare;
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, CollatorPrototype builtinEnum) {
        switch (builtinEnum) {
            case resolvedOptions:
                return JSCollatorResolvedOptionsNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
            case compare:
                return JSCollatorGetCompareNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
        }
        return null;
    }

    public abstract static class JSCollatorResolvedOptionsNode extends JSBuiltinNode {

        public JSCollatorResolvedOptionsNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        public Object doResolvedOptions(JSCollatorObject collator) {
            return JSCollator.resolvedOptions(getContext(), getRealm(), collator);
        }

        @Fallback
        public Object doIncompatibleReceiver(@SuppressWarnings("unused") Object bummer) {
            throw Errors.createTypeErrorTypeXExpected(JSCollator.CLASS_NAME);
        }
    }

    public abstract static class JSCollatorGetCompareNode extends JSBuiltinNode {

        static final HiddenKey BOUND_OBJECT_KEY = new HiddenKey(Strings.toJavaString(JSCollator.CLASS_NAME));

        @Child private PropertySetNode setBoundObjectNode;

        public JSCollatorGetCompareNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
            this.setBoundObjectNode = PropertySetNode.createSetHidden(BOUND_OBJECT_KEY, context);
        }

        @Specialization
        public Object doCollator(JSCollatorObject collatorObj,
                        @Cached InlinedBranchProfile errorBranch) {
            JSCollator.InternalState state = collatorObj.getInternalState();

            if (state == null || !state.isInitializedCollator()) {
                errorBranch.enter(this);
                throw Errors.createTypeErrorMethodCalledOnNonObjectOrWrongType("compare");
            }

            if (state.getBoundCompareFunction() == null) {
                JSFunctionData compareFunctionData = getContext().getOrCreateBuiltinFunctionData(JSContext.BuiltinFunctionKey.CollatorCompare, c -> createCompareFunctionData(c));
                JSDynamicObject compareFn = JSFunction.create(getRealm(), compareFunctionData);
                setBoundObjectNode.setValue(compareFn, collatorObj);
                state.setBoundCompareFunction(compareFn);
            }

            return state.getBoundCompareFunction();
        }

        @Fallback
        public Object doIncompatibleReceiver(@SuppressWarnings("unused") Object bummer) {
            throw Errors.createTypeErrorTypeXExpected(JSCollator.CLASS_NAME);
        }

        private static JSFunctionData createCompareFunctionData(JSContext context) {
            return JSFunctionData.createCallOnly(context, new JavaScriptRootNode(context.getLanguage(), null, null) {
                @Child private PropertyGetNode getBoundObjectNode = PropertyGetNode.createGetHidden(BOUND_OBJECT_KEY, context);
                @Child private JSToStringNode toString1Node = JSToStringNode.create();
                @Child private JSToStringNode toString2Node = JSToStringNode.create();

                @Override
                public Object execute(VirtualFrame frame) {
                    Object[] arguments = frame.getArguments();
                    JSCollatorObject thisObj = (JSCollatorObject) getBoundObjectNode.getValue(JSArguments.getFunctionObject(arguments));
                    int argumentCount = JSArguments.getUserArgumentCount(arguments);
                    String one = Strings.toJavaString((argumentCount > 0) ? toString1Node.executeString(JSArguments.getUserArgument(arguments, 0)) : Undefined.NAME);
                    String two = Strings.toJavaString((argumentCount > 1) ? toString2Node.executeString(JSArguments.getUserArgument(arguments, 1)) : Undefined.NAME);
                    return JSCollator.compare(thisObj, one, two);
                }
            }.getCallTarget(), 2, Strings.EMPTY_STRING);
        }
    }
}
