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
package com.oracle.truffle.js.builtins;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.js.builtins.NumberPrototypeBuiltinsFactory.JSNumberToExponentialNodeGen;
import com.oracle.truffle.js.builtins.NumberPrototypeBuiltinsFactory.JSNumberToFixedNodeGen;
import com.oracle.truffle.js.builtins.NumberPrototypeBuiltinsFactory.JSNumberToLocaleStringIntlNodeGen;
import com.oracle.truffle.js.builtins.NumberPrototypeBuiltinsFactory.JSNumberToLocaleStringNodeGen;
import com.oracle.truffle.js.builtins.NumberPrototypeBuiltinsFactory.JSNumberToPrecisionNodeGen;
import com.oracle.truffle.js.builtins.NumberPrototypeBuiltinsFactory.JSNumberToStringNodeGen;
import com.oracle.truffle.js.builtins.NumberPrototypeBuiltinsFactory.JSNumberValueOfNodeGen;
import com.oracle.truffle.js.nodes.cast.JSDoubleToStringNode;
import com.oracle.truffle.js.nodes.cast.JSToIntegerNode;
import com.oracle.truffle.js.nodes.cast.JSToNumberNode;
import com.oracle.truffle.js.nodes.cast.JSToStringNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.nodes.intl.InitializeNumberFormatNode;
import com.oracle.truffle.js.runtime.Boundaries;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSException;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSNumber;
import com.oracle.truffle.js.runtime.builtins.JSNumberFormat;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.Undefined;

/**
 * Contains builtins for {@linkplain JSNumber}.prototype.
 */
public final class NumberPrototypeBuiltins extends JSBuiltinsContainer.SwitchEnum<NumberPrototypeBuiltins.NumberPrototype> {
    protected NumberPrototypeBuiltins() {
        super(JSNumber.PROTOTYPE_NAME, NumberPrototype.class);
    }

    public enum NumberPrototype implements BuiltinEnum<NumberPrototype> {
        toExponential(1),
        toFixed(1),
        toLocaleString(0),
        toPrecision(1),
        toString(1),
        valueOf(0);

        private final int length;

        NumberPrototype(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, NumberPrototype builtinEnum) {
        switch (builtinEnum) {
            case toExponential:
                return JSNumberToExponentialNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case toFixed:
                return JSNumberToFixedNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case toLocaleString:
                if (context.isOptionIntl402()) {
                    return JSNumberToLocaleStringIntlNodeGen.create(context, builtin, args().withThis().fixedArgs(2).createArgumentNodes(context));
                } else {
                    return JSNumberToLocaleStringNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
                }
            case toPrecision:
                return JSNumberToPrecisionNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case toString:
                return JSNumberToStringNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case valueOf:
                return JSNumberValueOfNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
        }
        return null;
    }

    public abstract static class JSNumberOperation extends JSBuiltinNode {

        public JSNumberOperation(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Child private JSToStringNode toStringNode;
        @Child private JSToIntegerNode toIntegerNode;
        @Child private JSToNumberNode toNumberNode;

        @TruffleBoundary
        protected JSException noNumberFailure(Object value) {
            String message = (JSObject.isDynamicObject(value) ? JSObject.safeToString((DynamicObject) value) : value.toString()) + " is not a Number";
            throw Errors.createTypeError(message);
        }

        protected Number getNumberValue(DynamicObject obj) {
            return JSNumber.valueOf(obj);
        }

        protected double getDoubleValue(DynamicObject obj) {
            return JSRuntime.doubleValue(JSNumber.valueOf(obj));
        }

        protected String toString(Object target) {
            if (toStringNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                toStringNode = insert(JSToStringNode.create());
            }
            return toStringNode.executeString(target);
        }

        protected int toInteger(Object target) {
            if (toIntegerNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                toIntegerNode = insert(JSToIntegerNode.create());
            }
            return toIntegerNode.executeInt(target);
        }

        protected Number toNumber(Object target) {
            if (toNumberNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                toNumberNode = insert(JSToNumberNode.create());
            }
            return toNumberNode.executeNumber(target);
        }
    }

    public abstract static class JSNumberToStringNode extends JSNumberOperation {

        public JSNumberToStringNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Child private JSDoubleToStringNode doubleToStringNode;
        private final BranchProfile radixOtherBranch = BranchProfile.create();
        private final BranchProfile radixErrorBranch = BranchProfile.create();

        protected String doubleToString(double value) {
            if (doubleToStringNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                doubleToStringNode = insert(JSDoubleToStringNode.create());
            }
            return doubleToStringNode.executeString(value);
        }

        protected boolean isRadix10(Object radix) {
            return radix == Undefined.instance || (radix instanceof Integer && ((Integer) radix) == 10);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"isJSNumber(thisObj)", "isJSNumberInteger(thisObj)", "isRadix10(radix)"})
        protected String toStringIntRadix10(DynamicObject thisObj, Object radix) {
            Integer i = (Integer) getNumberValue(thisObj);
            return Boundaries.stringValueOf(i.intValue());
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"isJSNumber(thisObj)", "isRadix10(radix)"})
        protected String toStringRadix10(DynamicObject thisObj, Object radix) {
            return doubleToString(getDoubleValue(thisObj));
        }

        @Specialization(guards = {"isJSNumber(thisObj)", "!isUndefined(radix)"})
        protected String toString(DynamicObject thisObj, Object radix) {
            return toStringIntl(getDoubleValue(thisObj), radix);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"isJavaNumber(thisObj)", "isNumberInteger(thisObj)", "isRadix10(radix)"})
        protected String toStringPrimitiveIntRadix10(Object thisObj, Object radix) {
            Integer i = (Integer) thisObj;
            return Boundaries.stringValueOf(i.intValue());
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"isJavaNumber(thisObj)", "isRadix10(radix)"})
        protected String toStringPrimitiveRadix10(Object thisObj, Object radix) {
            Number n = (Number) thisObj;
            return doubleToString(JSRuntime.doubleValue(n));
        }

        @Specialization(guards = {"isJavaNumber(thisObj)", "!isUndefined(radix)"})
        protected String toStringPrimitive(Object thisObj, Object radix) {
            return toStringIntl(JSRuntime.doubleValue((Number) thisObj), radix);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"!isJSNumber(thisObj)", "!isJavaNumber(thisObj)"})
        protected String toStringNoNumber(Object thisObj, Object radix) {
            throw noNumberFailure(thisObj);
        }

        private String toStringIntl(double numberVal, Object radix) {
            int radixVal = toInteger(radix);
            if (radixVal < 2 || radixVal > 36) {
                radixErrorBranch.enter();
                throw Errors.createRangeError("toString() expects radix in range 2-36");
            }
            if (radixVal == 10) {
                return doubleToString(numberVal);
            } else {
                radixOtherBranch.enter();
                return JSRuntime.doubleToString(numberVal, radixVal);
            }
        }
    }

    public abstract static class JSNumberToLocaleStringNode extends JSNumberOperation {

        public JSNumberToLocaleStringNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(guards = "isJSNumber(thisObj)")
        protected String toLocaleString(DynamicObject thisObj) {
            double d = getDoubleValue(thisObj);
            return toLocaleStringIntl(d);
        }

        @Specialization(guards = "isJavaNumber(thisObj)")
        protected String toLocaleStringPrimitive(Object thisObj) {
            double d = JSRuntime.doubleValue((Number) thisObj);
            return toLocaleStringIntl(d);
        }

        private static String toLocaleStringIntl(double d) {
            if (JSRuntime.doubleIsRepresentableAsInt(d)) {
                return Boundaries.stringValueOf((int) d);
            } else {
                return Boundaries.stringValueOf(d);
            }
        }

        @Specialization(guards = {"!isJSNumber(thisNumber)", "!isJavaNumber(thisNumber)"})
        protected String toLocaleString(Object thisNumber) {
            throw noNumberFailure(thisNumber);
        }
    }

    /**
     * Implementation of the Number.prototype.toLocaleString() method as specified by ECMAScript.
     * Internationalization API, https://tc39.github.io/ecma402/#sup-number.prototype.tolocalestring
     */
    public abstract static class JSNumberToLocaleStringIntlNode extends JSNumberOperation {

        @Child InitializeNumberFormatNode initNumberFormatNode;

        public JSNumberToLocaleStringIntlNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
            this.initNumberFormatNode = InitializeNumberFormatNode.createInitalizeNumberFormatNode(context);
        }

        @TruffleBoundary
        private DynamicObject createNumberFormat(Object locales, Object options) {
            DynamicObject numberFormatObj = JSNumberFormat.create(getContext());
            initNumberFormatNode.executeInit(numberFormatObj, locales, options);
            return numberFormatObj;
        }

        @Specialization(guards = "isJSNumber(thisObj)")
        protected String jsNumberToLocaleString(DynamicObject thisObj, Object locales, Object options) {
            DynamicObject numberFormatObj = createNumberFormat(locales, options);
            return JSNumberFormat.format(numberFormatObj, getNumberValue(thisObj));
        }

        @Specialization(guards = "isJavaNumber(thisObj)")
        protected String javaNumberToLocaleString(Object thisObj, Object locales, Object options) {
            DynamicObject numberFormatObj = createNumberFormat(locales, options);
            return JSNumberFormat.format(numberFormatObj, JSRuntime.doubleValue((Number) thisObj));
        }

        @Specialization(guards = {"!isJSNumber(notANumber)", "!isJavaNumber(notANumber)"})
        @SuppressWarnings("unused")
        protected String failForNonNumbers(Object notANumber, Object locales, Object options) {
            throw noNumberFailure(notANumber);
        }
    }

    public abstract static class JSNumberValueOfNode extends JSNumberOperation {

        public JSNumberValueOfNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(guards = "isJSNumber(thisNumber)")
        protected Number valueOf(DynamicObject thisNumber) {
            return getNumberValue(thisNumber);
        }

        @Specialization(guards = "isJavaNumber(thisNumber)")
        protected double valueOfPrimitive(Object thisNumber) {
            return JSRuntime.doubleValue((Number) thisNumber);
        }

        @Specialization(guards = {"!isJSNumber(thisNumber)", "!isJavaNumber(thisNumber)"})
        protected Object valueOf(Object thisNumber) {
            throw noNumberFailure(thisNumber);
        }
    }

    public abstract static class JSNumberToFixedNode extends JSNumberOperation {
        private final BranchProfile digitsErrorBranch = BranchProfile.create();
        private final BranchProfile nanBranch = BranchProfile.create();
        private final ConditionProfile dtoaOrString = ConditionProfile.createBinaryProfile();

        protected JSNumberToFixedNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(guards = "isJSNumber(thisNumber)")
        protected String toFixed(DynamicObject thisNumber, Object fractionDigits) {
            int digits = toInteger(fractionDigits);
            return toFixedIntl(getDoubleValue(thisNumber), digits);
        }

        @Specialization(guards = "isJavaNumber(thisNumber)")
        protected String toFixedJava(Object thisNumber, Object fractionDigits) {
            int digits = toInteger(fractionDigits);
            return toFixedIntl(JSRuntime.doubleValue((Number) thisNumber), digits);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"!isJSNumber(thisNumber)", "!isJavaNumber(thisNumber)"})
        protected String toFixedGeneric(Object thisNumber, Object fractionDigits) {
            throw noNumberFailure(thisNumber);
        }

        private String toFixedIntl(double value, int digits) {
            if (0 > digits || digits > ((getContext().getEcmaScriptVersion() >= 9) ? 100 : 20)) {
                digitsErrorBranch.enter();
                throw Errors.createRangeError("toFixed() fraction digits need to be in range 0-100");
            }
            if (Double.isNaN(value)) {
                nanBranch.enter();
                return JSRuntime.NAN_STRING;
            }
            if (dtoaOrString.profile(value >= 1E21 || value <= -1E21)) {
                return toString(value);
            } else {
                return JSRuntime.formatDtoAFixed(value, digits);
            }
        }
    }

    public abstract static class JSNumberToExponentialNode extends JSNumberOperation {
        private final BranchProfile digitsErrorBranch = BranchProfile.create();

        public JSNumberToExponentialNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"isJSNumber(thisNumber)", "isUndefined(fractionDigits)"})
        protected String toExponentialUndefined(DynamicObject thisNumber, Object fractionDigits) {
            return JSRuntime.formatDtoAExponential(getDoubleValue(thisNumber));
        }

        @Specialization(guards = {"isJSNumber(thisNumber)", "!isUndefined(fractionDigits)"})
        protected String toExponential(DynamicObject thisNumber, Object fractionDigits) {
            int digits = toInteger(fractionDigits);
            return toExponentialIntl(digits, getDoubleValue(thisNumber));
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"isJavaNumber(thisNumber)", "isUndefined(fractionDigits)"})
        protected String toExponentialPrimtiveUndefined(Object thisNumber, Object fractionDigits) {
            return JSRuntime.formatDtoAExponential(JSRuntime.doubleValue((Number) thisNumber));
        }

        @Specialization(guards = {"isJavaNumber(thisNumber)", "!isUndefined(fractionDigits)"})
        protected String toExponentialPrimitive(Object thisNumber, Object fractionDigits) {
            int digits = toInteger(fractionDigits);
            return toExponentialIntl(digits, JSRuntime.doubleValue((Number) thisNumber));
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"!isJSNumber(thisNumber)", "!isJavaNumber(thisNumber)"})
        protected String toExponential(Object thisNumber, Object fractionDigits) {
            throw noNumberFailure(thisNumber);
        }

        public String toExponentialIntl(int digits, double value) {
            if (Double.isNaN(value)) {
                return JSRuntime.NAN_STRING;
            } else if (Double.isInfinite(value)) {
                return (value < 0) ? JSRuntime.NEGATIVE_INFINITY_STRING : JSRuntime.INFINITY_STRING;
            }
            checkDigits(digits);
            return JSRuntime.formatDtoAExponential(value, digits + 1);
        }

        private void checkDigits(int digits) {
            if (0 > digits || digits > ((getContext().getEcmaScriptVersion() >= 9) ? 100 : 20)) {
                digitsErrorBranch.enter();
                throw Errors.createRangeError("toExponential() fraction digits need to be in range 0-100");
            }
        }
    }

    public abstract static class JSNumberToPrecisionNode extends JSNumberOperation {
        private final BranchProfile precisionErrorBranch = BranchProfile.create();

        public JSNumberToPrecisionNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"isJSNumber(thisNumber)", "isUndefined(precision)"})
        protected String toPrecisionUndefined(DynamicObject thisNumber, Object precision) {
            return toString(thisNumber); // ECMA 15.7.4.7 2
        }

        @Specialization(guards = {"isJSNumber(thisNumber)", "!isUndefined(precision)"})
        protected String toPrecision(DynamicObject thisNumber, Object precision) {
            long lPrecision = JSRuntime.toInteger(toNumber(precision));
            double thisNumberVal = getDoubleValue(thisNumber);
            return toPrecisionIntl(thisNumberVal, lPrecision);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"isJavaNumber(thisNumber)", "isUndefined(precision)"})
        protected String toPrecisionPrimitiveUndefined(Object thisNumber, Object precision) {
            return toString(thisNumber); // ECMA 15.7.4.7 2
        }

        @Specialization(guards = {"isJavaNumber(thisNumber)", "!isUndefined(precision)"})
        protected String toPrecisionPrimitive(Object thisNumber, Object precision) {
            long lPrecision = JSRuntime.toInteger(toNumber(precision));
            double thisNumberVal = JSRuntime.doubleValue((Number) thisNumber);
            return toPrecisionIntl(thisNumberVal, lPrecision);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"!isJSNumber(thisNumber)", "!isJavaNumber(thisNumber)"})
        protected String toPrecision(Object thisNumber, Object precision) {
            throw noNumberFailure(thisNumber);
        }

        private String toPrecisionIntl(double thisNumberVal, long lPrecision) {
            if (Double.isNaN(thisNumberVal)) {
                return JSRuntime.NAN_STRING;
            } else if (Double.isInfinite(thisNumberVal)) {
                return (thisNumberVal < 0) ? JSRuntime.NEGATIVE_INFINITY_STRING : JSRuntime.INFINITY_STRING;
            }
            checkPrecision(lPrecision);
            return JSRuntime.formatDtoAPrecision(thisNumberVal, (int) lPrecision);
        }

        private void checkPrecision(long precision) {
            if (1 > precision || precision > ((getContext().getEcmaScriptVersion() >= 9) ? 100 : 20)) {
                precisionErrorBranch.enter();
                throw Errors.createRangeError("precision not in range");
            }
        }
    }
}
