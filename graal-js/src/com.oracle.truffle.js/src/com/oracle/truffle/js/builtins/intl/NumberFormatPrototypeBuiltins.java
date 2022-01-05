/*
 * Copyright (c) 2018, 2022, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.js.builtins.JSBuiltinsContainer;
import com.oracle.truffle.js.builtins.intl.NumberFormatPrototypeBuiltinsFactory.JSNumberFormatFormatRangeNodeGen;
import com.oracle.truffle.js.builtins.intl.NumberFormatPrototypeBuiltinsFactory.JSNumberFormatFormatRangeToPartsNodeGen;
import com.oracle.truffle.js.builtins.intl.NumberFormatPrototypeBuiltinsFactory.JSNumberFormatFormatToPartsNodeGen;
import com.oracle.truffle.js.builtins.intl.NumberFormatPrototypeBuiltinsFactory.JSNumberFormatResolvedOptionsNodeGen;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.nodes.intl.ToIntlMathematicalValue;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.intl.JSNumberFormat;
import com.oracle.truffle.js.runtime.objects.Undefined;

public final class NumberFormatPrototypeBuiltins extends JSBuiltinsContainer.SwitchEnum<NumberFormatPrototypeBuiltins.NumberFormatPrototype> {

    public static final JSBuiltinsContainer BUILTINS = new NumberFormatPrototypeBuiltins();

    protected NumberFormatPrototypeBuiltins() {
        super(JSNumberFormat.PROTOTYPE_NAME, NumberFormatPrototype.class);
    }

    public enum NumberFormatPrototype implements BuiltinEnum<NumberFormatPrototype> {

        resolvedOptions(0),
        formatToParts(1),
        formatRange(2),
        formatRangeToParts(2);

        private final int length;

        NumberFormatPrototype(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }

        @Override
        public int getECMAScriptVersion() {
            switch (this) {
                case formatToParts:
                    return JSConfig.ECMAScript2018;
                case formatRange:
                case formatRangeToParts:
                    return JSConfig.ECMAScript2023;
            }
            return BuiltinEnum.super.getECMAScriptVersion();
        }

    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, NumberFormatPrototype builtinEnum) {
        switch (builtinEnum) {
            case resolvedOptions:
                return JSNumberFormatResolvedOptionsNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
            case formatToParts:
                return JSNumberFormatFormatToPartsNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case formatRange:
                return JSNumberFormatFormatRangeNodeGen.create(context, builtin, args().withThis().fixedArgs(2).createArgumentNodes(context));
            case formatRangeToParts:
                return JSNumberFormatFormatRangeToPartsNodeGen.create(context, builtin, args().withThis().fixedArgs(2).createArgumentNodes(context));
        }
        return null;
    }

    public abstract static class JSNumberFormatResolvedOptionsNode extends JSBuiltinNode {

        public JSNumberFormatResolvedOptionsNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(guards = {"isJSNumberFormat(numberFormat)"})
        public Object doResolvedOptions(DynamicObject numberFormat) {
            return JSNumberFormat.resolvedOptions(getContext(), getRealm(), numberFormat);
        }

        @Fallback
        public void doResolvedOptions(@SuppressWarnings("unused") Object bummer) {
            throw Errors.createTypeErrorTypeXExpected(JSNumberFormat.CLASS_NAME);
        }
    }

    public abstract static class JSNumberFormatFormatToPartsNode extends JSBuiltinNode {

        public JSNumberFormatFormatToPartsNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(guards = {"isJSNumberFormat(numberFormat)"})
        public Object doFormatToParts(DynamicObject numberFormat, Object value) {
            return JSNumberFormat.formatToParts(getContext(), getRealm(), numberFormat, value);
        }

        @Fallback
        @SuppressWarnings("unused")
        public void throwTypeError(Object bummer, Object value) {
            throw Errors.createTypeErrorTypeXExpected(JSNumberFormat.CLASS_NAME);
        }
    }

    public abstract static class JSNumberFormatFormatRangeNode extends JSBuiltinNode {

        public JSNumberFormatFormatRangeNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(guards = {"isJSNumberFormat(numberFormat)"})
        public String doFormatRange(DynamicObject numberFormat, Object start, Object end,
                        @Cached("create(true)") ToIntlMathematicalValue startToIntlMVNode,
                        @Cached("create(true)") ToIntlMathematicalValue endToIntlMVNode,
                        @Cached BranchProfile errorBranch) {
            if (start == Undefined.instance || end == Undefined.instance) {
                errorBranch.enter();
                throw Errors.createTypeError("invalid range");
            }
            Number x = startToIntlMVNode.executeNumber(start);
            Number y = endToIntlMVNode.executeNumber(end);
            return JSNumberFormat.formatRange(numberFormat, x, y);
        }

        @Fallback
        @SuppressWarnings("unused")
        public void throwTypeError(Object bummer, Object start, Object end) {
            throw Errors.createTypeErrorTypeXExpected(JSNumberFormat.CLASS_NAME);
        }
    }

    public abstract static class JSNumberFormatFormatRangeToPartsNode extends JSBuiltinNode {

        public JSNumberFormatFormatRangeToPartsNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(guards = {"isJSNumberFormat(numberFormat)"})
        public Object doFormatRangeToParts(DynamicObject numberFormat, Object start, Object end,
                        @Cached("create(true)") ToIntlMathematicalValue startToIntlMVNode,
                        @Cached("create(true)") ToIntlMathematicalValue endToIntlMVNode,
                        @Cached BranchProfile errorBranch) {
            if (start == Undefined.instance || end == Undefined.instance) {
                errorBranch.enter();
                throw Errors.createTypeError("invalid range");
            }
            Number x = startToIntlMVNode.executeNumber(start);
            Number y = endToIntlMVNode.executeNumber(end);
            return JSNumberFormat.formatRangeToParts(getContext(), getRealm(), numberFormat, x, y);
        }

        @Fallback
        @SuppressWarnings("unused")
        public void throwTypeError(Object bummer, Object start, Object end) {
            throw Errors.createTypeErrorTypeXExpected(JSNumberFormat.CLASS_NAME);
        }
    }

}
