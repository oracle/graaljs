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
package com.oracle.truffle.js.runtime;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.List;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.api.object.ObjectType;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.js.runtime.builtins.JSAbstractArray;
import com.oracle.truffle.js.runtime.builtins.JSAdapter;
import com.oracle.truffle.js.runtime.builtins.JSArray;
import com.oracle.truffle.js.runtime.builtins.JSArrayBuffer;
import com.oracle.truffle.js.runtime.builtins.JSArrayBufferView;
import com.oracle.truffle.js.runtime.builtins.JSBigInt;
import com.oracle.truffle.js.runtime.builtins.JSBoolean;
import com.oracle.truffle.js.runtime.builtins.JSClass;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSFunctionData;
import com.oracle.truffle.js.runtime.builtins.JSMap;
import com.oracle.truffle.js.runtime.builtins.JSNumber;
import com.oracle.truffle.js.runtime.builtins.JSProxy;
import com.oracle.truffle.js.runtime.builtins.JSSIMD;
import com.oracle.truffle.js.runtime.builtins.JSSet;
import com.oracle.truffle.js.runtime.builtins.JSString;
import com.oracle.truffle.js.runtime.builtins.JSSymbol;
import com.oracle.truffle.js.runtime.builtins.JSUserObject;
import com.oracle.truffle.js.runtime.doubleconv.DoubleConversion;
import com.oracle.truffle.js.runtime.external.DToA;
import com.oracle.truffle.js.runtime.interop.JSJavaWrapper;
import com.oracle.truffle.js.runtime.interop.JavaClass;
import com.oracle.truffle.js.runtime.interop.JavaMethod;
import com.oracle.truffle.js.runtime.interop.JavaPackage;
import com.oracle.truffle.js.runtime.objects.JSAttributes;
import com.oracle.truffle.js.runtime.objects.JSLazyString;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.Null;
import com.oracle.truffle.js.runtime.objects.PropertyDescriptor;
import com.oracle.truffle.js.runtime.objects.PropertyReference;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.truffleinterop.JSInteropNodeUtil;
import com.oracle.truffle.js.runtime.util.JSHashMap;

public final class JSRuntime {
    private static final long NEGATIVE_ZERO_DOUBLE_BITS = Double.doubleToRawLongBits(-0.0);
    private static final long POSITIVE_INFINITY_DOUBLE_BITS = Double.doubleToRawLongBits(Double.POSITIVE_INFINITY);
    public static final String INFINITY_STRING = "Infinity";
    public static final String NEGATIVE_INFINITY_STRING = "-Infinity";
    public static final String POSITIVE_INFINITY_STRING = "+Infinity";
    public static final String NAN_STRING = "NaN";
    public static final double TWO32 = 4294967296d;
    public static final char LINE_SEPARATOR = '\n';
    public static final long INVALID_ARRAY_INDEX = -1;
    public static final long MAX_ARRAY_LENGTH = 4294967295L;
    public static final int MAX_UINT32_DIGITS = 10;
    public static final double MAX_SAFE_INTEGER = Math.pow(2, 53) - 1;
    public static final double MIN_SAFE_INTEGER = -MAX_SAFE_INTEGER;
    public static final long MAX_SAFE_INTEGER_LONG = (long) MAX_SAFE_INTEGER;
    public static final long MIN_SAFE_INTEGER_LONG = (long) MIN_SAFE_INTEGER;
    public static final long INVALID_INTEGER_INDEX = -1;
    public static final int MAX_INTEGER_INDEX_DIGITS = 21;
    public static final int MAX_SAFE_INTEGER_IN_FLOAT = 1 << 24;
    public static final int MIN_SAFE_INTEGER_IN_FLOAT = -MAX_SAFE_INTEGER_IN_FLOAT;
    public static final long MAX_BIG_INT_EXPONENT = Integer.MAX_VALUE;

    public static final String TO_STRING = "toString";
    public static final String VALUE_OF = "valueOf";

    public static final String VALUE = "value";
    public static final String DONE = "done";
    public static final String NEXT = "next";

    public static final String HINT_STRING = "string";
    public static final String HINT_NUMBER = "number";
    public static final String HINT_DEFAULT = "default";

    public static final String PRIMITIVE_VALUE = "PrimitiveValue";

    public static final HiddenKey ITERATED_OBJECT_ID = new HiddenKey("IteratedObject");
    public static final HiddenKey ITERATOR_NEXT_INDEX = new HiddenKey("IteratorNextIndex");
    public static final HiddenKey ENUMERATE_ITERATOR_ID = new HiddenKey("EnumerateIterator");

    public static final int ITERATION_KIND_KEY = 1 << 0;
    public static final int ITERATION_KIND_VALUE = 1 << 1;
    public static final int ITERATION_KIND_KEY_PLUS_VALUE = ITERATION_KIND_KEY | ITERATION_KIND_VALUE;

    private JSRuntime() {
        // this class should not be instantiated
    }

    public static boolean doubleIsRepresentableAsInt(double d) {
        return doubleIsRepresentableAsInt(d, false);
    }

    public static boolean doubleIsRepresentableAsInt(double d, boolean ignoreNegativeZero) {
        long longValue = (long) d;
        return doubleIsRepresentableAsLong(d) && longIsRepresentableAsInt(longValue) && (ignoreNegativeZero || !isNegativeZero(d));
    }

    public static boolean doubleIsRepresentableAsUnsignedInt(double d, boolean ignoreNegativeZero) {
        long longValue = (long) d;
        return doubleIsRepresentableAsLong(d) && longIsRepresentableAsInt(longValue) && (ignoreNegativeZero || !isNegativeZero(d));
    }

    public static boolean isNegativeZero(double d) {
        return Double.doubleToRawLongBits(d) == NEGATIVE_ZERO_DOUBLE_BITS;
    }

    public static boolean isPositiveInfinity(double d) {
        return Double.doubleToRawLongBits(d) == POSITIVE_INFINITY_DOUBLE_BITS;
    }

    public static Number doubleToNarrowestNumber(double d) {
        if (doubleIsRepresentableAsInt(d)) {
            return (int) d;
        }
        return d;
    }

    public static boolean longIsRepresentableAsInt(long value) {
        return value == (int) value;
    }

    public static boolean isRepresentableAsUnsignedInt(long value) {
        return (value & 0xffffffffL) == value;
    }

    public static boolean doubleIsRepresentableAsLong(double d) {
        return d == (long) d;
    }

    public static Object positiveLongToIntOrDouble(long value) {
        if (value <= Integer.MAX_VALUE) {
            return (int) value;
        } else {
            return (double) value;
        }
    }

    public static Object longToIntOrDouble(long value) {
        if (Integer.MIN_VALUE <= value && value <= Integer.MAX_VALUE) {
            return (int) value;
        } else {
            return (double) value;
        }
    }

    public static boolean isNaN(Object value) {
        if (!(value instanceof Double)) {
            return false;
        }
        double d = (Double) value;
        return Double.isNaN(d);
    }

    @TruffleBoundary
    public static String typeof(Object value) {
        if (value == Null.instance) {
            return Null.TYPE_NAME;
        } else if (value == Undefined.instance) {
            return Undefined.TYPE_NAME;
        } else if (isString(value)) {
            return JSString.TYPE_NAME;
        } else if (isNumber(value)) {
            return JSNumber.TYPE_NAME;
        } else if (isBigInt(value)) {
            return JSBigInt.TYPE_NAME;
        } else if (value instanceof Boolean) {
            return JSBoolean.TYPE_NAME;
        } else if (value instanceof Symbol) {
            return JSSymbol.TYPE_NAME;
        } else if (JSObject.isDynamicObject(value)) {
            if (JSProxy.isProxy((DynamicObject) value)) {
                return typeof(JSProxy.getTarget((DynamicObject) value));
            } else if (JSFunction.isJSFunction((DynamicObject) value)) {
                return JSFunction.TYPE_NAME;
            }
            return JSUserObject.TYPE_NAME;
        } else if (JSTruffleOptions.NashornJavaInterop && value instanceof JavaPackage) {
            return JavaPackage.TYPE_NAME;
        } else if (JSTruffleOptions.NashornJavaInterop && value instanceof JavaClass) {
            return JavaClass.TYPE_NAME;
        } else if (JSTruffleOptions.NashornJavaInterop && value instanceof JavaMethod) {
            return JavaMethod.TYPE_NAME;
        } else if (value instanceof TruffleObject) {
            assert !(value instanceof Symbol);
            if (JSInteropNodeUtil.isBoxed((TruffleObject) value)) {
                return typeof(JSInteropNodeUtil.unbox((TruffleObject) value));
            }
            if (ForeignAccess.sendIsExecutable(Message.IS_EXECUTABLE.createNode(), (TruffleObject) value)) {
                return JSFunction.TYPE_NAME;
            } else {
                return JSUserObject.TYPE_NAME;
            }
        } else {
            CompilerDirectives.transferToInterpreter();
            throw new UnsupportedOperationException("typeof: don't know " + value.getClass().getSimpleName());
        }
    }

    /**
     * Returns whether object is a DynamicObject. JS-Null and JS-Undefined are not considered
     * objects.
     */
    public static boolean isObject(Object vo) {
        return JSObject.isDynamicObject(vo) && isObject((DynamicObject) vo);
    }

    /**
     * Returns whether object is a DynamicObject. JS-Null and JS-Undefined are not considered
     * objects.
     */
    public static boolean isObject(DynamicObject vo) {
        ObjectType type = vo.getShape().getObjectType();
        return (type instanceof JSClass) && (type != Null.NULL_CLASS);
    }

    /**
     * Returns whether {@code value} is JS {@code null} or {@code undefined}.
     */
    public static boolean isNullOrUndefined(Object value) {
        return JSObject.isDynamicObject(value) && isNullOrUndefined((DynamicObject) value);
    }

    /**
     * Returns whether {@code value} is JS {@code null} or {@code undefined}.
     */
    public static boolean isNullOrUndefined(DynamicObject value) {
        return value.getShape().getObjectType() == Null.NULL_CLASS;
    }

    /**
     * Implementation of ECMA 7.1.1 "ToPrimitive", with NO hint given.
     *
     * @param value an Object to be converted to a primitive value
     *
     * @return an Object representing the primitive value of the parameter
     */
    @TruffleBoundary
    public static Object toPrimitive(Object value) {
        return toPrimitive(value, HINT_DEFAULT);
    }

    /**
     * Implementation of ECMA 7.1.1 "ToPrimitive".
     *
     * @param value an Object to be converted to a primitive value
     * @param hint the preferred type of primitive to return ("number", "string" or "default")
     *
     * @return an Object representing the primitive value of the parameter
     */
    @TruffleBoundary
    public static Object toPrimitive(Object value, String hint) {
        if (value == Null.instance || value == Undefined.instance) {
            return value;
        } else if (JSTruffleOptions.SIMDJS && JSSIMD.isJSSIMD(value)) {
            return value;
        } else if (JSObject.isJSObject(value)) {
            return JSObject.toPrimitive((DynamicObject) value, hint);
        } else if (isForeignObject(value)) {
            TruffleObject tObj = (TruffleObject) value;
            return toPrimitiveFromForeign(tObj);
        } else {
            return value;
        }
    }

    /**
     * Converting a TruffleObject to a primitive Value. See JSUnboxOrGetNode.
     */
    @TruffleBoundary
    public static Object toPrimitiveFromForeign(TruffleObject tObj) {
        if (JSInteropNodeUtil.isNull(tObj)) {
            return Null.instance;
        } else if (JSInteropNodeUtil.isBoxed(tObj)) {
            try {
                return JSRuntime.importValue(JSInteropNodeUtil.unbox(tObj));
            } catch (Exception e) {
                return Null.instance;
            }
        } else {
            boolean hasSize = JSInteropNodeUtil.hasSize(tObj);
            return JSRuntime.objectToConsoleString(tObj, hasSize ? null : "foreign");
        }
    }

    /**
     * Implementation of ECMA 9.2 "ToBoolean".
     *
     * @param value an Object to be converted to a Boolean
     * @return an Object representing the primitive value of the parameter
     */
    @TruffleBoundary
    public static boolean toBoolean(Object value) {
        if (value == Boolean.TRUE) {
            return true;
        } else if (value == Boolean.FALSE || value == Undefined.instance || value == Null.instance) {
            return false;
        } else if (isNumber(value)) {
            return toBoolean((Number) value);
        } else if (value instanceof String) {
            return ((String) value).length() != 0;
        } else if (isLazyString(value)) {
            return value.toString().length() != 0;
        } else if (value instanceof BigInt) {
            return ((BigInt) value).compareTo(BigInt.ZERO) != 0;
        } else {
            return true;
        }
    }

    public static boolean toBoolean(Number number) {
        double val = doubleValue(number);
        if (val == 0 || Double.isNaN(val)) {
            return false;
        }
        return Boolean.TRUE;
    }

    /**
     * Implementation of ECMA 9.3 "ToNumber".
     *
     * @param value an Object to be converted to a Number
     * @return an Object representing the Number value of the parameter
     */
    @TruffleBoundary
    public static Number toNumber(Object value) {
        Object primitive = isObject(value) ? JSObject.toPrimitive((DynamicObject) value, HINT_NUMBER) : value;
        return toNumberFromPrimitive(primitive);
    }

    @TruffleBoundary
    public static Object toNumeric(Object value) {
        Object primitive = isObject(value) ? JSObject.toPrimitive((DynamicObject) value, HINT_NUMBER) : value;
        if (primitive instanceof BigInt) {
            return primitive;
        } else {
            return toNumberFromPrimitive(primitive);
        }
    }

    @TruffleBoundary
    public static Number toNumberFromPrimitive(Object value) {
        if (CompilerDirectives.injectBranchProbability(CompilerDirectives.LIKELY_PROBABILITY, isNumber(value))) {
            return (Number) value;
        } else if (value == Undefined.instance) {
            return Double.NaN;
        } else if (value == Null.instance) {
            return 0;
        } else if (value instanceof Boolean) {
            return booleanToNumber((Boolean) value);
        } else if (value instanceof String) {
            return stringToNumber((String) value);
        } else if (isLazyString(value)) {
            return stringToNumber(value.toString());
        } else if (value instanceof Symbol) {
            throw Errors.createTypeErrorCannotConvertToNumber("a Symbol value");
        } else if (JSTruffleOptions.NashornJavaInterop && value instanceof Number) {
            return (Number) value; // BigDecimal, BigInteger
        }
        assert false : "coerceToNumber: should never reach here, type " + value.getClass().getSimpleName() + " not handled.";
        throw Errors.createTypeErrorCannotConvertToNumber(safeToString(value));
    }

    public static int booleanToNumber(boolean value) {
        return value ? 1 : 0;
    }

    public static boolean isNumber(Object value) {
        return value instanceof Integer || value instanceof Double || value instanceof Long || value instanceof LargeInteger;
    }

    public static boolean isBigInt(Object value) {
        return value instanceof BigInt;
    }

    public static void ensureBothSameNumericType(Object a, Object b) {
        if ((a instanceof BigInt) != (b instanceof BigInt)) {
            throw Errors.createTypeErrorCanNotMixBigIntWithOtherTypes();
        }
    }

    public static boolean isJavaNumber(Object value) {
        return value instanceof Number;
    }

    /**
     * Implementation of ECMA 9.3.1 "ToNumber Applied to the String Type".
     *
     * @param string
     * @return a Number
     */
    @TruffleBoundary
    public static Number stringToNumber(String string) {
        // "Infinity" written exactly like this
        String strCamel = trimJSWhiteSpace(string);
        if (strCamel.length() == 0) {
            return 0;
        }
        char firstChar = strCamel.charAt(0);
        if (strCamel.length() >= INFINITY_STRING.length() && strCamel.contains(INFINITY_STRING)) {
            return identifyInfinity(strCamel, firstChar);
        }
        if (!(JSRuntime.isAsciiDigit(firstChar) || firstChar == '-' || firstChar == '.' || firstChar == '+')) {
            return Double.NaN;
        }
        return stringToNumberParse(strCamel);
    }

    private static Number stringToNumberParse(String str) {
        assert str.length() > 0;
        boolean hex = str.startsWith("0x") || str.startsWith("0X");
        int eIndex = firstExpIndexInString(str);
        boolean sci = !hex && (0 <= eIndex && eIndex < str.length() - 1);
        try {
            if (sci) {
                return stringToNumberSci(str);
            } else if (str.length() <= 18 && !str.contains(".")) {
                // 18 digits always fit into long
                if (hex) {
                    return Long.valueOf(str.substring(2), 16);
                } else {
                    return stringToNumberLong(str);
                }
            } else {
                return Double.valueOf(str);
            }
        } catch (NumberFormatException e) {
            return Double.valueOf(Double.NaN);
        }
    }

    @TruffleBoundary
    public static Number stringToNumberLong(String strLower) throws NumberFormatException {
        assert strLower.length() > 0;
        long num = Long.parseLong(strLower);
        if (longIsRepresentableAsInt(num)) {
            if (num == 0 && strLower.charAt(0) == '-') {
                return -0.0;
            }
            return (int) num;
        } else {
            return (double) num;
        }
    }

    @TruffleBoundary
    public static double stringToNumberSci(String str) {
        int firstIdx = firstExpIndexInString(str);
        if (firstIdx < 0) {
            return Double.NaN; // no 'e' found
        }
        String part1 = str.substring(0, firstIdx);
        if ("-0".equals(part1)) {
            return -0.0;
        }
        String part2 = str.substring(firstIdx + 1);
        int exponent = Integer.parseInt(part2);
        if (exponent <= -324 || exponent >= 324 || part1.contains(".")) {
            return stringToNumberSciBigExponent(part1, exponent);
        } else {
            return movePointRight(part1, exponent).doubleValue();
        }
    }

    /**
     * Returns the first index of a String that contains either 'e' or 'E'.
     */
    @TruffleBoundary
    public static int firstExpIndexInString(String str) {
        int firstIdx = str.indexOf('e', 0);
        if (firstIdx >= 0) {
            return firstIdx;
        }
        return str.indexOf('E', 0);
    }

    private static double stringToNumberSciBigExponent(String number, int exponent) {
        BigDecimal result = movePointRight(number, exponent);
        BigDecimal resultAbs = result.abs();
        if ((resultAbs.compareTo(BigDecimal.valueOf(Double.MAX_VALUE)) > 0)) {
            if (result.signum() > 0) {
                return Double.POSITIVE_INFINITY;
            } else {
                return Double.NEGATIVE_INFINITY;
            }
        }
        if ((resultAbs.compareTo(BigDecimal.valueOf(Double.MIN_VALUE)) < 0)) {
            if (result.signum() < 0) {
                return -0.0;
            } else {
                return 0;
            }
        }
        return result.doubleValue();
    }

    private static BigDecimal movePointRight(String number, int exponent) {
        // we won't be accurate if we do the decimalization twice
        BigDecimal exp = BigDecimal.TEN.pow(exponent, MathContext.DECIMAL128);
        return new BigDecimal(number).multiply(exp);
    }

    public static double identifyInfinity(String str, char firstChar) {
        int len = str.length();
        int infinityLength = INFINITY_STRING.length();
        if (len == infinityLength) {
            return Double.POSITIVE_INFINITY;
        } else if (len == (infinityLength + 1)) {
            if (firstChar == '+') {
                return Double.POSITIVE_INFINITY;
            } else if (firstChar == '-') {
                return Double.NEGATIVE_INFINITY;
            }
        }
        return Double.NaN;
    }

    /**
     * Implementation of ECMA 9.4 "ToInteger".
     *
     * @param value an Object to be converted to an Integer
     * @return an Object representing the Integer value of the parameter
     */
    public static long toInteger(Object value) {
        Number number = toNumber(value);
        return toInteger(number);
    }

    @TruffleBoundary
    public static long toInteger(Number number) {
        if (isNaN(number)) {
            return 0;
        }
        return number.longValue();
    }

    /**
     * Implementation of ECMAScript6 7.1.15 "ToLength".
     */
    public static long toLength(Object value) {
        long l = toInteger(value);
        return toLength(l);
    }

    public static double toLength(double d) {
        if (d <= 0) {
            return 0;
        }
        if (d > MAX_SAFE_INTEGER) { // also checks for positive infinity
            return MAX_SAFE_INTEGER;
        }
        return d;
    }

    public static long toLength(long l) {
        if (l <= 0) {
            return 0;
        }
        if (l > MAX_SAFE_INTEGER_LONG) {
            return MAX_SAFE_INTEGER_LONG;
        }
        return l;
    }

    public static int toLength(int value) {
        if (value <= 0) {
            return 0;
        }
        return value;
    }

    /**
     * Implementation of ECMA 7.1.10 "ToUInt8".
     *
     * @param value an Object to be converted to a UInt8
     * @return an Object representing the Number value of the parameter
     */
    public static int toUInt8(Object value) {
        Number number = toNumber(value);
        return toUInt8(number);
    }

    @TruffleBoundary
    public static int toUInt8(Number number) {
        if (number instanceof Double) {
            Double d = (Double) number;
            if (isPositiveInfinity(d)) {
                return 0;
            }
        }
        return toUInt8(number.longValue());
    }

    public static int toUInt8(long number) {
        return (int) (number & 0x000000FF);
    }

    /**
     * Implementation of ECMA 7.1.9 "ToInt8".
     *
     * @param value an Object to be converted to a Int8
     * @return an Object representing the Number value of the parameter
     */
    public static int toInt8(Object value) {
        Number number = toNumber(value);
        return toInt8(number);
    }

    @TruffleBoundary
    public static int toInt8(Number number) {
        if (number instanceof Double) {
            Double d = (Double) number;
            if (isPositiveInfinity(d)) {
                return 0;
            }
        }
        return toInt8(number.longValue());
    }

    @TruffleBoundary
    public static int toInt8(long number) {
        int res = (int) (Math.floorMod(number, 256));
        if (res >= 128) {
            res = res - 256;
        }
        return res;
    }

    /**
     * Implementation of ECMA 9.7 "ToUInt16".
     *
     * @param value an Object to be converted to a UInt16
     * @return an Object representing the Number value of the parameter
     */
    public static int toUInt16(Object value) {
        Number number = toNumber(value);
        return toUInt16(number);
    }

    public static int toUInt16(Number number) {
        if (number instanceof Double) {
            Double d = (Double) number;
            if (isPositiveInfinity(d)) {
                return 0;
            }
        }
        return toUInt16(longValue(number));
    }

    public static int toUInt16(long number) {
        return (int) (number & 0x0000FFFF);
    }

    /**
     * Implementation of ECMA 7.1.7 "ToInt16".
     *
     * @param value an Object to be converted to a Int16
     * @return an Object representing the Number value of the parameter
     */
    public static int toInt16(Object value) {
        Number number = toNumber(value);
        return toInt16(number);
    }

    @TruffleBoundary()
    public static int toInt16(Number number) {
        if (number instanceof Double) {
            Double d = (Double) number;
            if (isPositiveInfinity(d)) {
                return 0;
            }
        }
        return toInt16(number.longValue());
    }

    @TruffleBoundary()
    public static int toInt16(long number) {
        int res = (int) (Math.floorMod(number, 65536));
        if (res >= 32768) {
            res = res - 65536;
        }
        return res;
    }

    /**
     * Implementation of ECMA 9.6 "ToUInt32".
     *
     * @param value an Object to be converted to a UInt32
     * @return an Object representing the Number value of the parameter
     */
    public static long toUInt32(Object value) {
        return toUInt32(toNumber(value));
    }

    public static long toUInt32(Number number) {
        if (number instanceof Double) {
            return toUInt32(((Double) number).doubleValue());
        }
        return toUInt32(longValue(number));
    }

    public static long toUInt32(long value) {
        return (value & 0xFFFFFFFFL);
    }

    public static long toUInt32(double value) {
        return toUInt32NoTruncate(truncateDouble(value));
    }

    public static long toUInt32NoTruncate(double value) {
        assert !Double.isFinite(value) || value % 1 == 0;
        double d = doubleModuloTwo32(value);
        return toUInt32((long) d);
    }

    public static double truncateDouble(double value) {
        return Math.signum(value) * JSRuntime.mathFloor(Math.abs(value));
    }

    public static double truncateDouble2(double thing) {
        return (thing < 0) ? JSRuntime.mathCeil(thing) : JSRuntime.mathFloor(thing);
    }

    /**
     * Implementation of ECMA 9.5 "ToInt32".
     *
     * @param value an Object to be converted to a Int32
     * @return an Object representing the Number value of the parameter
     */
    public static int toInt32(Object value) {
        Number number = toNumber(value);
        return toInt32(number);
    }

    /**
     * Convert JS number to int32.
     */
    public static int toInt32(Number number) {
        if (number instanceof Double) {
            return toInt32(((Double) number).doubleValue());
        }
        if (number instanceof Integer) {
            return (int) number;
        }
        if (number instanceof Long) {
            return (int) (long) number;
        }
        return toInt32Intl(number);
    }

    @TruffleBoundary
    private static int toInt32Intl(Number number) {
        return toInt32(number.doubleValue());
    }

    public static int toInt32(double value) {
        return toInt32NoTruncate(truncateDouble(value));
    }

    public static int toInt32NoTruncate(double value) {
        assert !Double.isFinite(value) || value % 1 == 0;
        // equivalent, but slower: double d = value % two32;
        return (int) (long) doubleModuloTwo32(value);
    }

    private static double doubleModuloTwo32(double value) {
        return value - JSRuntime.mathFloor(value / TWO32) * TWO32;
    }

    /**
     * Non-standard "ToDouble" utility function.
     *
     * @return the result of calling ToNumber, but converted to a primitive double
     */
    public static double toDouble(Object value) {
        return doubleValue(toNumber(value));
    }

    /**
     * ToDouble for Numbers. In fact, just forwarding to doubleValue(). Keep it, as otherwise it is
     * very easy to mistakenly call toDouble() inadvertently.
     */
    public static double toDouble(Number value) {
        return doubleValue(value);
    }

    /**
     * Implementation of ECMA 9.8 "ToString".
     *
     * @param value an Object to be converted to a Number
     *
     * @return an Object representing the Number value of the parameter
     */
    @TruffleBoundary
    public static String toString(Object value) {
        if (CompilerDirectives.injectBranchProbability(CompilerDirectives.LIKELY_PROBABILITY, value instanceof String)) {
            return (String) value;
        } else if (isLazyString(value)) {
            return value.toString();
        } else if (value == Undefined.instance) {
            return Undefined.NAME;
        } else if (value == Null.instance) {
            return Null.NAME;
        } else if (value instanceof Boolean) {
            return booleanToString((Boolean) value);
        } else if (isNumber(value)) {
            return numberToString((Number) value);
        } else if (value instanceof Symbol) {
            throw Errors.createTypeErrorCannotConvertToString("a Symbol value");
        } else if (JSObject.isJSObject(value)) {
            return toString(JSObject.toPrimitive((DynamicObject) value, HINT_STRING));
        } else if (JSTruffleOptions.NashornJavaInterop && (value instanceof JavaClass || value instanceof JavaPackage || value instanceof JavaMethod)) {
            return value.toString();
        } else if (value instanceof TruffleObject) {
            assert !(value instanceof Symbol);
            return value.toString();
        } else if (value != null) {
            assert isJSNative(value);
            return value.toString();
        }
        throw toStringTypeError(value);
    }

    /**
     * Converts the value to a String that can be print on the console. This function should not
     * trigger side-effects!
     */
    @TruffleBoundary
    public static String safeToString(Object value) {
        if (value == Undefined.instance) {
            return Undefined.NAME;
        } else if (value == Null.instance) {
            return Null.NAME;
        } else if (JSObject.isJSObject(value)) {
            return JSObject.safeToString((DynamicObject) value);
        } else if (value instanceof Symbol) {
            return value.toString();
        } else {
            return toString(value);
        }
    }

    @TruffleBoundary
    public static String objectToConsoleString(TruffleObject obj, String name) {
        return objectToConsoleString(obj, name, null, null);
    }

    @TruffleBoundary
    public static String objectToConsoleString(TruffleObject obj, String name, String[] internalKeys, Object[] internalValues) {
        assert !JSFunction.isJSFunction(obj) && !JSProxy.isProxy(obj);
        StringBuilder sb = new StringBuilder();

        if (name != null) {
            sb.append(name);
        }
        boolean isAnyArray = false; // also TypedArrays
        boolean isArray = false;
        long length = -1;
        if (isArrayLike(obj)) {
            isAnyArray = true;
            isArray = true;
            length = arrayGetLength(obj);
            sb.append('(');
            sb.append(length);
            sb.append(") ");
        } else if (JSArrayBufferView.isJSArrayBufferView(obj)) {
            isAnyArray = true;
            sb.append('(');
            sb.append(JSArrayBufferView.typedArrayGetLength((DynamicObject) obj));
            sb.append(')');
        } else if (JSString.isJSString(obj)) {
            length = JSString.getStringLength((DynamicObject) obj);
        }
        if (name != null) {
            sb.append(' ');
        }
        boolean isStringObj = JSString.isJSString(obj);
        long prevArrayIndex = -1;

        sb.append(isAnyArray ? '[' : '{');
        int propertyCount = 0;
        for (Object key : ownPropertyKeys(obj, isArray, length)) {
            PropertyDescriptor desc = getOwnProperty(obj, key);
            if ((isAnyArray || isStringObj) && key.equals("length") || (isStringObj && JSRuntime.isArrayIndex(key) && JSRuntime.parseArrayIndexRaw(key.toString()) < length)) {
                // length for arrays is printed as very first item
                // don't print individual characters (and length) for Strings
                continue;
            }
            if (propertyCount > 0) {
                sb.append(", ");
            }
            if (isArray) {
                // merge holes to "undefined (times) (count)" entries
                if (JSRuntime.isArrayIndex(key)) {
                    long index = JSRuntime.parseArrayIndexRaw(key.toString());
                    if ((index < length) && (index != (prevArrayIndex + 1))) {
                        sb.append("undefined \u00d7 ");
                        sb.append((index - prevArrayIndex - 1));
                        sb.append(", ");
                        propertyCount++;
                        if (propertyCount >= JSTruffleOptions.MaxConsolePrintProperties) {
                            sb.append("...");
                            break;
                        }
                    }
                    prevArrayIndex = index;
                } else {
                    if (fillUndefinedArrayRest(sb, length, prevArrayIndex, false)) {
                        sb.append(", ");
                        propertyCount++;
                        if (propertyCount >= JSTruffleOptions.MaxConsolePrintProperties) {
                            sb.append("...");
                            break;
                        }
                    }
                    prevArrayIndex = Math.max(prevArrayIndex, length);
                }
            }
            if (!isAnyArray || !JSRuntime.isArrayIndex(key)) {
                // print keys, but don't print array-indices
                sb.append(key);
                sb.append(": ");
            }
            String valueStr = null;
            if (desc.isDataDescriptor()) {
                Object value = desc.getValue();
                valueStr = toPrintableValue(value);
            } else if (desc.isAccessorDescriptor()) {
                valueStr = "accessor";
            } else {
                valueStr = "empty";
            }
            sb.append(valueStr);
            propertyCount++;
            if (propertyCount >= JSTruffleOptions.MaxConsolePrintProperties) {
                sb.append(", ...");
                break;
            }
        }
        if (isArray && propertyCount < JSTruffleOptions.MaxConsolePrintProperties) {
            // fill "undefined (times) (count)" entries at the end of the array
            if (fillUndefinedArrayRest(sb, length, prevArrayIndex, propertyCount > 0)) {
                propertyCount++;
            }
            prevArrayIndex = Math.max(prevArrayIndex, length);
        }
        if (internalKeys != null) {
            assert internalValues != null;
            appendInternalFields(sb, internalKeys, internalValues, propertyCount <= 0);
        }
        sb.append(isAnyArray ? ']' : '}');
        return sb.toString();
    }

    private static boolean isArrayLike(TruffleObject obj) {
        return JSArray.isJSArray(obj) || (isForeignObject(obj) && JSInteropNodeUtil.hasSize(obj));
    }

    private static PropertyDescriptor getOwnProperty(TruffleObject obj, Object key) {
        if (JSObject.isJSObject(obj)) {
            return JSObject.getOwnProperty((DynamicObject) obj, key);
        } else {
            return PropertyDescriptor.createDataDefault(JSInteropNodeUtil.read(obj, key));
        }
    }

    private static Iterable<Object> ownPropertyKeys(TruffleObject obj, boolean isArray, long size) {
        if (JSObject.isJSObject(obj)) {
            return JSObject.ownPropertyKeys((DynamicObject) obj);
        } else {
            if (isArray) {
                // Foreign arrays don't answer the KEYS message
                // they just have keys from 0 to length-1
                List<Object> keys = new ArrayList<>((int) size);
                for (int i = 0; i < size; i++) {
                    keys.add(i);
                }
                return keys;
            } else {
                try {
                    return JSInteropNodeUtil.keys(obj);
                } catch (Exception ex) {
                    return new ArrayList<>();
                }
            }
        }
    }

    @TruffleBoundary
    private static long arrayGetLength(TruffleObject obj) {
        if (JSArray.isJSArray(obj)) {
            return JSArray.arrayGetLength((DynamicObject) obj);
        } else {
            assert isForeignObject(obj);
            return JSRuntime.toLong(JSRuntime.toNumber(JSInteropNodeUtil.getSize(obj)));
        }
    }

    private static String toPrintableValue(Object value) {
        if (JSObject.isJSObject(value) && value != Null.instance && value != Undefined.instance) {
            return "{...}";
        } else if (value instanceof CharSequence) {
            String valueStr = '"' + value.toString() + '"';
            if (valueStr.length() > 50) {
                valueStr = valueStr.substring(0, 20) + " ... " + valueStr.substring(valueStr.length() - 20);
            }
            return valueStr;
        } else {
            return safeToString(value);
        }
    }

    private static boolean fillUndefinedArrayRest(StringBuilder sb, long arrayLen, long prevArrayIndex, boolean prependComma) {
        if (prevArrayIndex < (arrayLen - 1)) {
            if (prependComma) {
                sb.append(", ");
            }
            sb.append("undefined \u00d7 ");
            sb.append((arrayLen - prevArrayIndex - 1));
            return true;
        }
        return false;
    }

    public static String collectionToConsoleString(DynamicObject obj, String name, JSHashMap map) {
        assert JSMap.isJSMap(obj) || JSSet.isJSSet(obj);
        StringBuilder sb = new StringBuilder();

        boolean isMap = JSMap.isJSMap(obj);
        if (name != null) {
            sb.append(name);
        }
        sb.append('(');
        sb.append(map.size());
        sb.append(") {");

        boolean isFirst = true;
        JSHashMap.Cursor cursor = map.getEntries();
        while (cursor.advance()) {
            Object key = cursor.getKey();
            if (key != null) {
                collectionElementToString(sb, isMap, isFirst, key, cursor.getValue());
                isFirst = false;
            }
        }
        sb.append('}');
        return sb.toString();
    }

    private static void collectionElementToString(StringBuilder sb, boolean isMap, boolean first, Object key, Object value) {
        if (!first) {
            sb.append(", ");
        }
        if (isString(key)) {
            sb.append('"');
            sb.append(key);
            sb.append('"');
        } else if (JSObject.isDynamicObject(key)) {
            sb.append("{...}");
        } else {
            sb.append(key);
        }
        if (isMap) {
            sb.append(" => ");
            sb.append(toPrintableValue(value));
        }
    }

    private static void appendInternalFields(StringBuilder sb, String[] internalKeys, Object[] internalValues, boolean first) {
        assert internalKeys.length == internalValues.length;
        boolean seenProperty = !first;
        for (int i = 0; i < internalKeys.length; i++) {
            if (seenProperty) {
                sb.append(", ");
            }
            sb.append("[[").append(internalKeys[i]).append("]]: ").append(toPrintableValue(internalValues[i]));
            seenProperty = true;
        }
    }

    @TruffleBoundary
    public static JSException toStringTypeError(Object value) {
        String what = (value == null ? Null.NAME : (JSObject.isDynamicObject(value) ? JSObject.defaultToString((DynamicObject) value) : value.getClass().getName()));
        throw Errors.createTypeErrorCannotConvertToString(what);
    }

    public static String booleanToString(boolean value) {
        return value ? JSBoolean.TRUE_NAME : JSBoolean.FALSE_NAME;
    }

    public static String toString(DynamicObject value) {
        if (value == Undefined.instance) {
            return Undefined.NAME;
        } else if (value == Null.instance) {
            return Null.NAME;
        }
        return toString(JSObject.toPrimitive(value, HINT_STRING));
    }

    public static String numberToString(Number number) {
        if (number instanceof Integer) {
            return Boundaries.stringValueOf(((Integer) number).intValue());
        } else if (number instanceof LargeInteger) {
            return doubleToString(((LargeInteger) number).doubleValue());
        } else if (number instanceof Double) {
            return doubleToString((Double) number);
        } else if (number instanceof Long) {
            return Boundaries.stringValueOf(number.longValue());
        }
        CompilerDirectives.transferToInterpreter();
        throw new UnsupportedOperationException("unknown number value: " + number.toString() + " " + number.getClass().getSimpleName());
    }

    public static int length(CharSequence cs) {
        if (cs instanceof String) {
            return ((String) cs).length();
        } else if (cs instanceof JSLazyString) {
            return ((JSLazyString) cs).length();
        }
        return lengthIntl(cs);
    }

    public static int length(CharSequence cs, ConditionProfile profile1, ConditionProfile profile2) {
        if (profile1.profile(cs instanceof String)) {
            return ((String) cs).length();
        } else if (profile2.profile(cs instanceof JSLazyString)) {
            return ((JSLazyString) cs).length();
        }
        return lengthIntl(cs);
    }

    public static String javaToString(Object obj) {
        if (obj instanceof String) {
            return (String) obj;
        } else if (obj instanceof JSLazyString) {
            return ((JSLazyString) obj).toString();
        } else if (obj instanceof PropertyReference) {
            return ((PropertyReference) obj).toString();
        }
        return Boundaries.javaToString(obj);
    }

    @TruffleBoundary
    private static int lengthIntl(CharSequence cs) {
        return cs.length();
    }

    // avoiding a virtual call for equals(), not fail on SVM.
    // No TruffleBoundary, we want this to partially evaluate.
    public static boolean propertyKeyEquals(Object a, Object b) {
        assert isPropertyKey(a);
        if (a instanceof String) {
            if (b instanceof String) {
                return ((String) a).equals(b);
            } else if (b instanceof JSLazyString) {
                return ((String) a).equals(((JSLazyString) b).toString());
            } else {
                return false;
            }
        } else if (a instanceof Symbol) {
            return ((Symbol) a).equals(b);
        } else {
            throw Errors.shouldNotReachHere();
        }
    }

    @TruffleBoundary
    public static String doubleToString(double d, int radix) {
        assert radix >= 2 && radix <= 36;
        if (Double.isNaN(d)) {
            return NAN_STRING;
        } else if (d == Double.POSITIVE_INFINITY) {
            return INFINITY_STRING;
        } else if (d == Double.NEGATIVE_INFINITY) {
            return NEGATIVE_INFINITY_STRING;
        } else if (d == 0) {
            return "0";
        }
        return formatDtoA(d, radix);
    }

    /**
     * 9.8.1 ToString Applied to the Number Type.
     *
     * Better use JSDoubleToStringNode where appropriate.
     */
    public static String doubleToString(double d) {
        if (Double.isNaN(d)) {
            return NAN_STRING;
        } else if (d == Double.POSITIVE_INFINITY) {
            return INFINITY_STRING;
        } else if (d == Double.NEGATIVE_INFINITY) {
            return NEGATIVE_INFINITY_STRING;
        } else if (d == 0) {
            return "0";
        }

        if (doubleIsRepresentableAsInt(d)) {
            return Boundaries.stringValueOf((int) d);
        }

        return formatDtoA(d);
    }

    @TruffleBoundary
    public static String formatDtoA(double value) {
        return DoubleConversion.toShortestString(value);
    }

    @TruffleBoundary
    public static String formatDtoAPrecision(double value, int precision) {
        return DoubleConversion.toPrecision(value, precision);
    }

    @TruffleBoundary
    public static String formatDtoAExponential(double d, int digits) {
        StringBuilder buffer = new StringBuilder();
        DToA.jsDtostr(buffer, DToA.DTOSTR_EXPONENTIAL, digits, d);
        return buffer.toString();
    }

    @TruffleBoundary
    public static String formatDtoAExponential(double d) {
        StringBuilder buffer = new StringBuilder();
        DToA.jsDtostr(buffer, DToA.DTOSTR_STANDARD_EXPONENTIAL, 0, d);
        return buffer.toString();
    }

    @TruffleBoundary
    public static String formatDtoAFixed(double value, int digits) {
        return DoubleConversion.toFixed(value, digits);
    }

    @TruffleBoundary
    public static String formatDtoA(double d, int radix) {
        return DToA.jsDtobasestr(radix, d);
    }

    /**
     * Implementation of ECMA 9.9 "ToObject".
     *
     * @param value an Object to be converted to an Object
     * @return an Object
     */
    public static DynamicObject toObject(JSContext ctx, Object value) {
        checkObjectCoercible(value);
        if (CompilerDirectives.injectBranchProbability(CompilerDirectives.LIKELY_PROBABILITY, JSObject.isDynamicObject(value))) {
            return (DynamicObject) value;
        }
        return toObjectFromPrimitive(ctx, value, true);
    }

    @TruffleBoundary
    public static DynamicObject toObjectFromPrimitive(JSContext ctx, Object value, boolean useJavaWrapper) {
        if (value instanceof Boolean) {
            return JSBoolean.create(ctx, (Boolean) value);
        } else if (value instanceof String) {
            return JSString.create(ctx, (String) value);
        } else if (value instanceof JSLazyString) {
            return JSString.create(ctx, (JSLazyString) value);
        } else if (value instanceof BigInt) {
            return JSBigInt.create(ctx, (BigInt) value);
        } else if (value instanceof PropertyReference) {
            return JSString.create(ctx, value.toString());
        } else if (isNumber(value)) {
            return JSNumber.create(ctx, (Number) value);
        } else if (value instanceof Symbol) {
            return JSSymbol.create(ctx, (Symbol) value);
        } else {
            if (useJavaWrapper) {
                assert !isJSNative(value);
                return JSJavaWrapper.create(ctx, value);
            } else {
                return null;
            }
        }
    }

    /**
     * 9.12 SameValue Algorithm.
     */
    @TruffleBoundary
    public static boolean isSameValue(Object x, Object y) {
        if (x == Undefined.instance && y == Undefined.instance) {
            return true;
        } else if (x == Null.instance && y == Null.instance) {
            return true;
        } else if (x instanceof Integer && y instanceof Integer) {
            return (int) x == (int) y;
        } else if (isNumber(x) && isNumber(y)) {
            double xd = doubleValue((Number) x);
            double yd = doubleValue((Number) y);
            return Double.compare(xd, yd) == 0;
        } else if (isString(x) && isString(y)) {
            return x.toString().equals(y.toString());
        } else if (x instanceof Boolean && y instanceof Boolean) {
            return (boolean) x == (boolean) y;
        } else if (isBigInt(x) && isBigInt(y)) {
            return ((BigInt) x).compareTo((BigInt) y) == 0;
        }
        return x == y;
    }

    @TruffleBoundary
    public static boolean equal(Object a, Object b) {
        if (a == b) {
            return true;
        } else if ((a == Undefined.instance || a == Null.instance) && (b == Undefined.instance || b == Null.instance)) {
            return true;
        } else if (a instanceof Boolean && b instanceof Boolean) {
            return a.equals(b);
        } else if (isString(a) && isString(b)) {
            return a.toString().equals(b.toString());
        } else if (isJavaNumber(a) && isJavaNumber(b)) {
            double da = doubleValue((Number) a);
            double db = doubleValue((Number) b);
            return da == db;
        } else if (JSObject.isDynamicObject(a) && JSObject.isDynamicObject(b)) {
            return a == b;
        } else if (isJavaNumber(a) && isString(b)) {
            return equal(a, stringToNumber(b.toString()));
        } else if (isString(a) && isJavaNumber(b)) {
            return equal(stringToNumber(a.toString()), b);
        } else if (isBigInt(a) && isBigInt(b)) {
            return a.equals(b);
        } else if (isBigInt(a) && isString(b)) {
            return a.equals(stringToBigInt(b.toString()));
        } else if (isString(a) && isBigInt(b)) {
            return b.equals(stringToBigInt(a.toString()));
        } else if (isJavaNumber(a) && isBigInt(b)) {
            double numberVal = doubleValue((Number) a);
            return !Double.isNaN(numberVal) && ((BigInt) b).compareValueTo(numberVal) == 0;
        } else if (isBigInt(a) && isJavaNumber(b)) {
            double numberVal = doubleValue((Number) a);
            return !Double.isNaN(numberVal) && ((BigInt) a).compareValueTo(numberVal) == 0;
        } else if (a instanceof Boolean) {
            return equal(booleanToNumber((Boolean) a), b);
        } else if (b instanceof Boolean) {
            return equal(a, booleanToNumber((Boolean) b));
        } else if (isObject(a)) {
            if (b == Undefined.instance || b == Null.instance) {
                return false;
            }
            return equal(JSObject.toPrimitive((DynamicObject) a), b);
        } else if (isObject(b)) {
            if (a == Undefined.instance || a == Null.instance) {
                return false;
            }
            return equal(a, JSObject.toPrimitive(((DynamicObject) b)));
        } else if (isForeignObject(a) || isForeignObject(b)) {
            return equalInterop(a, b);
        } else {
            return false;
        }
    }

    public static boolean isForeignObject(Object value) {
        return value instanceof TruffleObject && !JSObject.isJSObject(value) && !(value instanceof Symbol) && !(value instanceof JSLazyString) && !(value instanceof LargeInteger) &&
                        !(value instanceof BigInt);
    }

    private static boolean equalInterop(Object a, Object b) {
        assert a != null & b != null;
        final Object defaultValue = null;
        Object primLeft;
        if (isForeignObject(a)) {
            primLeft = JSInteropNodeUtil.toPrimitiveOrDefault((TruffleObject) a, defaultValue);
        } else {
            primLeft = isNullOrUndefined(a) ? Null.instance : a;
        }
        Object primRight;
        if (isForeignObject(b)) {
            primRight = JSInteropNodeUtil.toPrimitiveOrDefault((TruffleObject) b, defaultValue);
        } else {
            primRight = isNullOrUndefined(b) ? Null.instance : b;
        }

        if (primLeft == Null.instance || primRight == Null.instance) {
            // at least one is nullish => both need to be for equality
            return primLeft == primRight;
        } else if (primLeft == defaultValue || primRight == defaultValue) {
            // if both are foreign objects and not null and not boxed, use Java equals
            if (primLeft == defaultValue && primRight == defaultValue) {
                return Boundaries.equals(a, b);
            } else {
                return false; // cannot be equal
            }
        } else {
            assert !isForeignObject(primLeft) && !isForeignObject(primRight);
            return equal(primLeft, primRight);
        }
    }

    @TruffleBoundary
    public static boolean identical(Object a, Object b) {
        if (a == b) {
            if (a instanceof Double) {
                return !Double.isNaN((Double) a);
            }
            return true;
        }
        if (a == Undefined.instance || b == Undefined.instance) {
            return false;
        }
        if (a == Null.instance || b == Null.instance) {
            return false;
        }
        if (isBigInt(a) && isBigInt(b)) {
            return a.equals(b);
        }
        if (isJavaNumber(a) && isJavaNumber(b)) {
            if (a instanceof Integer && b instanceof Integer) {
                return ((Integer) a).intValue() == ((Integer) b).intValue();
            } else {
                return doubleValue((Number) a) == doubleValue((Number) b);
            }
        }
        if ((a instanceof Boolean && b instanceof Boolean)) {
            return a.equals(b);
        }
        if (isString(a) && isString(b)) {
            return a.toString().equals(b.toString());
        }
        TruffleLanguage.Env env = AbstractJavaScriptLanguage.getCurrentEnv();
        if (env.isHostObject(a) && env.isHostObject(b)) {
            return env.asHostObject(a) == env.asHostObject(b);
        }
        return false;
    }

    /**
     * Implementation of the (abstract) function CheckObjectCoercible as defined in ECMA 9.10.
     *
     * @param thisObj
     */
    public static void checkObjectCoercible(Object thisObj) {
        if (thisObj == Undefined.instance || thisObj == Null.instance) {
            throw Errors.createTypeErrorNotObjectCoercible(thisObj);
        }
    }

    /**
     * Implementation of the ToPropertyDescriptor function as defined in ECMA 8.10.5.
     *
     * @return a property descriptor
     */
    @TruffleBoundary
    public static PropertyDescriptor toPropertyDescriptor(Object property) {
        // 1.
        if (!isObject(property)) {
            throw Errors.createTypeErrorNotAnObject(property);
        }
        DynamicObject obj = (DynamicObject) property;
        PropertyDescriptor desc = PropertyDescriptor.createEmpty();

        // 3.
        if (JSObject.hasProperty(obj, JSAttributes.ENUMERABLE)) {
            desc.setEnumerable(toBoolean(JSObject.get(obj, JSAttributes.ENUMERABLE)));
        }
        // 4.
        if (JSObject.hasProperty(obj, JSAttributes.CONFIGURABLE)) {
            desc.setConfigurable(toBoolean(JSObject.get(obj, JSAttributes.CONFIGURABLE)));
        }
        // 5.
        boolean hasValue = JSObject.hasProperty(obj, JSAttributes.VALUE);
        if (hasValue) {
            desc.setValue(JSObject.get(obj, JSAttributes.VALUE));
        }
        // 6.
        boolean hasWritable = JSObject.hasProperty(obj, JSAttributes.WRITABLE);
        if (hasWritable) {
            desc.setWritable(toBoolean(JSObject.get(obj, JSAttributes.WRITABLE)));
        }
        // 7.
        boolean hasGet = JSObject.hasProperty(obj, JSAttributes.GET);
        if (hasGet) {
            Object getter = JSObject.get(obj, JSAttributes.GET);
            if (!JSRuntime.isCallable(getter) && getter != Undefined.instance) {
                throw Errors.createTypeError("Getter must be a function");
            }
            desc.setGet((DynamicObject) getter);
        }
        // 8.
        boolean hasSet = JSObject.hasProperty(obj, JSAttributes.SET);
        if (hasSet) {
            Object setter = JSObject.get(obj, JSAttributes.SET);
            if (!JSRuntime.isCallable(setter) && setter != Undefined.instance) {
                throw Errors.createTypeError("Setter must be a function");
            }
            desc.setSet((DynamicObject) setter);
        }
        // 9.
        if (hasGet || hasSet) {
            if (hasValue || hasWritable) {
                throw Errors.createTypeError("Invalid property. A property cannot both have accessors and be writable or have a value");
            }
        }
        return desc;
    }

    public static int valueInRadix(char c, int radix) {
        int val = valueInRadixIntl(c);
        return val < radix ? val : -1;
    }

    private static int valueInRadixIntl(char c) {
        if (isAsciiDigit(c)) {
            return c - '0';
        }
        if ('a' <= c && c <= 'z') {
            return c - 'a' + 10;
        }
        if ('A' <= c && c <= 'Z') {
            return c - 'A' + 10;
        }
        return -1;
    }

    public static int valueInHex(char c) {
        if (isAsciiDigit(c)) {
            return c - '0';
        }
        if ('a' <= c && c <= 'f') {
            return c - 'a' + 10;
        }
        if ('A' <= c && c <= 'F') {
            return c - 'A' + 10;
        }
        return -1;
    }

    public static boolean isHex(char c) {
        return isAsciiDigit(c) || ('a' <= c && c <= 'f') || ('A' <= c && c <= 'F');
    }

    /**
     * NB: does not check whether the result fits into the uint32 range. The caller is responsible
     * for the range check and must take care not to pass in too long strings.
     *
     * @return parsed unsigned integer value or INVALID_UINT32 if the string is not parsable.
     * @see #isArrayIndex(long)
     */
    @TruffleBoundary
    public static long parseArrayIndexRaw(String string) {
        long value = 0;
        int pos = 0;
        int len = string.length();
        if (len > 1 && string.charAt(pos) == '0') {
            return INVALID_ARRAY_INDEX;
        }
        while (pos < len) {
            char c = string.charAt(pos);
            if (!isAsciiDigit(c)) {
                return INVALID_ARRAY_INDEX;
            }
            value *= 10;
            value += c - '0';
            pos++;
        }
        return value;
    }

    public static String trimJSWhiteSpace(String string) {
        return trimJSWhiteSpace(string, false);
    }

    @TruffleBoundary
    public static String trimJSWhiteSpace(String string, boolean useLineTerminators) {
        int firstIdx = firstNonWhitespaceIndex(string, useLineTerminators);
        int lastIdx = lastNonWhitespaceIndex(string, useLineTerminators);
        if (firstIdx == 0) {
            if (lastIdx == string.length()) {
                return string;
            }
        } else if (firstIdx > lastIdx) {
            return "";
        }
        return string.substring(firstIdx, lastIdx + 1);
    }

    public static int firstNonWhitespaceIndex(String string, boolean useLineTerminators) {
        int idx = 0;
        while ((idx < string.length()) && (isWhiteSpace(string.charAt(idx)) || (useLineTerminators && isLineTerminator(string.charAt(idx))))) {
            idx++;
        }
        return idx;
    }

    public static int lastNonWhitespaceIndex(String string, boolean useLineTerminators) {
        int idx = string.length() - 1;
        while ((idx >= 0) && (isWhiteSpace(string.charAt(idx)) || (useLineTerminators && isLineTerminator(string.charAt(idx))))) {
            idx--;
        }
        return idx;
    }

    public static boolean isWhiteSpace(char cp) {
        return (0x0009 <= cp && cp <= 0x000D) || (0x2000 <= cp && cp <= 0x200A) || cp == 0x0020 || cp == 0x00A0 || cp == 0x1680 || cp == 0x2028 || cp == 0x2029 || cp == 0x202F ||
                        cp == 0x205F || cp == 0x3000 || cp == 0xFEFF || (JSTruffleOptions.U180EWhitespace && cp == 0x180E);
    }

    private static boolean isLineTerminator(char codePoint) {
        switch (codePoint) {
            case 0x000A:
            case 0x000D:
            case 0x2028:
            case 0x2029:
                return true;
            default:
                return false;
        }
    }

    /**
     * Checks whether a long value is within the valid range of array lengths. Note the difference
     * to isArrayIndex, that does not allow the MAX_ARRAY_LENGTH value.
     */
    public static boolean isValidArrayLength(long longValue) {
        return 0L <= longValue && longValue <= MAX_ARRAY_LENGTH; // <= 2^32-1, according to 15.4
    }

    public static boolean isArrayIndex(int intValue) {
        return intValue >= 0;
    }

    public static boolean isArrayIndex(long longValue) {
        return 0L <= longValue && longValue < MAX_ARRAY_LENGTH; // < 2^32-1, according to 15.4
    }

    public static boolean isArrayIndex(double doubleValue) {
        long longValue = (long) doubleValue;
        return longValue == doubleValue && isArrayIndex(longValue);
    }

    public static boolean isArrayIndex(String property) {
        long idx = propertyNameToArrayIndex(property);
        return isArrayIndex(idx);
    }

    public static boolean isArrayIndex(Object property) {
        if (property instanceof Integer) {
            return isArrayIndex((int) property);
        } else if (property instanceof Long) {
            return isArrayIndex((long) property);
        } else if (property instanceof Double) {
            return isArrayIndex((double) property);
        } else {
            long idx = propertyNameToArrayIndex(property.toString());
            return isArrayIndex(idx);
        }
    }

    public static long castArrayIndex(double doubleValue) {
        assert isArrayIndex(doubleValue);
        return (long) doubleValue & 0xffff_ffffL;
    }

    public static long castArrayIndex(long longValue) {
        assert isArrayIndex(longValue);
        return longValue;
    }

    public static boolean isAsciiDigit(char c) {
        return '0' <= c && c <= '9';
    }

    @TruffleBoundary
    public static long propertyNameToArrayIndex(String propertyName) {
        if (propertyName != null && propertyName.length() > 0 && propertyName.length() <= MAX_UINT32_DIGITS) {
            if (isAsciiDigit(propertyName.charAt(0))) {
                return parseArrayIndexRaw(propertyName);
            }
        }
        return INVALID_ARRAY_INDEX;
    }

    public static long propertyKeyToArrayIndex(Object propertyKey) {
        return propertyKey instanceof String ? propertyNameToArrayIndex((String) propertyKey) : INVALID_ARRAY_INDEX;
    }

    @TruffleBoundary
    public static long propertyNameToIntegerIndex(String propertyName) {
        if (propertyName != null && propertyName.length() > 0 && propertyName.length() <= MAX_INTEGER_INDEX_DIGITS) {
            if (isAsciiDigit(propertyName.charAt(0))) {
                return parseArrayIndexRaw(propertyName);
            } else if (propertyName.charAt(0) == '-' && propertyName.length() >= 2 && isAsciiDigit(propertyName.charAt(1))) {
                if (parseArrayIndexRaw(propertyName.substring(1)) != INVALID_ARRAY_INDEX) {
                    // valid numerical index string (but OOB), distinguish from invalid index
                    return Long.MAX_VALUE;
                }
            }
        }
        return INVALID_INTEGER_INDEX;
    }

    public static long propertyKeyToIntegerIndex(Object propertyKey) {
        return propertyKey instanceof String ? propertyNameToIntegerIndex((String) propertyKey) : INVALID_INTEGER_INDEX;
    }

    /**
     * Is value a native JavaScript object or primitive? (excluding Java interoperability)
     */
    public static boolean isJSNative(Object value) {
        return JSObject.isDynamicObject(value) || isJSPrimitive(value);
    }

    public static boolean isJSPrimitive(Object value) {
        return isNumber(value) || value instanceof BigInt || value instanceof Boolean || isString(value) || value == Undefined.instance || value == Null.instance || value instanceof Symbol;
    }

    /**
     * Is value a native JavaScript string.
     */
    public static boolean isString(Object value) {
        return value instanceof String || isLazyString(value);
    }

    /**
     * Is value is a {@link CharSequence} that lazily evaluates to a {@link String}).
     */
    public static boolean isLazyString(Object value) {
        return value instanceof JSLazyString || value instanceof PropertyReference;
    }

    public static boolean isStringClass(Class<?> clazz) {
        return String.class.isAssignableFrom(clazz) || JSLazyString.class.isAssignableFrom(clazz) || PropertyReference.class.isAssignableFrom(clazz);
    }

    public static boolean isJavaObject(Object value) {
        return !isJSNative(value) && !(value instanceof Number) && !(value instanceof TruffleObject);
    }

    public static Object nullToUndefined(Object value) {
        return value == null ? Undefined.instance : value;
    }

    public static Object undefinedToNull(Object value) {
        return value == Undefined.instance ? null : value;
    }

    public static Object toJSNull(Object value) {
        return value == null ? Null.instance : value;
    }

    public static Object toJavaNull(Object value) {
        return value == Null.instance ? null : value;
    }

    @TruffleBoundary
    public static Object jsObjectToJavaObject(Object obj) {
        if (isLazyString(obj)) {
            return obj.toString();
        } else {
            return toJavaNull(undefinedToNull(obj));
        }
    }

    public static boolean isPropertyKey(Object key) {
        return key instanceof String || key instanceof Symbol;
    }

    public static Object boxIndex(long longIndex, ConditionProfile indexInIntRangeConditionProfile) {
        if (indexInIntRangeConditionProfile.profile(longIndex <= Integer.MAX_VALUE)) {
            return (int) longIndex;
        } else {
            return (double) longIndex;
        }
    }

    @TruffleBoundary
    public static BigInt stringToBigInt(String s) {
        try {
            return BigInt.valueOf(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * @see Number#intValue()
     */
    public static int intValue(Number number) {
        if (number instanceof Integer) {
            return ((Integer) number).intValue();
        }
        if (number instanceof Double) {
            return ((Double) number).intValue();
        }
        return intValueVirtual(number);
    }

    @TruffleBoundary
    public static int intValueVirtual(Number number) {
        return number.intValue();
    }

    /**
     * @see Number#doubleValue()
     */
    public static double doubleValue(Number number) {
        if (number instanceof Double) {
            return ((Double) number).doubleValue();
        }
        if (number instanceof Integer) {
            return ((Integer) number).doubleValue();
        }
        return doubleValueVirtual(number);
    }

    @TruffleBoundary
    public static double doubleValueVirtual(Number number) {
        return number.doubleValue();
    }

    /**
     * @see Number#floatValue()
     */
    public static float floatValue(Number n) {
        if (n instanceof Double) {
            return ((Double) n).floatValue();
        }
        if (n instanceof Integer) {
            return ((Integer) n).floatValue();
        }
        return floatValueVirtual(n);
    }

    @TruffleBoundary
    public static float floatValueVirtual(Number n) {
        return n.floatValue();
    }

    /**
     * @see Number#longValue()
     */
    public static long longValue(Number n) {
        if (n instanceof Integer) {
            return ((Integer) n).longValue();
        }
        if (n instanceof Double) {
            return ((Double) n).longValue();
        }
        return longValueVirtual(n);
    }

    @TruffleBoundary
    private static long longValueVirtual(Number n) {
        return n.longValue();
    }

    /**
     * Convert JS number to long.
     */
    public static long toLong(Number value) {
        return longValue(value);
    }

    public static void checkStringLength(String str) {
        if (str.length() > JSTruffleOptions.StringLengthLimit) {
            CompilerDirectives.transferToInterpreter();
            throw Errors.createRangeErrorInvalidStringLength();
        }
    }

    public static boolean stringLengthValid(CharSequence left, CharSequence right) {
        return (length(left) + length(right)) <= JSTruffleOptions.StringLengthLimit;
    }

    /**
     * Concatenate two strings, preallocating the buffer with the exact length of the result.
     */
    @TruffleBoundary
    public static String stringConcat(String first, String second) {
        StringBuilder stringBuilder = new StringBuilder(first.length() + second.length());
        stringBuilder.append(first).append(second);
        return stringBuilder.toString();
    }

    // ES2015, 6.2.4.4, FromPropertyDescriptor
    @TruffleBoundary
    public static DynamicObject fromPropertyDescriptor(PropertyDescriptor desc, JSContext context) {
        if (desc == null) {
            return Undefined.instance;
        }
        DynamicObject obj = JSUserObject.create(context);
        if (desc.hasValue()) {
            JSObject.set(obj, JSAttributes.VALUE, desc.getValue());
        }
        if (desc.hasWritable()) {
            JSObject.set(obj, JSAttributes.WRITABLE, desc.getWritable());
        }
        if (desc.hasGet()) {
            JSObject.set(obj, JSAttributes.GET, desc.getGet());
        }
        if (desc.hasSet()) {
            JSObject.set(obj, JSAttributes.SET, desc.getSet());
        }
        if (desc.hasEnumerable()) {
            JSObject.set(obj, JSAttributes.ENUMERABLE, desc.getEnumerable());
        }
        if (desc.hasConfigurable()) {
            JSObject.set(obj, JSAttributes.CONFIGURABLE, desc.getConfigurable());
        }
        return obj;
    }

    public static Object getArgOrUndefined(Object[] args, int i) {
        return args.length > i ? args[i] : Undefined.instance;
    }

    public static Object getArg(Object[] args, int i, Object defaultValue) {
        return args.length > i ? args[i] : defaultValue;
    }

    public static long getOffset(long start, long length, ConditionProfile profile) {
        if (profile.profile(start < 0)) {
            return Math.max(start + length, 0);
        } else {
            return Math.min(start, length);
        }
    }

    /**
     * Parse a string to a double. Use a long value during parsing, thus you need to ensure the
     * result and intermediate values fit into a long. Returned types are int or double.
     */
    @TruffleBoundary
    public static Object parseRawFitsLong(String string, int radix) {
        char firstChar = string.charAt(0);

        int pos = 0;
        int len = string.length();
        boolean negate = false;

        if (firstChar == '-') {
            pos = 1;
            negate = true;
        } else if (firstChar == '+') {
            pos = 1;
        }
        if (pos == len) {
            throw new NumberFormatException();
        }

        int firstPos = pos;
        long value = 0;
        while (pos < len) {
            char c = string.charAt(pos);
            int cval = JSRuntime.valueInRadix(c, radix);
            if (cval < 0) {
                if (pos != firstPos) {
                    break;
                } else {
                    throw new NumberFormatException();
                }
            }
            value *= radix;
            value += cval;
            pos++;
        }
        if (value == 0 && negate && string.charAt(1) == '0') {
            return -0.0;
        }

        assert value >= 0;
        long signedValue = negate ? -value : value;

        if (value <= Integer.MAX_VALUE) {
            return (int) signedValue;
        } else {
            return (double) signedValue;
        }
    }

    /**
     * Parse a string to a double. Use a double value during parsing.
     */
    @TruffleBoundary
    public static double parseRawDontFitLong(String string, int radix) {
        char firstChar = string.charAt(0);

        int pos = 0;
        int len = string.length();
        boolean negate = false;

        if (firstChar == '-') {
            pos = 1;
            negate = true;
        } else if (firstChar == '+') {
            pos = 1;
        }
        if (pos == len) {
            throw new NumberFormatException();
        }

        int firstPos = pos;
        double value = 0;
        while (pos < len) {
            char c = string.charAt(pos);
            int cval = JSRuntime.valueInRadix(c, radix);
            if (cval < 0) {
                if (pos != firstPos) {
                    break;
                } else {
                    throw new NumberFormatException();
                }
            }
            value *= radix;
            value += cval;
            pos++;
        }
        if (value == 0 && negate && string.charAt(1) == '0') {
            return -0.0;
        }

        assert value >= 0;
        return negate ? -value : value;
    }

    /**
     * ES2015, 7.3.4 CreateDataProperty(O, P, V).
     */
    public static boolean createDataProperty(DynamicObject o, Object p, Object v) {
        assert JSRuntime.isObject(o);
        assert JSRuntime.isPropertyKey(p);
        return JSObject.defineOwnProperty(o, p, PropertyDescriptor.createDataDefault(v));
    }

    /**
     * ES2015, 7.3.6 CreateDataPropertyOrThrow(O, P, V).
     */
    public static boolean createDataPropertyOrThrow(DynamicObject o, Object p, Object v) {
        boolean success = createDataProperty(o, p, v);
        if (!success) {
            throw Errors.createTypeError("cannot create data property");
        }
        return success;
    }

    /**
     * ES2016, 7.3.7 DefinePropertyOrThrow(O, P, desc).
     */
    public static void definePropertyOrThrow(DynamicObject o, Object key, PropertyDescriptor desc, JSContext context) {
        definePropertyOrThrow(o, key, desc, context, "Cannot DefineOwnProperty");
    }

    @TruffleBoundary
    public static void definePropertyOrThrow(DynamicObject o, Object key, PropertyDescriptor desc, JSContext context, String message) {
        assert JSRuntime.isObject(o);
        assert JSRuntime.isPropertyKey(key);
        if (context.isOptionV8CompatibilityMode()) {
            boolean success = JSObject.getJSClass(o).defineOwnProperty(o, key, desc, true);
            assert success; // we should have thrown instead of returning false
        } else if (!JSObject.getJSClass(o).defineOwnProperty(o, key, desc, false)) {
            throw Errors.createTypeError(message);
        }
    }

    public static boolean isPrototypeOf(DynamicObject object, DynamicObject prototype) {
        DynamicObject prototypeChainObject = object;
        do {
            prototypeChainObject = JSObject.getPrototype(prototypeChainObject);
            if (prototypeChainObject == prototype) {
                return true;
            }
        } while (prototypeChainObject != Null.instance);
        return false;
    }

    /**
     * ES2016 7.3.16 CreateArrayFromList(elements).
     */
    public static DynamicObject createArrayFromList(JSContext context, List<Object> list) {
        return JSArray.createConstant(context, Boundaries.listToArray(list));
    }

    /**
     * ES2015 19.1.2.8.1 GetOwnPropertyKeys (O, Type).
     *
     */
    public static DynamicObject getOwnPropertyKeys(JSContext context, DynamicObject object, boolean symbols) {
        return createArrayFromList(context, ownPropertyKeysAsList(object, symbols));
    }

    @TruffleBoundary
    public static List<Object> ownPropertyKeysAsList(DynamicObject object, boolean symbols) {
        List<Object> names = new ArrayList<>();
        for (Object key : JSObject.ownPropertyKeys(object)) {
            if ((symbols && key instanceof Symbol) || (!symbols && key instanceof String)) {
                names.add(key);
            }
        }
        return names;
    }

    /**
     * ES2015 7.2.3 IsCallable(argument).
     */
    public static boolean isCallable(Object value) {
        if (JSFunction.isJSFunction(value)) {
            return true;
        } else if (JSProxy.isProxy(value)) {
            return isCallableProxy((DynamicObject) value);
        }
        return false;
    }

    @TruffleBoundary
    public static boolean isCallableProxy(DynamicObject proxy) {
        assert JSProxy.isProxy(proxy);
        return isCallable(JSProxy.getTarget(proxy));
    }

    /**
     * ES2015 7.2.2 IsArray(argument).
     */
    public static boolean isArray(Object obj) {
        if (JSArray.isJSArray(obj)) {
            return true;
        } else if (JSProxy.isProxy(obj)) {
            DynamicObject proxy = (DynamicObject) obj;
            if (JSProxy.isRevoked(proxy)) {
                throw Errors.createTypeErrorProxyRevoked();
            }
            return isArrayProxy(proxy);
        } else if (isForeignObject(obj)) {
            return JSInteropNodeUtil.hasSize((TruffleObject) obj);
        }
        return false;
    }

    public static boolean isArray(Object obj, ConditionProfile profile1, ConditionProfile profile2, Node hasSizeNode) {
        if (profile1.profile(JSArray.isJSArray(obj))) {
            return true;
        } else if (profile2.profile(JSProxy.isProxy(obj))) {
            DynamicObject proxy = (DynamicObject) obj;
            if (JSProxy.isRevoked(proxy)) {
                throw Errors.createTypeErrorProxyRevoked();
            }
            return isArrayProxy(proxy);
        } else if (isForeignObject(obj)) {
            return JSInteropNodeUtil.hasSize((TruffleObject) obj, hasSizeNode);
        }
        return false;
    }

    @TruffleBoundary
    private static boolean isArrayProxy(DynamicObject proxy) {
        return isArray(JSProxy.getTarget(proxy));
    }

    /**
     * ES2015 7.1.14 ToPropertyKey(argument).
     */
    @TruffleBoundary
    public static Object toPropertyKey(Object arg) {
        if (arg instanceof String) {
            return arg;
        }
        Object key = toPrimitive(arg);
        if (key instanceof Symbol) {
            return key;
        } else if (isString(key)) {
            return key.toString();
        } else if (key instanceof HiddenKey) {
            return key;
        }
        return toString(key);
    }

    /**
     * ES2015 7.3.12 Call(F, V, arguments).
     */
    public static Object call(Object fnObj, Object holder, Object[] arguments) {
        if (JSFunction.isJSFunction(fnObj)) {
            return JSFunction.call((DynamicObject) fnObj, holder, arguments);
        } else if (JSProxy.isProxy(fnObj)) {
            return JSProxy.call((DynamicObject) fnObj, holder, arguments);
        } else if (isForeignObject(fnObj)) {
            return JSInteropNodeUtil.call((TruffleObject) fnObj, arguments);
        } else {
            throw Errors.createTypeErrorNotAFunction(fnObj);
        }
    }

    /**
     * ES2015, 7.1.16 CanonicalNumericIndexString().
     */
    @TruffleBoundary
    public static Object canonicalNumericIndexString(Object arg) {
        assert JSRuntime.isString(arg);
        String s = arg.toString();
        if ("-0".equals(s)) {
            return -0.0;
        }
        Number n = JSRuntime.toNumber(s);
        if (!JSRuntime.toString(n).equals(s)) {
            return Undefined.instance;
        }
        return n;
    }

    /**
     * ES2015, 9.4.5.8 IntegerIndexedElementGet.
     */
    @TruffleBoundary
    public static Object integerIndexedElementGet(DynamicObject thisObj, Object index) {
        assert JSRuntime.isNumber(index);
        assert JSArrayBufferView.isJSArrayBufferView(thisObj);
        DynamicObject buffer = JSArrayBufferView.getArrayBuffer(thisObj);
        if (JSArrayBuffer.isDetachedBuffer(buffer)) {
            throw Errors.createTypeErrorDetachedBuffer();
        }
        if (!JSRuntime.isInteger(index)) {
            return Undefined.instance;
        }
        if (JSRuntime.isNegativeZero(((Number) index).doubleValue())) {
            return Undefined.instance;
        }
        long lIndex = ((Number) index).longValue();
        int length = JSArrayBufferView.typedArrayGetLength(thisObj);
        if (lIndex < 0 || lIndex >= length) {
            return Undefined.instance;
        }
        return JSArrayBufferView.typedArrayGetArrayType(thisObj).getElement(thisObj, lIndex);
    }

    /**
     * ES2015, 7.2.6 IsInteger.
     */
    public static boolean isInteger(Object obj) {
        if (!JSRuntime.isNumber(obj)) {
            return false;
        }
        double d = doubleValue((Number) obj);
        if (Double.isInfinite(d) || Double.isNaN(d)) {
            return false;
        }
        return JSRuntime.doubleIsRepresentableAsLong(d);
    }

    /**
     * ES205, 9.4.5.9 IntegerIndexedElementSet.
     */
    @TruffleBoundary
    public static boolean integerIndexedElementSet(DynamicObject thisObj, Object indexObj, Object value) {
        assert JSRuntime.isNumber(indexObj);
        assert JSArrayBufferView.isJSArrayBufferView(thisObj);
        Number index = (Number) indexObj;
        Number numValue = JSRuntime.toNumber(value);
        DynamicObject buffer = JSArrayBufferView.getArrayBuffer(thisObj);
        if (JSArrayBuffer.isDetachedBuffer(buffer)) {
            throw Errors.createTypeErrorDetachedBuffer();
        }
        if (!isInteger(index)) {
            return false;
        }
        double dIndex = index.doubleValue();
        if (isNegativeZero(dIndex)) {
            return false;
        }
        int length = JSArrayBufferView.typedArrayGetLength(thisObj);
        if (dIndex < 0 || dIndex >= length) {
            return false;
        }
        JSArrayBufferView.typedArrayGetArrayType(thisObj).setElement(thisObj, index.intValue(), numValue, true);
        return true;
    }

    /**
     * ES2015, 9.4.5.9 IntegerIndexedElementSet, simplified version (numIndex is an int already).
     */
    @TruffleBoundary
    public static boolean integerIndexedElementSet(DynamicObject thisObj, int numIndex, Object value) {
        assert JSArrayBufferView.isJSArrayBufferView(thisObj);
        Number numValue = JSRuntime.toNumber(value);
        DynamicObject buffer = JSArrayBufferView.getArrayBuffer(thisObj);
        if (JSArrayBuffer.isDetachedBuffer(buffer)) {
            throw Errors.createTypeErrorDetachedBuffer();
        }
        int length = JSArrayBufferView.typedArrayGetLength(thisObj);
        if (numIndex < 0 || numIndex >= length) {
            return false;
        }
        JSArrayBufferView.typedArrayGetArrayType(thisObj).setElement(thisObj, numIndex, numValue, true);
        return true;
    }

    @TruffleBoundary
    public static double mathFloor(double d) {
        if (Double.isNaN(d)) {
            return Double.NaN;
        }
        if (JSRuntime.isNegativeZero(d)) {
            return -0.0;
        }
        if (JSRuntime.isSafeInteger(d)) {
            long i = (long) d;
            return d < i ? i - 1 : i;
        } else {
            return Math.floor(d);
        }
    }

    @TruffleBoundary
    public static double mathCeil(double d) {
        if (Double.isNaN(d)) {
            return Double.NaN;
        }
        if (JSRuntime.isNegativeZero(d)) {
            return -0.0;
        }
        if (JSRuntime.isSafeInteger(d)) {
            long i = (long) d;
            long result = d > i ? i + 1 : i;
            if (result == 0 && d < 0) {
                return -0.0;
            }
            return result;
        } else {
            return Math.ceil(d);
        }
    }

    @TruffleBoundary
    public static double mathRint(double d) {
        return Math.rint(d);
    }

    /**
     * Compare property keys such that a stable sort using it would maintain the following order.
     * <ol>
     * <li>integer index keys, in ascending numeric index order
     * <li>string keys, in original insertion order
     * <li>symbols keys, in original insertion order
     * </ol>
     */
    public static int comparePropertyKeys(Object key1, Object key2) {
        assert isPropertyKey(key1) && isPropertyKey(key2);
        boolean isString1 = key1 instanceof String;
        boolean isString2 = key2 instanceof String;
        if (isString1 && isString2) {
            String str1 = (String) key1;
            String str2 = (String) key2;
            long index1 = JSRuntime.propertyNameToArrayIndex(str1);
            long index2 = JSRuntime.propertyNameToArrayIndex(str2);
            boolean isIndex1 = isArrayIndex(index1);
            boolean isIndex2 = isArrayIndex(index2);
            if (isIndex1 && isIndex2) {
                return Long.compare(index1, index2);
            } else if (isIndex1) {
                return -1;
            } else if (isIndex2) {
                return 1;
            } else {
                return 0;
            }
        } else if (isString1) {
            return -1;
        } else if (isString2) {
            return 1;
        } else {
            return 0;
        }
    }

    /**
     * Carefully try getting the constructor name, must not throw.
     */
    public static String getConstructorName(DynamicObject receiver) {
        // Try @@toStringTag first
        Object toStringTag = getDataProperty(receiver, Symbol.SYMBOL_TO_STRING_TAG);
        if (JSRuntime.isString(toStringTag)) {
            return JSRuntime.javaToString(toStringTag);
        }

        // Try function name of prototype.constructor
        if (!isProxy(receiver)) {
            DynamicObject prototype = JSObject.getPrototype(receiver);
            if (prototype != Null.instance) {
                Object constructor = getDataProperty(prototype, JSObject.CONSTRUCTOR);
                if (JSFunction.isJSFunction(constructor)) {
                    return JSFunction.getName((DynamicObject) constructor);
                }
            }
        }

        // As a last resort, use class name
        return JSObject.getClassName(receiver);
    }

    public static Object getDataProperty(DynamicObject thisObj, Object key) {
        assert JSRuntime.isPropertyKey(key);
        DynamicObject current = thisObj;
        while (current != Null.instance && current != null && !isProxy(current)) {
            PropertyDescriptor desc = JSObject.getOwnProperty(current, key);
            if (desc != null) {
                if (desc.isDataDescriptor()) {
                    return desc.getValue();
                } else {
                    break;
                }
            }
            current = JSObject.getPrototype(current);
        }
        return null;
    }

    private static boolean isProxy(DynamicObject receiver) {
        return JSProxy.isProxy(receiver) || JSAdapter.isJSAdapter(receiver);
    }

    public static boolean isJSRootNode(RootNode rootNode) {
        return rootNode instanceof JavaScriptRootNode;
    }

    public static boolean isJSFunctionRootNode(RootNode rootNode) {
        return rootNode instanceof JavaScriptRootNode && ((JavaScriptRootNode) rootNode).isFunction();
    }

    public static boolean isSafeInteger(double value) {
        return value >= JSRuntime.MIN_SAFE_INTEGER && value <= JSRuntime.MAX_SAFE_INTEGER;
    }

    public static boolean isSafeInteger(long value) {
        return value >= JSRuntime.MIN_SAFE_INTEGER_LONG && value <= JSRuntime.MAX_SAFE_INTEGER_LONG;
    }

    @TruffleBoundary
    public static JSRealm getFunctionRealm(Object obj, JSRealm currentRealm) {
        if (JSObject.isDynamicObject(obj)) {
            DynamicObject dynObj = (DynamicObject) obj;
            if (JSFunction.isJSFunction(dynObj)) {
                if (JSFunction.isBoundFunction(dynObj)) {
                    return getFunctionRealm(JSFunction.getBoundTargetFunction(dynObj), currentRealm);
                } else {
                    return JSFunction.getRealm(dynObj);
                }
            } else if (JSProxy.isProxy(dynObj)) {
                if (JSProxy.getHandler(dynObj) == Null.instance) {
                    throw Errors.createTypeErrorProxyRevoked();
                }
                return getFunctionRealm(JSProxy.getTarget(dynObj), currentRealm);
            }
        }
        return currentRealm;
    }

    /**
     * ES2016 7.2.4 IsConstructor().
     */
    public static boolean isConstructor(Object constrObj) {
        if (JSFunction.isConstructor(constrObj)) {
            return true;
        } else if (JSProxy.isProxy(constrObj)) {
            return isConstructorProxy((DynamicObject) constrObj);
        }
        return false;
    }

    @TruffleBoundary
    public static boolean isConstructorProxy(DynamicObject constrObj) {
        assert JSProxy.isProxy(constrObj);
        return isConstructor(JSProxy.getTarget(constrObj));
    }

    public static boolean isGenerator(Object genObj) {
        if (JSFunction.isJSFunction(genObj) && JSFunction.isGenerator((DynamicObject) genObj)) {
            return true;
        } else if (JSProxy.isProxy(genObj)) {
            return isGeneratorProxy((DynamicObject) genObj);
        }
        return false;
    }

    @TruffleBoundary
    public static boolean isGeneratorProxy(DynamicObject genObj) {
        assert JSProxy.isProxy(genObj);
        return isGenerator(JSProxy.getTarget(genObj));
    }

    // ES2015: 7.3.17 CreateListFromArrayLike
    @TruffleBoundary
    public static List<Object> createListFromArrayLikeAllowSymbolString(Object obj) {
        if (!isObject(obj)) {
            throw Errors.createTypeErrorNotAnObject(obj);
        }
        DynamicObject jsObj = (DynamicObject) obj;
        long len = JSRuntime.toLength(JSObject.get(jsObj, JSAbstractArray.LENGTH));
        if (len > Integer.MAX_VALUE) {
            throw Errors.createRangeError("range exceeded");
        }
        List<Object> list = new ArrayList<>();
        long index = 0;
        while (index < len) {
            Object next = JSObject.get(jsObj, index);
            if (JSRuntime.isLazyString(next)) {
                next = next.toString();
            }
            if (!(next instanceof String || next instanceof Symbol)) {
                throw Errors.createTypeError("Symbol or String expected");
            }
            Boundaries.listAdd(list, next);
            index++;
        }
        return list;
    }

    @TruffleBoundary
    public static String quote(String value) {
        int pos = 0;
        while (pos < value.length()) {
            char ch = value.charAt(pos);
            if (ch < ' ' || ch == '\\' || ch == '"') {
                break;
            }
            pos++;
        }

        StringBuilder builder = new StringBuilder(value.length() + 2);
        builder.append('"');
        builder.append(value, 0, pos);
        for (int i = pos; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (ch < ' ') {
                if (ch == '\b') {
                    builder.append("\\b");
                } else if (ch == '\f') {
                    builder.append("\\f");
                } else if (ch == '\n') {
                    builder.append("\\n");
                } else if (ch == '\r') {
                    builder.append("\\r");
                } else if (ch == '\t') {
                    builder.append("\\t");
                } else {
                    builder.append("\\u00");
                    builder.append(Character.forDigit((ch & 0xF0) >> 4, 16));
                    builder.append(Character.forDigit((ch & 0x0F), 16));
                }
            } else if (ch == '\\') {
                builder.append("\\\\");
            } else if (ch == '"') {
                builder.append("\\\"");
            } else {
                builder.append(ch);
            }
        }
        builder.append('"');
        return builder.toString();
    }

    public static DynamicObject expectJSObject(TruffleObject to, BranchProfile errorBranch) {
        if (!JSObject.isJSObject(to)) {
            errorBranch.enter();
            throw Errors.createTypeErrorJSObjectExpected();
        }
        return (DynamicObject) to;
    }

    /**
     * Convert the value to a type valid in Truffle Interop. Use ExportValueNode where possible.
     */
    @TruffleBoundary
    public static Object exportValue(Object value) {
        if (JSRuntime.isLazyString(value)) {
            return value.toString();
        } else if (value instanceof LargeInteger) {
            return ((LargeInteger) value).doubleValue();
        } else if (value instanceof TruffleObject) {
            return value;
        } else if (JSRuntime.isJSPrimitive(value)) {
            return value;
        }
        TruffleLanguage.Env env = AbstractJavaScriptLanguage.getCurrentEnv();
        if (value instanceof JavaClass) {
            return env.asGuestValue(((JavaClass) value).getType());
        }
        return env.asGuestValue(value);
    }

    @TruffleBoundary
    public static Object[] exportValueArray(Object[] arr) {
        Object[] newArr = new Object[arr.length];
        for (int i = 0; i < arr.length; i++) {
            newArr[i] = exportValue(arr[i]);
        }
        return newArr;
    }

    /**
     * Convert the value to a type valid in Graal.js, from something received via TruffleInterop.
     * Use JSForeignToJSTypeNode where possible.
     */
    @TruffleBoundary
    public static Object importValue(Object value) {
        if (value == null) {
            return Null.instance;
        } else if (value instanceof Integer || value instanceof Double || value instanceof String || value instanceof Boolean || value instanceof TruffleObject) {
            return value;
        } else if (value instanceof Character) {
            return String.valueOf(value);
        } else if (value instanceof Number) {
            Number numVal = (Number) value;
            if (value instanceof Byte) {
                return ((Byte) numVal).intValue();
            } else if (value instanceof Short) {
                return ((Short) numVal).intValue();
            } else if (value instanceof Long) {
                long lValue = numVal.longValue();
                if (JSRuntime.longIsRepresentableAsInt(lValue)) {
                    return (int) lValue;
                } else if (JSRuntime.MIN_SAFE_INTEGER_LONG <= lValue && lValue <= JSRuntime.MAX_SAFE_INTEGER_LONG) {
                    return LargeInteger.valueOf(lValue);
                }
                return (double) lValue;
            }
            return numVal.doubleValue();
        } else {
            throw Errors.createTypeError("type " + value.getClass().getSimpleName() + " not supported in JavaScript");
        }
    }

    @TruffleBoundary
    public static JSFunctionData getFunctionData(TruffleObject callable) {
        if (JSFunction.isJSFunction(callable)) {
            return JSFunction.getFunctionData((DynamicObject) callable);
        } else if (JSProxy.isProxy(callable)) {
            return getFunctionData(JSProxy.getTarget((DynamicObject) callable));
        }
        return null; // could be a TruffleObject (as Proxy's target)
    }

    public static boolean intIsRepresentableAsFloat(int value) {
        return (MIN_SAFE_INTEGER_IN_FLOAT <= value && value <= MAX_SAFE_INTEGER_IN_FLOAT);
    }

    public static boolean isJavaPrimitive(Object value) {
        return value != null &&
                        value instanceof Boolean ||
                        value instanceof Byte ||
                        value instanceof Short ||
                        value instanceof Integer ||
                        value instanceof Long ||
                        value instanceof Float ||
                        value instanceof Double ||
                        value instanceof Character;
    }
}
