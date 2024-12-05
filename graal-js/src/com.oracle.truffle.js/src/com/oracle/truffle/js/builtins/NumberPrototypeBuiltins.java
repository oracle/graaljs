/*
 * Copyright (c) 2018, 2024, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.InlinedBranchProfile;
import com.oracle.truffle.api.profiles.InlinedConditionProfile;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.builtins.NumberPrototypeBuiltinsFactory.JSNumberToExponentialNodeGen;
import com.oracle.truffle.js.builtins.NumberPrototypeBuiltinsFactory.JSNumberToFixedNodeGen;
import com.oracle.truffle.js.builtins.NumberPrototypeBuiltinsFactory.JSNumberToLocaleStringIntlNodeGen;
import com.oracle.truffle.js.builtins.NumberPrototypeBuiltinsFactory.JSNumberToLocaleStringNodeGen;
import com.oracle.truffle.js.builtins.NumberPrototypeBuiltinsFactory.JSNumberToPrecisionNodeGen;
import com.oracle.truffle.js.builtins.NumberPrototypeBuiltinsFactory.JSNumberToStringNodeGen;
import com.oracle.truffle.js.builtins.NumberPrototypeBuiltinsFactory.JSNumberValueOfNodeGen;
import com.oracle.truffle.js.nodes.JSGuards;
import com.oracle.truffle.js.nodes.cast.IsNumberNode;
import com.oracle.truffle.js.nodes.cast.JSDoubleToStringNode;
import com.oracle.truffle.js.nodes.cast.JSToDoubleNode;
import com.oracle.truffle.js.nodes.cast.JSToIntegerAsIntNode;
import com.oracle.truffle.js.nodes.cast.JSToNumberNode;
import com.oracle.truffle.js.nodes.cast.JSToStringNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.nodes.intl.InitializeNumberFormatNode;
import com.oracle.truffle.js.runtime.BigInt;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSException;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSNumber;
import com.oracle.truffle.js.runtime.builtins.JSNumberObject;
import com.oracle.truffle.js.runtime.builtins.intl.JSNumberFormat;
import com.oracle.truffle.js.runtime.builtins.intl.JSNumberFormatObject;
import com.oracle.truffle.js.runtime.objects.Undefined;

/**
 * Contains builtins for {@linkplain JSNumber}.prototype.
 */
public final class NumberPrototypeBuiltins extends JSBuiltinsContainer.SwitchEnum<NumberPrototypeBuiltins.NumberPrototype> {

    public static final JSBuiltinsContainer BUILTINS = new NumberPrototypeBuiltins();

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

    protected static double getDoubleValue(JSNumberObject obj) {
        return JSRuntime.doubleValue(obj.getNumber());
    }

    protected static double getDoubleValue(InteropLibrary interop, Object obj) {
        assert JSGuards.isForeignObjectOrNumber(obj);
        try {
            if (interop.fitsInDouble(obj)) {
                return interop.asDouble(obj);
            } else if (interop.fitsInBigInteger(obj)) {
                return BigInt.doubleValueOf(interop.asBigInteger(obj));
            }
        } catch (UnsupportedMessageException ex) {
            throw Errors.createTypeErrorUnboxException(obj, ex, interop);
        }
        throw Errors.createTypeErrorNotANumber(obj);
    }

    @ImportStatic({JSConfig.class})
    public abstract static class JSNumberToStringNode extends JSBuiltinNode {

        public JSNumberToStringNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        protected boolean isRadix10(Object radix) {
            return radix == Undefined.instance || (radix instanceof Integer && ((Integer) radix) == 10);
        }

        /**
         * Guard used to ensure that the parameter is a JSNumberObject that hosts an Integer.
         */
        protected static boolean isJSNumberInteger(JSNumberObject thisObj) {
            return thisObj.getNumber() instanceof Integer;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"isJSNumberInteger(thisObj)", "isRadix10(radix)"})
        protected Object toStringIntRadix10(JSNumberObject thisObj, Object radix) {
            Integer i = (Integer) thisObj.getNumber();
            return Strings.fromInt(i.intValue());
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"isRadix10(radix)"})
        protected Object toStringRadix10(JSNumberObject thisObj, Object radix,
                        @Shared @Cached JSDoubleToStringNode doubleToString) {
            return doubleToString.executeString(getDoubleValue(thisObj));
        }

        @Specialization(guards = {"!isUndefined(radix)"})
        protected Object toString(JSNumberObject thisObj, Object radix,
                        @Shared @Cached JSToIntegerAsIntNode toIntegerNode,
                        @Shared @Cached JSDoubleToStringNode doubleToString,
                        @Shared @Cached InlinedBranchProfile radixOtherBranch,
                        @Shared @Cached InlinedBranchProfile radixErrorBranch) {
            return toStringWithRadix(getDoubleValue(thisObj), radix,
                            this, toIntegerNode, doubleToString, radixOtherBranch, radixErrorBranch);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"isRadix10(radix)"})
        protected Object toStringPrimitiveIntRadix10(int thisInteger, Object radix) {
            return Strings.fromInt(thisInteger);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"isNumber.execute(this, thisNumber)", "isRadix10(radix)"})
        protected Object toStringPrimitiveRadix10(Object thisNumber, Object radix,
                        @Shared @Cached @SuppressWarnings("unused") IsNumberNode isNumber,
                        @Shared @Cached JSDoubleToStringNode doubleToString) {
            return doubleToString.executeString(thisNumber);
        }

        @Specialization(guards = {"isNumber.execute(this, thisNumber)"})
        protected Object toStringPrimitiveRadixInt(Object thisNumber, int radix,
                        @Shared @Cached @SuppressWarnings("unused") IsNumberNode isNumber,
                        @Shared @Cached JSToDoubleNode toDouble,
                        @Shared @Cached JSDoubleToStringNode doubleToString,
                        @Shared @Cached InlinedBranchProfile radixOtherBranch,
                        @Shared @Cached InlinedBranchProfile radixErrorBranch) {
            double doubleValue = toDouble.executeDouble(thisNumber);
            return toStringWithIntRadix(doubleValue, radix,
                            this, doubleToString, radixOtherBranch, radixErrorBranch);
        }

        @Specialization(guards = {"isNumber.execute(this, thisNumber)", "!isUndefined(radix)"}, replaces = "toStringPrimitiveRadixInt")
        protected Object toStringPrimitive(Object thisNumber, Object radix,
                        @Shared @Cached @SuppressWarnings("unused") IsNumberNode isNumber,
                        @Shared @Cached JSToDoubleNode toDouble,
                        @Shared @Cached JSToIntegerAsIntNode toIntegerNode,
                        @Shared @Cached JSDoubleToStringNode doubleToString,
                        @Shared @Cached InlinedBranchProfile radixOtherBranch,
                        @Shared @Cached InlinedBranchProfile radixErrorBranch) {
            double doubleValue = toDouble.executeDouble(thisNumber);
            return toStringWithRadix(doubleValue, radix,
                            this, toIntegerNode, doubleToString, radixOtherBranch, radixErrorBranch);
        }

        @SuppressWarnings("truffle-static-method")
        @Specialization(guards = "isForeignObjectOrNumber(thisObj)", limit = "InteropLibraryLimit")
        protected Object toStringForeignObject(Object thisObj, Object radix,
                        @Bind Node node,
                        @Shared @Cached JSToIntegerAsIntNode toIntegerNode,
                        @Shared @Cached JSDoubleToStringNode doubleToString,
                        @Shared @Cached InlinedBranchProfile radixOtherBranch,
                        @Shared @Cached InlinedBranchProfile radixErrorBranch,
                        @CachedLibrary("thisObj") InteropLibrary interop) {
            Object radixToUse;
            if (radix == Undefined.instance) {
                if (thisObj instanceof Long longValue) {
                    // Do not lose precision when toString() is invoked on Long
                    return Strings.fromLong(longValue);
                }
                radixToUse = 10;
            } else {
                radixToUse = radix;
            }
            return toStringWithRadix(getDoubleValue(interop, thisObj), radixToUse,
                            node, toIntegerNode, doubleToString, radixOtherBranch, radixErrorBranch);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"!isJSNumber(thisObj)", "!isNumber(thisObj)", "!isForeignObjectOrNumber(thisObj)"})
        protected String toStringNoNumber(Object thisObj, Object radix) {
            throw Errors.createTypeErrorNotANumber(thisObj);
        }

        private static Object toStringWithRadix(double numberVal, Object radix,
                        Node node, JSToIntegerAsIntNode toIntegerNode, JSDoubleToStringNode doubleToString, InlinedBranchProfile radixOtherBranch, InlinedBranchProfile radixErrorBranch) {
            int radixVal = toIntegerNode.executeInt(radix);
            return toStringWithIntRadix(numberVal, radixVal,
                            node, doubleToString, radixOtherBranch, radixErrorBranch);
        }

        private static Object toStringWithIntRadix(double numberVal, int radixVal,
                        Node node, JSDoubleToStringNode doubleToString, InlinedBranchProfile radixOtherBranch, InlinedBranchProfile radixErrorBranch) {
            if (radixVal < 2 || radixVal > 36) {
                radixErrorBranch.enter(node);
                throw Errors.createRangeError("toString() expects radix in range 2-36");
            }
            if (radixVal == 10) {
                return doubleToString.executeString(numberVal);
            } else {
                radixOtherBranch.enter(node);
                return JSRuntime.doubleToString(numberVal, radixVal);
            }
        }
    }

    @ImportStatic({JSConfig.class})
    public abstract static class JSNumberToLocaleStringNode extends JSBuiltinNode {

        public JSNumberToLocaleStringNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected static Object doNumberObject(JSNumberObject thisObj) {
            double d = getDoubleValue(thisObj);
            return JSRuntime.doubleToString(d);
        }

        @Specialization
        protected static Object doInt(int thisInteger) {
            return Strings.fromInt(thisInteger);
        }

        @Specialization(guards = "isNumber.execute(node, thisNumber)", limit = "1")
        protected static Object doNumber(Object thisNumber,
                        @Bind @SuppressWarnings("unused") Node node,
                        @Cached @SuppressWarnings("unused") IsNumberNode isNumber,
                        @Cached JSToDoubleNode toDouble) {
            double d = toDouble.executeDouble(thisNumber);
            return JSRuntime.doubleToString(d);
        }

        @Specialization(guards = "isForeignObjectOrNumber(thisObj)", limit = "InteropLibraryLimit")
        protected static Object doForeign(Object thisObj,
                        @CachedLibrary("thisObj") InteropLibrary interop) {
            return JSRuntime.doubleToString(getDoubleValue(interop, thisObj));
        }

        @Fallback
        protected String toLocaleStringOther(Object thisNumber) {
            throw Errors.createTypeErrorNotANumber(thisNumber);
        }
    }

    /**
     * Implementation of the Number.prototype.toLocaleString() method as specified by ECMAScript.
     * Internationalization API, https://tc39.github.io/ecma402/#sup-number.prototype.tolocalestring
     */
    @ImportStatic({JSConfig.class})
    public abstract static class JSNumberToLocaleStringIntlNode extends JSBuiltinNode {

        @Child InitializeNumberFormatNode initNumberFormatNode;

        public JSNumberToLocaleStringIntlNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
            this.initNumberFormatNode = InitializeNumberFormatNode.createInitalizeNumberFormatNode(context);
        }

        @TruffleBoundary
        private JSNumberFormatObject createNumberFormat(Object locales, Object options) {
            JSNumberFormatObject numberFormatObj = JSNumberFormat.create(getContext(), getRealm());
            initNumberFormatNode.executeInit(numberFormatObj, locales, options);
            return numberFormatObj;
        }

        @Specialization
        protected TruffleString jsNumberToLocaleString(JSNumberObject thisObj, Object locales, Object options) {
            JSNumberFormatObject numberFormatObj = createNumberFormat(locales, options);
            return JSNumberFormat.format(numberFormatObj, thisObj.getNumber());
        }

        @SuppressWarnings("truffle-static-method")
        @Specialization(guards = "isNumber.execute(node, thisNumber)", limit = "1")
        protected final TruffleString javaNumberToLocaleString(Object thisNumber, Object locales, Object options,
                        @Bind @SuppressWarnings("unused") Node node,
                        @Cached @SuppressWarnings("unused") IsNumberNode isNumber,
                        @Cached JSToDoubleNode toDouble) {
            double doubleValue = toDouble.executeDouble(thisNumber);
            JSNumberFormatObject numberFormatObj = createNumberFormat(locales, options);
            return JSNumberFormat.format(numberFormatObj, doubleValue);
        }

        @Specialization(guards = "isForeignObjectOrNumber(thisObj)", limit = "InteropLibraryLimit")
        protected TruffleString toLocaleStringForeignObject(Object thisObj, Object locales, Object options,
                        @CachedLibrary("thisObj") InteropLibrary interop) {
            double doubleValue = getDoubleValue(interop, thisObj);
            JSNumberFormatObject numberFormatObj = createNumberFormat(locales, options);
            return JSNumberFormat.format(numberFormatObj, doubleValue);
        }

        @Fallback
        @SuppressWarnings("unused")
        protected Object failForNonNumbers(Object notANumber, Object locales, Object options) {
            throw Errors.createTypeErrorNotANumber(notANumber);
        }
    }

    @ImportStatic({JSConfig.class})
    public abstract static class JSNumberValueOfNode extends JSBuiltinNode {

        public JSNumberValueOfNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected static Number valueOf(JSNumberObject thisNumber) {
            return thisNumber.getNumber();
        }

        @Specialization(guards = "isNumber.execute(node, thisNumber)", limit = "1")
        protected static double valueOfPrimitive(Object thisNumber,
                        @Bind @SuppressWarnings("unused") Node node,
                        @Cached @SuppressWarnings("unused") IsNumberNode isNumber,
                        @Cached JSToDoubleNode toDouble) {
            return toDouble.executeDouble(thisNumber);
        }

        @Specialization(guards = "isForeignObjectOrNumber(thisNumber)", limit = "InteropLibraryLimit")
        protected static double valueOfForeignObject(Object thisNumber,
                        @CachedLibrary("thisNumber") InteropLibrary interop) {
            return getDoubleValue(interop, thisNumber);
        }

        @Fallback
        protected static Object valueOfOther(Object thisNumber) {
            throw Errors.createTypeErrorNotANumber(thisNumber);
        }
    }

    @ImportStatic({JSConfig.class})
    public abstract static class JSNumberToFixedNode extends JSBuiltinNode {

        protected JSNumberToFixedNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected Object toFixed(JSNumberObject thisNumber, Object fractionDigits,
                        @Shared @Cached JSToIntegerAsIntNode toIntegerNode,
                        @Shared @Cached JSDoubleToStringNode doubleToString,
                        @Shared @Cached InlinedBranchProfile digitsErrorBranch,
                        @Shared @Cached InlinedBranchProfile nanBranch,
                        @Shared @Cached InlinedConditionProfile dtoaOrString) {
            int digits = toIntegerNode.executeInt(fractionDigits);
            return toFixedIntl(getDoubleValue(thisNumber), digits,
                            this, doubleToString, digitsErrorBranch, nanBranch, dtoaOrString);
        }

        @Specialization(guards = "isNumber.execute(this, thisNumber)", limit = "1")
        protected Object toFixedJava(Object thisNumber, Object fractionDigits,
                        @Cached @SuppressWarnings("unused") IsNumberNode isNumber,
                        @Shared @Cached JSToIntegerAsIntNode toIntegerNode,
                        @Shared @Cached JSDoubleToStringNode doubleToString,
                        @Shared @Cached InlinedBranchProfile digitsErrorBranch,
                        @Shared @Cached InlinedBranchProfile nanBranch,
                        @Shared @Cached InlinedConditionProfile dtoaOrString) {
            int digits = toIntegerNode.executeInt(fractionDigits);
            return toFixedIntl(JSRuntime.doubleValue((Number) thisNumber), digits,
                            this, doubleToString, digitsErrorBranch, nanBranch, dtoaOrString);
        }

        @SuppressWarnings("truffle-static-method")
        @Specialization(guards = "isForeignObjectOrNumber(thisNumber)", limit = "InteropLibraryLimit")
        protected Object toFixedForeignObject(Object thisNumber, Object fractionDigits,
                        @Bind Node node,
                        @Shared @Cached JSToIntegerAsIntNode toIntegerNode,
                        @Shared @Cached JSDoubleToStringNode doubleToString,
                        @Shared @Cached InlinedBranchProfile digitsErrorBranch,
                        @Shared @Cached InlinedBranchProfile nanBranch,
                        @Shared @Cached InlinedConditionProfile dtoaOrString,
                        @CachedLibrary("thisNumber") InteropLibrary interop) {
            double doubleValue = getDoubleValue(interop, thisNumber);
            int digits = toIntegerNode.executeInt(fractionDigits);
            return toFixedIntl(doubleValue, digits,
                            node, doubleToString, digitsErrorBranch, nanBranch, dtoaOrString);
        }

        @SuppressWarnings("unused")
        @Fallback
        protected Object toFixedGeneric(Object thisNumber, Object fractionDigits) {
            throw Errors.createTypeErrorNotANumber(thisNumber);
        }

        private Object toFixedIntl(double value, int digits,
                        Node node, JSDoubleToStringNode doubleToString, InlinedBranchProfile digitsErrorBranch, InlinedBranchProfile nanBranch, InlinedConditionProfile dtoaOrString) {
            if (0 > digits || digits > ((getContext().getEcmaScriptVersion() >= JSConfig.ECMAScript2018) ? 100 : 20)) {
                digitsErrorBranch.enter(node);
                throw Errors.createRangeError("toFixed() fraction digits need to be in range 0-100");
            }
            if (Double.isNaN(value)) {
                nanBranch.enter(node);
                return Strings.NAN;
            }
            if (dtoaOrString.profile(node, value >= 1E21 || value <= -1E21)) {
                return doubleToString.executeString(value);
            } else {
                return JSRuntime.formatDtoAFixed(value, digits);
            }
        }
    }

    @ImportStatic({JSConfig.class})
    public abstract static class JSNumberToExponentialNode extends JSBuiltinNode {

        public JSNumberToExponentialNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"isUndefined(fractionDigits)"})
        protected Object toExponentialUndefined(JSNumberObject thisNumber, Object fractionDigits) {
            double doubleValue = getDoubleValue(thisNumber);
            return toExponentialStandard(doubleValue);
        }

        @Specialization(guards = {"!isUndefined(fractionDigits)"})
        protected Object toExponential(JSNumberObject thisNumber, Object fractionDigits,
                        @Shared @Cached InlinedBranchProfile digitsErrorBranch,
                        @Shared @Cached JSToIntegerAsIntNode toIntegerNode) {
            double doubleValue = getDoubleValue(thisNumber);
            int digits = toIntegerNode.executeInt(fractionDigits);
            return toExponential(doubleValue, digits, this, digitsErrorBranch);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"isNumber.execute(node, thisNumber)", "isUndefined(fractionDigits)"})
        protected static Object toExponentialPrimitiveUndefined(Object thisNumber, Object fractionDigits,
                        @Bind @SuppressWarnings("unused") Node node,
                        @Shared @Cached @SuppressWarnings("unused") IsNumberNode isNumber,
                        @Shared @Cached JSToDoubleNode toDouble) {
            double doubleValue = toDouble.executeDouble(thisNumber);
            return toExponentialStandard(doubleValue);
        }

        @Specialization(guards = {"isNumber.execute(node, thisNumber)", "!isUndefined(fractionDigits)"})
        protected final Object toExponentialPrimitive(Object thisNumber, Object fractionDigits,
                        @Bind @SuppressWarnings("unused") Node node,
                        @Shared @Cached @SuppressWarnings("unused") IsNumberNode isNumber,
                        @Shared @Cached JSToDoubleNode toDouble,
                        @Shared @Cached InlinedBranchProfile digitsErrorBranch,
                        @Shared @Cached JSToIntegerAsIntNode toIntegerNode) {
            double doubleValue = toDouble.executeDouble(thisNumber);
            int digits = toIntegerNode.executeInt(fractionDigits);
            return toExponential(doubleValue, digits, this, digitsErrorBranch);
        }

        @Specialization(guards = {"isForeignObjectOrNumber(thisNumber)", "isUndefined(fractionDigits)"}, limit = "InteropLibraryLimit")
        protected Object toExponentialForeignObjectUndefined(Object thisNumber, @SuppressWarnings("unused") Object fractionDigits,
                        @CachedLibrary("thisNumber") InteropLibrary interop) {
            double doubleValue = getDoubleValue(interop, thisNumber);
            return toExponentialStandard(doubleValue);
        }

        @SuppressWarnings("truffle-static-method")
        @Specialization(guards = {"isForeignObjectOrNumber(thisNumber)", "!isUndefined(fractionDigits)"}, limit = "InteropLibraryLimit")
        protected Object toExponentialForeignObject(Object thisNumber, Object fractionDigits,
                        @Bind Node node,
                        @Shared @Cached InlinedBranchProfile digitsErrorBranch,
                        @Shared @Cached JSToIntegerAsIntNode toIntegerNode,
                        @CachedLibrary("thisNumber") InteropLibrary interop) {
            double doubleValue = getDoubleValue(interop, thisNumber);
            int digits = toIntegerNode.executeInt(fractionDigits);
            return toExponential(doubleValue, digits, node, digitsErrorBranch);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"!isJSNumber(thisNumber)", "!isNumber(thisNumber)", "!isForeignObjectOrNumber(thisNumber)"})
        protected Object toExponentialOther(Object thisNumber, Object fractionDigits) {
            throw Errors.createTypeErrorNotANumber(thisNumber);
        }

        private static Object toExponentialStandard(double value) {
            if (Double.isNaN(value)) {
                return Strings.NAN;
            } else if (Double.isInfinite(value)) {
                return (value < 0) ? Strings.NEGATIVE_INFINITY : Strings.INFINITY;
            }
            return JSRuntime.formatDtoAExponential(value);
        }

        private Object toExponential(double value, int digits, Node node, InlinedBranchProfile digitsErrorBranch) {
            if (Double.isNaN(value)) {
                return Strings.NAN;
            } else if (Double.isInfinite(value)) {
                return (value < 0) ? Strings.NEGATIVE_INFINITY : Strings.INFINITY;
            }
            checkDigits(digits, node, digitsErrorBranch);
            return JSRuntime.formatDtoAExponential(value, digits);
        }

        private void checkDigits(int digits, Node node, InlinedBranchProfile digitsErrorBranch) {
            final int maxDigits = getContext().getEcmaScriptVersion() >= JSConfig.ECMAScript2018 ? 100 : 20;
            if (0 > digits || digits > maxDigits) {
                digitsErrorBranch.enter(node);
                throw digitsRangeError(maxDigits);
            }
        }

        @TruffleBoundary
        private static JSException digitsRangeError(int maxDigits) {
            return Errors.createRangeError("toExponential() fraction digits need to be in range 0-" + maxDigits);
        }
    }

    @ImportStatic({JSConfig.class})
    public abstract static class JSNumberToPrecisionNode extends JSBuiltinNode {

        public JSNumberToPrecisionNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"isUndefined(precision)"})
        protected Object toPrecisionUndefined(JSNumberObject thisNumber, Object precision,
                        @Shared @Cached JSToStringNode toStringNode) {
            return toStringNode.executeString(thisNumber);
        }

        @Specialization(guards = {"!isUndefined(precision)"})
        protected Object toPrecision(JSNumberObject thisNumber, Object precision,
                        @Shared @Cached JSToNumberNode toNumberNode,
                        @Shared @Cached InlinedBranchProfile errorBranch) {
            long lPrecision = JSRuntime.toInteger(toNumberNode.executeNumber(precision));
            double thisNumberVal = getDoubleValue(thisNumber);
            return toPrecisionIntl(thisNumberVal, lPrecision, this, errorBranch);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"isNumber.execute(this, thisNumber)", "isUndefined(precision)"})
        protected static Object toPrecisionPrimitiveUndefined(Object thisNumber, Object precision,
                        @Shared @Cached @SuppressWarnings("unused") IsNumberNode isNumber,
                        @Shared @Cached JSToStringNode toStringNode) {
            return toStringNode.executeString(thisNumber);
        }

        @Specialization(guards = {"isNumber.execute(this, thisNumber)", "!isUndefined(precision)"})
        protected final Object toPrecisionPrimitive(Object thisNumber, Object precision,
                        @Shared @Cached @SuppressWarnings("unused") IsNumberNode isNumber,
                        @Shared @Cached JSToNumberNode toNumberNode,
                        @Shared @Cached InlinedBranchProfile errorBranch) {
            long lPrecision = JSRuntime.toInteger(toNumberNode.executeNumber(precision));
            double thisNumberVal = JSRuntime.doubleValue((Number) thisNumber);
            return toPrecisionIntl(thisNumberVal, lPrecision, this, errorBranch);
        }

        @Specialization(guards = {"isForeignObjectOrNumber(thisNumber)", "isUndefined(precision)"}, limit = "InteropLibraryLimit")
        protected Object toPrecisionForeignObjectUndefined(Object thisNumber, @SuppressWarnings("unused") Object precision,
                        @Shared @Cached JSToStringNode toStringNode,
                        @CachedLibrary("thisNumber") InteropLibrary interop) {
            return toStringNode.executeString(getDoubleValue(interop, thisNumber));
        }

        @SuppressWarnings("truffle-static-method")
        @Specialization(guards = {"isForeignObjectOrNumber(thisNumber)", "!isUndefined(precision)"}, limit = "InteropLibraryLimit")
        protected Object toPrecisionForeignObject(Object thisNumber, Object precision,
                        @Bind Node node,
                        @Shared @Cached JSToNumberNode toNumberNode,
                        @Shared @Cached InlinedBranchProfile errorBranch,
                        @CachedLibrary("thisNumber") InteropLibrary interop) {
            double thisNumberVal = getDoubleValue(interop, thisNumber);
            long lPrecision = JSRuntime.toInteger(toNumberNode.executeNumber(precision));
            return toPrecisionIntl(thisNumberVal, lPrecision, node, errorBranch);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"!isJSNumber(thisNumber)", "!isNumber(thisNumber)", "!isForeignObjectOrNumber(thisNumber)"})
        protected Object toPrecisionOther(Object thisNumber, Object precision) {
            throw Errors.createTypeErrorNotANumber(thisNumber);
        }

        private Object toPrecisionIntl(double thisNumberVal, long lPrecision, Node node, InlinedBranchProfile errorBranch) {
            if (Double.isNaN(thisNumberVal)) {
                return Strings.NAN;
            } else if (Double.isInfinite(thisNumberVal)) {
                return (thisNumberVal < 0) ? Strings.NEGATIVE_INFINITY : Strings.INFINITY;
            }
            checkPrecision(lPrecision, node, errorBranch);
            return JSRuntime.formatDtoAPrecision(thisNumberVal, (int) lPrecision);
        }

        private void checkPrecision(long precision, Node node, InlinedBranchProfile errorBranch) {
            int maxPrecision = (getContext().getEcmaScriptVersion() >= JSConfig.ECMAScript2018) ? 100 : 20;
            if (precision < 1 || precision > maxPrecision) {
                errorBranch.enter(node);
                throw precisionRangeError(maxPrecision);
            }
        }

        @TruffleBoundary
        private static JSException precisionRangeError(int maxPrecision) {
            return Errors.createRangeError("toPrecision() argument must be between 1 and " + maxPrecision);
        }
    }
}
