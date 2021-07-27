/*
 * Copyright (c) 2019, 2021, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.builtins.JSBuiltinsContainer;
import com.oracle.truffle.js.builtins.intl.RelativeTimeFormatPrototypeBuiltinsFactory.JSRelativeTimeFormatFormatNodeGen;
import com.oracle.truffle.js.builtins.intl.RelativeTimeFormatPrototypeBuiltinsFactory.JSRelativeTimeFormatFormatToPartsNodeGen;
import com.oracle.truffle.js.builtins.intl.RelativeTimeFormatPrototypeBuiltinsFactory.JSRelativeTimeFormatResolvedOptionsNodeGen;
import com.oracle.truffle.js.nodes.cast.JSToNumberNode;
import com.oracle.truffle.js.nodes.cast.JSToStringNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.intl.JSRelativeTimeFormat;

public final class RelativeTimeFormatPrototypeBuiltins extends JSBuiltinsContainer.SwitchEnum<RelativeTimeFormatPrototypeBuiltins.RelativeTimeFormatPrototype> {

    public static final JSBuiltinsContainer BUILTINS = new RelativeTimeFormatPrototypeBuiltins();

    protected RelativeTimeFormatPrototypeBuiltins() {
        super(JSRelativeTimeFormat.PROTOTYPE_NAME, RelativeTimeFormatPrototype.class);
    }

    public enum RelativeTimeFormatPrototype implements BuiltinEnum<RelativeTimeFormatPrototype> {

        resolvedOptions(0),
        format(2),
        formatToParts(2);

        private final int length;

        RelativeTimeFormatPrototype(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, RelativeTimeFormatPrototype builtinEnum) {
        switch (builtinEnum) {
            case resolvedOptions:
                return JSRelativeTimeFormatResolvedOptionsNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
            case format:
                return JSRelativeTimeFormatFormatNodeGen.create(context, builtin, args().withThis().fixedArgs(2).createArgumentNodes(context));
            case formatToParts:
                return JSRelativeTimeFormatFormatToPartsNodeGen.create(context, builtin, args().withThis().fixedArgs(2).createArgumentNodes(context));
        }
        return null;
    }

    public abstract static class JSRelativeTimeFormatResolvedOptionsNode extends JSBuiltinNode {

        public JSRelativeTimeFormatResolvedOptionsNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(guards = "isJSRelativeTimeFormat(relativeTimeFormat)")
        public Object doResolvedOptions(DynamicObject relativeTimeFormat) {
            return JSRelativeTimeFormat.resolvedOptions(getContext(), getRealm(), relativeTimeFormat);
        }

        @Specialization(guards = "!isJSRelativeTimeFormat(bummer)")
        public void doResolvedOptions(@SuppressWarnings("unused") Object bummer) {
            throw Errors.createTypeErrorTypeXExpected(JSRelativeTimeFormat.CLASS_NAME);
        }
    }

    public abstract static class JSRelativeTimeFormatFormatNode extends JSBuiltinNode {

        public JSRelativeTimeFormatFormatNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(guards = "isJSRelativeTimeFormat(relativeTimeFormat)")
        public String doFormat(DynamicObject relativeTimeFormat, Object value, Object unit,
                        @Cached("create()") JSToStringNode toStringNode,
                        @Cached("create()") JSToNumberNode toNumberNode) {
            return JSRelativeTimeFormat.format(relativeTimeFormat, JSRuntime.doubleValue(toNumberNode.executeNumber(value)), toStringNode.executeString(unit));
        }

        @Specialization(guards = "!isJSRelativeTimeFormat(bummer)")
        @SuppressWarnings("unused")
        public void throwTypeError(Object bummer, Object value, Object unit) {
            throw Errors.createTypeErrorTypeXExpected(JSRelativeTimeFormat.CLASS_NAME);
        }
    }

    public abstract static class JSRelativeTimeFormatFormatToPartsNode extends JSBuiltinNode {

        public JSRelativeTimeFormatFormatToPartsNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(guards = {"isJSRelativeTimeFormat(relativeTimeFormat)"})
        public Object doFormatToParts(DynamicObject relativeTimeFormat, Object value, Object unit,
                        @Cached("create()") JSToStringNode toStringNode,
                        @Cached("create()") JSToNumberNode toNumberNode) {
            double amount = JSRuntime.doubleValue(toNumberNode.executeNumber(value));
            String unitString = toStringNode.executeString(unit);
            return JSRelativeTimeFormat.formatToParts(getContext(), getRealm(), relativeTimeFormat, amount, unitString);
        }

        @Specialization(guards = "!isJSRelativeTimeFormat(bummer)")
        @SuppressWarnings("unused")
        public void throwTypeError(Object bummer, Object value, Object unit) {
            throw Errors.createTypeErrorTypeXExpected(JSRelativeTimeFormat.CLASS_NAME);
        }
    }
}
