/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.builtins.simd;

import java.util.ArrayList;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.builtins.JSBuiltinsContainer;
import com.oracle.truffle.js.builtins.simd.SIMDTypeFunctionBuiltins.JSBasicSimdOperation;
import com.oracle.truffle.js.builtins.simd.SIMDTypePrototypeBuiltinsFactory.SIMDToLocaleStringNodeGen;
import com.oracle.truffle.js.builtins.simd.SIMDTypePrototypeBuiltinsFactory.SIMDToStringNodeGen;
import com.oracle.truffle.js.builtins.simd.SIMDTypePrototypeBuiltinsFactory.SIMDValueOfNodeGen;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.access.PropertyGetNode;
import com.oracle.truffle.js.nodes.cast.JSToStringNode;
import com.oracle.truffle.js.nodes.cast.JSToStringNodeGen;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSSIMD;
import com.oracle.truffle.js.runtime.objects.Undefined;

public final class SIMDTypePrototypeBuiltins extends JSBuiltinsContainer.SwitchEnum<SIMDTypePrototypeBuiltins.SIMDTypePrototype> {

    public SIMDTypePrototypeBuiltins() {
        super(JSSIMD.PROTOTYPE_NAME, SIMDTypePrototype.class);
    }

    public enum SIMDTypePrototype implements BuiltinEnum<SIMDTypePrototype> {
        valueOf(0),
        toLocaleString(0),
        toString(0);

        private final int length;

        SIMDTypePrototype(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, SIMDTypePrototype builtinEnum) {
        switch (builtinEnum) {
            case valueOf:
                return SIMDValueOfNode.create(context, builtin, args().withThis().createArgumentNodes(context));
            case toLocaleString:
                return SIMDToLocaleStringNode.create(context, builtin, args().varArgs().withThis().createArgumentNodes(context));
            case toString:
                return SIMDToStringNode.create(context, builtin, args().withThis().createArgumentNodes(context));
        }
        return null;
    }

    public abstract static class SIMDToStringNode extends JSBasicSimdOperation {
        @Child private JSToStringNode toStringNode;

        protected SIMDToStringNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin, null);
        }

        public static SIMDToStringNode create(JSContext context, JSBuiltin builtin, JavaScriptNode[] createArgumentNodes) {
            return SIMDToStringNodeGen.create(context, builtin, createArgumentNodes);
        }

        protected final String toString(Object target) {
            if (toStringNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                toStringNode = insert(JSToStringNodeGen.create());
            }
            return toStringNode.executeString(target);
        }

        // 5.1.29 ArrayJoin( array, separator )
        // ArrayJoin is used only in the toString methods and by using list a conversion to array
        // can be saved.
        @TruffleBoundary
        public String arrayJoin(DynamicObject simd, Object separator) {
            long len = JSSIMD.simdTypeGetSIMDType(simd).getNumberOfElements();

            String sep = JSRuntime.toString(separator == null ? ',' : separator);
            if (len == 0) {
                return "";
            }

            Object[] simdArray = (Object[]) JSSIMD.simdGetArray(simd, JSSIMD.isJSSIMD(simd));
            StringBuilder sb = new StringBuilder();
            Object element0 = simdArray[0];

            if (element0 != null && element0 != Undefined.instance) {
                sb.append(JSRuntime.toString(element0)); // toString(frame, element0));
            }

            for (int k = 1; k < len; k++) {
                sb.append(sep);
                Object element = simdArray[k];
                if (element != null && element != Undefined.instance) {
                    sb.append(JSRuntime.toString(element)); // ;toString(frame, element));
                }
            }
            return sb.toString();
        }

        @Specialization
        protected Object doToString(@SuppressWarnings("unused") VirtualFrame frame, DynamicObject a) {
            String t = JSSIMD.simdTypeGetSIMDType(a).getFactory().getName();
            String e = arrayJoin(a, ",");
            return "SIMD." + t + "(" + e + ")";
        }
    }

    public abstract static class SIMDToLocaleStringNode extends SIMDToStringNode {
        @Child private JSToStringNode toStringNode;

        protected SIMDToLocaleStringNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        public static SIMDToLocaleStringNode create(JSContext context, JSBuiltin builtin, JavaScriptNode[] createArgumentNodes) {
            return SIMDToLocaleStringNodeGen.create(context, builtin, createArgumentNodes);
        }

        private JSFunctionCallNode callToLocaleString;
        private PropertyGetNode getToLocaleString;

        @SuppressWarnings("unused")
        private Object callToLocaleString(VirtualFrame frame, Object nextElement) {
            if (getToLocaleString == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getToLocaleString = insert(PropertyGetNode.create("toLocaleString", false, getContext()));
                callToLocaleString = insert(JSFunctionCallNode.create(false));
            }

            Object toLocaleString = getToLocaleString.getValue(nextElement);

            if (!JSFunction.isJSFunction(toLocaleString)) {
                return nextElement;
            }
            return callToLocaleString.executeCall(JSArguments.create(nextElement, toLocaleString));
        }

        @Override
        @Specialization
        protected Object doToString(VirtualFrame frame, DynamicObject a) {
            String separator = ", "; // TODO use locale
            ArrayList<Object> list = new ArrayList<>();

            for (int i = 0; i < JSSIMD.simdTypeGetSIMDType(a).getNumberOfElements(); i++) {
                String r = toString(callToLocaleString(frame, getLane(a, i)));
                list.add(r);
            }

            String t = JSSIMD.simdTypeGetSIMDType(a).getFactory().getName();
            String e = arrayJoin(a, separator);
            return doToStringIntl(t, e);
        }

        @TruffleBoundary
        private static String doToStringIntl(String t, String e) {
            return "SIMD." + t + "(" + e + ")";
        }
    }

    public abstract static class SIMDValueOfNode extends JSBasicSimdOperation {

        public SIMDValueOfNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin, null);
        }

        public static SIMDValueOfNode create(JSContext context, JSBuiltin builtin, JavaScriptNode[] createArgumentNodes) {
            return SIMDValueOfNodeGen.create(context, builtin, createArgumentNodes);
        }

        @Specialization
        protected Object doToString(DynamicObject a) {
            if (!JSSIMD.isJSSIMD(a)) {
                throw Errors.createTypeError("");
            }
            return a;
        }
    }
}
