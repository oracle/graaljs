/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.util.List;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.builtins.JSBuiltinsContainer;
import com.oracle.truffle.js.builtins.intl.ListFormatPrototypeBuiltinsFactory.JSListFormatFormatNodeGen;
import com.oracle.truffle.js.builtins.intl.ListFormatPrototypeBuiltinsFactory.JSListFormatFormatToPartsNodeGen;
import com.oracle.truffle.js.builtins.intl.ListFormatPrototypeBuiltinsFactory.JSListFormatResolvedOptionsNodeGen;
import com.oracle.truffle.js.nodes.cast.JSStringListFromIterableNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.intl.JSListFormat;

public final class ListFormatPrototypeBuiltins extends JSBuiltinsContainer.SwitchEnum<ListFormatPrototypeBuiltins.ListFormatPrototype> {

    public static final JSBuiltinsContainer BUILTINS = new ListFormatPrototypeBuiltins();

    protected ListFormatPrototypeBuiltins() {
        super(JSListFormat.PROTOTYPE_NAME, ListFormatPrototype.class);
    }

    public enum ListFormatPrototype implements BuiltinEnum<ListFormatPrototype> {

        resolvedOptions(0),
        format(1),
        formatToParts(1);

        private final int length;

        ListFormatPrototype(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, ListFormatPrototype builtinEnum) {
        switch (builtinEnum) {
            case resolvedOptions:
                return JSListFormatResolvedOptionsNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
            case format:
                return JSListFormatFormatNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case formatToParts:
                return JSListFormatFormatToPartsNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
        }
        return null;
    }

    public abstract static class JSListFormatResolvedOptionsNode extends JSBuiltinNode {

        public JSListFormatResolvedOptionsNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(guards = "isJSListFormat(listFormat)")
        public Object doResolvedOptions(DynamicObject listFormat) {
            return JSListFormat.resolvedOptions(getContext(), getRealm(), listFormat);
        }

        @Fallback
        public void doResolvedOptions(@SuppressWarnings("unused") Object bummer) {
            throw Errors.createTypeErrorTypeXExpected(JSListFormat.CLASS_NAME);
        }
    }

    public abstract static class JSListFormatFormatNode extends JSBuiltinNode {

        public JSListFormatFormatNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(guards = {"isJSListFormat(listFormat)"})
        public String doFormat(DynamicObject listFormat, Object value,
                        @Cached("create(getContext())") JSStringListFromIterableNode strListFromIterableNode) {
            List<String> list = strListFromIterableNode.executeIterable(value);
            return JSListFormat.format(listFormat, list);
        }

        @Fallback
        @SuppressWarnings("unused")
        public void throwTypeError(Object bummer, Object value) {
            throw Errors.createTypeErrorTypeXExpected(JSListFormat.CLASS_NAME);
        }
    }

    public abstract static class JSListFormatFormatToPartsNode extends JSBuiltinNode {

        public JSListFormatFormatToPartsNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(guards = {"isJSListFormat(listFormat)"})
        public Object doFormatToParts(DynamicObject listFormat, Object value,
                        @Cached("create(getContext())") JSStringListFromIterableNode strListFromIterableNode) {
            List<String> list = strListFromIterableNode.executeIterable(value);
            return JSListFormat.formatToParts(getContext(), getRealm(), listFormat, list);
        }

        @Fallback
        @SuppressWarnings("unused")
        public void throwTypeError(Object bummer, Object value) {
            throw Errors.createTypeErrorTypeXExpected(JSListFormat.CLASS_NAME);
        }
    }
}
