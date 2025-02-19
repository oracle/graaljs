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
package com.oracle.truffle.js.runtime;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.ExactMath;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.dsl.Idempotent;
import com.oracle.truffle.api.interop.InteropException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.nodes.EncapsulatingNodeReference;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.api.object.Property;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.InlinedConditionProfile;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.api.strings.TruffleStringBuilderUTF16;
import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.nodes.JSGuards;
import com.oracle.truffle.js.nodes.access.IsObjectNode;
import com.oracle.truffle.js.nodes.access.IsPrimitiveNode;
import com.oracle.truffle.js.nodes.cast.JSToBigIntNode;
import com.oracle.truffle.js.nodes.cast.JSToBooleanNode;
import com.oracle.truffle.js.nodes.cast.JSToObjectNode;
import com.oracle.truffle.js.nodes.cast.JSToPrimitiveNode;
import com.oracle.truffle.js.nodes.interop.ExportValueNode;
import com.oracle.truffle.js.nodes.interop.ForeignObjectPrototypeNode;
import com.oracle.truffle.js.nodes.interop.ImportValueNode;
import com.oracle.truffle.js.runtime.array.ByteBufferAccess;
import com.oracle.truffle.js.runtime.array.TypedArray;
import com.oracle.truffle.js.runtime.builtins.JSAbstractArray;
import com.oracle.truffle.js.runtime.builtins.JSAdapter;
import com.oracle.truffle.js.runtime.builtins.JSArray;
import com.oracle.truffle.js.runtime.builtins.JSBigInt;
import com.oracle.truffle.js.runtime.builtins.JSBoolean;
import com.oracle.truffle.js.runtime.builtins.JSError;
import com.oracle.truffle.js.runtime.builtins.JSErrorObject;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSFunctionObject;
import com.oracle.truffle.js.runtime.builtins.JSMap;
import com.oracle.truffle.js.runtime.builtins.JSNumber;
import com.oracle.truffle.js.runtime.builtins.JSOrdinary;
import com.oracle.truffle.js.runtime.builtins.JSOverloadedOperatorsObject;
import com.oracle.truffle.js.runtime.builtins.JSProxy;
import com.oracle.truffle.js.runtime.builtins.JSProxyObject;
import com.oracle.truffle.js.runtime.builtins.JSSet;
import com.oracle.truffle.js.runtime.builtins.JSString;
import com.oracle.truffle.js.runtime.builtins.JSSymbol;
import com.oracle.truffle.js.runtime.builtins.JSTypedArrayObject;
import com.oracle.truffle.js.runtime.doubleconv.DoubleConversion;
import com.oracle.truffle.js.runtime.external.DToA;
import com.oracle.truffle.js.runtime.interop.InteropFunction;
import com.oracle.truffle.js.runtime.interop.JSInteropUtil;
import com.oracle.truffle.js.runtime.objects.IteratorRecord;
import com.oracle.truffle.js.runtime.objects.JSAttributes;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSProperty;
import com.oracle.truffle.js.runtime.objects.Null;
import com.oracle.truffle.js.runtime.objects.Nullish;
import com.oracle.truffle.js.runtime.objects.OperatorSet;
import com.oracle.truffle.js.runtime.objects.PropertyDescriptor;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.JSHashMap;

public final class JSRuntime {
    private static final long NEGATIVE_ZERO_DOUBLE_BITS = Double.doubleToRawLongBits(-0.0);
    private static final long POSITIVE_INFINITY_DOUBLE_BITS = Double.doubleToRawLongBits(Double.POSITIVE_INFINITY);
    public static final double TWO32 = 4294967296d;
    public static final long INVALID_ARRAY_INDEX = -1;
    public static final long MAX_ARRAY_LENGTH = 4294967295L;
    public static final int MAX_UINT32_DIGITS = 10;
    public static final double MAX_SAFE_INTEGER = Math.pow(2, 53) - 1;
    public static final double MIN_SAFE_INTEGER = -MAX_SAFE_INTEGER;
    public static final long MAX_SAFE_INTEGER_LONG = (long) MAX_SAFE_INTEGER;
    public static final long MIN_SAFE_INTEGER_LONG = (long) MIN_SAFE_INTEGER;
    public static final long INVALID_INTEGER_INDEX = -1;
    public static final int MAX_INTEGER_INDEX_DIGITS = 16;
    public static final int MAX_SAFE_INTEGER_DIGITS = 16;
    public static final int MAX_SAFE_INTEGER_IN_FLOAT = 1 << 24;
    public static final int MIN_SAFE_INTEGER_IN_FLOAT = -MAX_SAFE_INTEGER_IN_FLOAT;
    public static final long INVALID_SAFE_INTEGER = Long.MIN_VALUE;

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

    public static Number longToIntOrDouble(long value) {
        if (Integer.MIN_VALUE <= value && value <= Integer.MAX_VALUE) {
            return (int) value;
        } else {
            return (double) value;
        }
    }

    public static boolean longFitsInDouble(long l) {
        double d = l;
        return l != Long.MAX_VALUE && (long) d == l;
    }

    public static boolean isNaN(Object value) {
        if (!(value instanceof Double)) {
            return false;
        }
        double d = (Double) value;
        return Double.isNaN(d);
    }

    @TruffleBoundary
    public static TruffleString typeof(Object value) {
        if (value == Null.instance) {
            return Null.TYPE_NAME;
        } else if (value == Undefined.instance) {
            return Undefined.TYPE_NAME;
        } else if (Strings.isTString(value)) {
            return JSString.TYPE_NAME;
        } else if (isNumber(value) || value instanceof Long) {
            return JSNumber.TYPE_NAME;
        } else if (isBigInt(value)) {
            return JSBigInt.TYPE_NAME;
        } else if (value instanceof Boolean) {
            return JSBoolean.TYPE_NAME;
        } else if (value instanceof Symbol) {
            return JSSymbol.TYPE_NAME;
        } else if (JSObject.isJSObject(value)) {
            JSObject object = (JSObject) value;
            if (JSProxy.isJSProxy(object)) {
                Object target = JSProxy.getTargetNonProxy(object);
                return typeof(target);
            } else if (JSFunction.isJSFunction(object)) {
                return JSFunction.TYPE_NAME;
            }
            return JSOrdinary.TYPE_NAME;
        } else if (value instanceof TruffleObject) {
            assert !(value instanceof Symbol);
            JSRealm realm = JSRealm.get(null);
            if (realm.getContext().isOptionNashornCompatibilityMode()) {
                TruffleLanguage.Env env = realm.getEnv();
                if (env.isHostSymbol(value)) {
                    return JSFunction.TYPE_NAME;
                }
            }
            TruffleObject object = (TruffleObject) value;
            InteropLibrary interop = InteropLibrary.getUncached();
            if (interop.isBoolean(object)) {
                return JSBoolean.TYPE_NAME;
            } else if (interop.isString(object)) {
                return JSString.TYPE_NAME;
            } else if (interop.isNumber(object)) {
                return JSNumber.TYPE_NAME;
            } else if (interop.isExecutable(object) || interop.isInstantiable(object)) {
                return JSFunction.TYPE_NAME;
            } else {
                return JSOrdinary.TYPE_NAME;
            }
        } else {
            throw new UnsupportedOperationException("typeof: don't know " + value.getClass().getSimpleName());
        }
    }

    /**
     * Returns whether object is a JSObject. JS-Null and JS-Undefined are not considered objects.
     */
    public static boolean isObject(Object value) {
        return value instanceof JSObject;
    }

    /**
     * Returns whether {@code value} is JS {@code null} or {@code undefined}.
     */
    public static boolean isNullOrUndefined(Object value) {
        return value instanceof Nullish;
    }

    /**
     * Returns whether {@code value} is JS {@code null} or {@code undefined} or a foreign null.
     */
    public static boolean isNullish(Object value) {
        return value == Null.instance || value == Undefined.instance || InteropLibrary.getUncached(value).isNull(value);
    }

    /**
     * Implementation of ECMA 7.1.1 "ToPrimitive", with NO hint given.
     *
     * @param value an Object to be converted to a primitive value
     * @return an Object representing the primitive value of the parameter
     */
    @TruffleBoundary
    public static Object toPrimitive(Object value) {
        return toPrimitive(value, JSToPrimitiveNode.Hint.Default);
    }

    /**
     * Implementation of ECMA 7.1.1 "ToPrimitive".
     *
     * @param value an Object to be converted to a primitive value
     * @param hint the preferred type of primitive to return ("number", "string" or "default")
     * @return an Object representing the primitive value of the parameter
     * @see com.oracle.truffle.js.nodes.cast.JSToPrimitiveNode
     */
    @TruffleBoundary
    public static Object toPrimitive(Object value, JSToPrimitiveNode.Hint hint) {
        return JSToPrimitiveNode.getUncached(hint).execute(value);
    }

    /**
     * Implementation of ECMA 9.2 "ToBoolean".
     *
     * @param value an Object to be converted to a Boolean
     * @return an Object representing the primitive value of the parameter
     */
    @TruffleBoundary
    public static boolean toBoolean(Object value) {
        return JSToBooleanNode.getUncached().executeBoolean(value);
    }

    private static Object toPrimitiveHintNumber(Object value) {
        if (isNumber(value)) {
            // fast path for likely value types
            return value;
        }
        return JSToPrimitiveNode.getUncachedHintNumber().execute(value);
    }

    private static Object toPrimitiveHintString(Object value) {
        return JSToPrimitiveNode.getUncachedHintString().execute(value);
    }

    /**
     * Implementation of ECMA 9.3 "ToNumber".
     *
     * @param value an Object to be converted to a Number
     * @return an Object representing the Number value of the parameter
     */
    @TruffleBoundary
    public static Number toNumber(Object value) {
        Object primitive = toPrimitiveHintNumber(value);
        return toNumberFromPrimitive(primitive);
    }

    @TruffleBoundary
    public static Object toNumeric(Object value) {
        Object primitive = toPrimitiveHintNumber(value);
        if (primitive instanceof BigInt bigInt) {
            if (!bigInt.isForeign()) {
                return primitive;
            } else {
                return bigInt.doubleValue();
            }
        } else if (primitive instanceof Long longValue) {
            return longValue.doubleValue();
        } else {
            return toNumberFromPrimitive(primitive);
        }
    }

    private static Number toNumberFromPrimitive(Object value) {
        CompilerAsserts.neverPartOfCompilation();
        if (isNumber(value)) {
            return (Number) value;
        } else if (value == Undefined.instance) {
            return Double.NaN;
        } else if (value == Null.instance) {
            return 0;
        } else if (value instanceof Boolean bool) {
            return booleanToNumber(bool);
        } else if (value instanceof TruffleString str) {
            return stringToNumber(str);
        } else if (value instanceof Symbol) {
            throw Errors.createTypeErrorCannotConvertToNumber("a Symbol value");
        } else if (value instanceof BigInt bigInt) {
            if (bigInt.isForeign()) {
                return bigInt.doubleValue();
            } else {
                throw Errors.createTypeErrorCannotConvertToNumber("a BigInt value");
            }
        } else if (value instanceof Long longValue) {
            return longValue.doubleValue();
        }
        throw toNumberTypeError(value);
    }

    private static JSException toNumberTypeError(Object value) {
        assert false : "should never reach here, type " + value.getClass().getName() + " not handled.";
        throw Errors.createTypeErrorCannotConvertToNumber(Strings.toJavaString(safeToString(value)));
    }

    public static int booleanToNumber(boolean value) {
        return value ? 1 : 0;
    }

    public static boolean isNumber(Object value) {
        return value instanceof Integer || value instanceof Double || value instanceof SafeInteger;
    }

    @TruffleBoundary
    public static BigInt toBigInt(Object value) {
        return JSToBigIntNode.getUncached().execute(value);
    }

    public static boolean isBigInt(Object value) {
        return value instanceof BigInt;
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
    public static Number stringToNumber(TruffleString string) {
        // "Infinity" written exactly like this
        TruffleString strCamel = trimJSWhiteSpace(string);
        int camelLength = Strings.length(strCamel);
        if (camelLength == 0) {
            return 0;
        }
        char firstChar = Strings.charAt(strCamel, 0);
        if (camelLength >= Strings.length(Strings.INFINITY) && camelLength <= Strings.length(Strings.INFINITY) + 1 && Strings.endsWith(strCamel, Strings.INFINITY)) {
            return identifyInfinity(firstChar, camelLength);
        }
        if (!(JSRuntime.isAsciiDigit(firstChar) || firstChar == '-' || firstChar == '.' || firstChar == '+')) {
            return Double.NaN;
        }

        return stringToNumberParse(strCamel);
    }

    private static Number stringToNumberParse(TruffleString str) {
        assert Strings.length(str) > 0;
        boolean hex = Strings.startsWith(str, Strings.LC_0X) || Strings.startsWith(str, Strings.UC_0X);
        int eIndex = firstExpIndexInString(str);
        boolean sci = !hex && (0 <= eIndex && eIndex < Strings.length(str) - 1);
        try {
            if (!sci && Strings.length(str) <= 18 && Strings.indexOf(str, '.') == -1) {
                // 18 digits always fit into long
                if (hex) {
                    return Strings.parseLong(Strings.lazySubstring(str, 2), 16);
                } else {
                    return stringToNumberLong(str);
                }
            } else {
                return parseDoubleOrNaN(str);
            }
        } catch (TruffleString.NumberFormatException e) {
            return Double.NaN;
        }
    }

    private static Number stringToNumberLong(TruffleString strLower) throws TruffleString.NumberFormatException {
        assert Strings.length(strLower) > 0;
        long num = Strings.parseLong(strLower);
        if (longIsRepresentableAsInt(num)) {
            if (num == 0 && Strings.charAt(strLower, 0) == '-') {
                return -0.0;
            }
            return (int) num;
        } else {
            return (double) num;
        }
    }

    /**
     * Like {@link Double#parseDouble(String)}, but does not allow trailing {@code d} or {@code f}.
     *
     * @return double value or {@link Double#NaN} if not parsable.
     */
    @TruffleBoundary
    public static double parseDoubleOrNaN(TruffleString input) {
        // A valid JS number must end with either a digit or '.'.
        // Double.parseDouble also accepts a trailing 'd', 'D', 'f', 'F'.
        if (Strings.isEmpty(input) || Strings.charAt(input, Strings.length(input) - 1) > '9') {
            return Double.NaN;
        }
        try {
            return Strings.parseDouble(input);
        } catch (TruffleString.NumberFormatException e) {
            return Double.NaN;
        }
    }

    /**
     * Returns the first index of a String that contains either 'e' or 'E'.
     */
    @TruffleBoundary
    public static int firstExpIndexInString(TruffleString str) {
        int firstIdx = Strings.indexOf(str, 'e', 0);
        if (firstIdx >= 0) {
            return firstIdx;
        }
        return Strings.indexOf(str, 'E', 0);
    }

    public static double identifyInfinity(char firstChar, int len) {
        int infinityLength = Strings.length(Strings.INFINITY);
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

    public static long toInteger(Number number) {
        // NaN is converted to 0L by longValue().
        return longValue(number);
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
     * Implementation of ECMA 9.6 "ToUInt32".
     *
     * @param value an Object to be converted to a UInt32
     * @return an Object representing the Number value of the parameter
     */
    public static long toUInt32(Object value) {
        return toUInt32(toNumber(value));
    }

    /**
     * ToUint32 after previous ToNumber conversion.
     */
    public static long toUInt32(Number number) {
        if (number instanceof Integer) {
            return Integer.toUnsignedLong((int) number);
        } else if (number instanceof Double) {
            return toUInt32((double) number);
        } else if (number instanceof SafeInteger) {
            return toUInt32(((SafeInteger) number).longValue());
        }
        return toUInt32(longValueVirtual(number));
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
        return ExactMath.truncate(value);
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
        if (number instanceof SafeInteger) {
            return (int) number.longValue();
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
        return value - Math.floor(value / TWO32) * TWO32;
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

    public static String toJavaString(Object value) {
        return Strings.toJavaString(toString(value));
    }

    /**
     * The abstract operation ToString. Converts a value to a string.
     */
    @TruffleBoundary
    public static TruffleString toString(Object value) {
        if (value instanceof TruffleString) {
            return (TruffleString) value;
        } else if (value == Undefined.instance) {
            return Undefined.NAME;
        } else if (value == Null.instance) {
            return Null.NAME;
        } else if (value instanceof Boolean) {
            return booleanToString((Boolean) value);
        } else if (isNumber(value) || value instanceof Long) {
            return numberToString((Number) value);
        } else if (value instanceof Symbol) {
            throw Errors.createTypeErrorCannotConvertToString("a Symbol value");
        } else if (value instanceof BigInt) {
            return Strings.fromBigInt((BigInt) value);
        } else if (JSObject.isJSObject(value)) {
            return toString(toPrimitiveHintString(value));
        } else if (value instanceof TruffleObject) {
            assert !isJSNative(value);
            return toString(toPrimitiveHintString(value));
        }
        throw toStringTypeError(value);
    }

    @TruffleBoundary
    public static TruffleString safeToString(Object value) {
        return toDisplayString(value, false, ToDisplayStringFormat.getDefaultFormat());
    }

    @TruffleBoundary
    public static TruffleString toDisplayString(Object value, boolean allowSideEffects) {
        return toDisplayString(value, allowSideEffects, ToDisplayStringFormat.getDefaultFormat());
    }

    @TruffleBoundary
    public static TruffleString toDisplayString(Object value, boolean allowSideEffects, ToDisplayStringFormat format) {
        return toDisplayStringImpl(value, allowSideEffects, format, 0, null);
    }

    @TruffleBoundary
    public static TruffleString toDisplayStringInner(Object value, boolean allowSideEffects, ToDisplayStringFormat format, int currentDepth, Object parent) {
        return toDisplayStringImpl(value, allowSideEffects, format.withQuoteString(true), currentDepth + 1, parent);
    }

    /**
     * Converts the value to a String that can be printed on the console and used in error messages.
     *
     * @param format formatting parameters
     * @param depth current recursion depth (starts at 0 = top level)
     * @param parent parent object or null
     */
    public static TruffleString toDisplayStringImpl(Object value, boolean allowSideEffects, ToDisplayStringFormat format, int depth, Object parent) {
        CompilerAsserts.neverPartOfCompilation();
        if (value == parent) {
            return Strings.PARENS_THIS;
        } else if (value == Undefined.instance) {
            return Undefined.NAME;
        } else if (value == Null.instance) {
            return Null.NAME;
        } else if (value instanceof Boolean) {
            return booleanToString((Boolean) value);
        } else if (value instanceof TruffleString str) {
            return format.quoteString() ? quote(str) : str;
        } else if (value instanceof String str) {
            return format.quoteString() ? quote(Strings.fromJavaString(str)) : Strings.fromJavaString(str);
        } else if (JSObject.isJSObject(value)) {
            return ((JSObject) value).toDisplayStringImpl(allowSideEffects, format, depth);
        } else if (value instanceof Symbol) {
            return ((Symbol) value).toTString();
        } else if (value instanceof BigInt) {
            return Strings.concat(((BigInt) value).toTString(), Strings.N);
        } else if (isNumber(value)) {
            Number number = (Number) value;
            if (JSRuntime.isNegativeZero(number.doubleValue())) {
                return Strings.NEGATIVE_ZERO;
            } else {
                return numberToString(number);
            }
        } else if (value instanceof InteropFunction) {
            return toDisplayStringImpl(((InteropFunction) value).getFunction(), allowSideEffects, format, depth, parent);
        } else if (value instanceof TruffleObject) {
            assert !isJSNative(value) : value;
            return foreignToString(value, allowSideEffects, format, depth);
        } else {
            return Strings.fromObject(value);
        }
    }

    @TruffleBoundary
    public static TruffleString objectToDisplayString(JSDynamicObject obj, boolean allowSideEffects, ToDisplayStringFormat format, int depth, TruffleString name) {
        return objectToDisplayString(obj, allowSideEffects, format, depth, name, null, null);
    }

    @TruffleBoundary
    public static TruffleString objectToDisplayString(JSDynamicObject obj, boolean allowSideEffects, ToDisplayStringFormat format, int depth, TruffleString name, TruffleString[] internalKeys,
                    Object[] internalValues) {
        assert !JSFunction.isJSFunction(obj) && !JSProxy.isJSProxy(obj);
        boolean v8CompatMode = JSObject.getJSContext(obj).isOptionV8CompatibilityMode();
        var sb = Strings.builderCreate();

        if (name != null) {
            Strings.builderAppend(sb, name);
        }
        boolean isArrayLike = false; // also TypedArrays
        boolean isArray = false;
        long length = -1;
        if (JSArray.isJSArray(obj)) {
            isArrayLike = true;
            isArray = true;
            length = JSArray.arrayGetLength(obj);
        } else if (obj instanceof JSTypedArrayObject typedArrayObj) {
            isArrayLike = true;
            length = typedArrayObj.getLength();
        } else if (JSString.isJSString(obj)) {
            length = JSString.getStringLength(obj);
        }
        boolean isStringObj = JSString.isJSString(obj);
        long prevArrayIndex = -1;

        if (isArrayLike) {
            if (length > 0) {
                boolean topLevel = depth == 0;
                if (depth >= format.getMaxDepth() || (!topLevel && length > format.getMaxElements())) {
                    if (name == null) {
                        Strings.builderAppend(sb, Strings.UC_ARRAY);
                    }
                    Strings.builderAppend(sb, Strings.PAREN_OPEN);
                    Strings.builderAppend(sb, length);
                    Strings.builderAppend(sb, Strings.PAREN_CLOSE);
                    return Strings.builderToString(sb);
                } else if (topLevel && length >= 2 && !v8CompatMode && format.includeArrayLength()) {
                    Strings.builderAppend(sb, Strings.PAREN_OPEN);
                    Strings.builderAppend(sb, length);
                    Strings.builderAppend(sb, Strings.PAREN_CLOSE);
                }
            }
        } else if (depth >= format.getMaxDepth()) {
            Strings.builderAppend(sb, Strings.EMPTY_OBJECT_DOTS);
            return Strings.builderToString(sb);
        }

        char chr1 = isArrayLike ? '[' : '{';
        Strings.builderAppend(sb, chr1);
        int propertyCount = 0;
        for (Object key : JSObject.ownPropertyKeys(obj)) {
            if (!allowSideEffects && JSError.STACK_NAME.equals(key)) {
                Property prop = obj.getShape().getProperty(JSError.STACK_NAME);
                if (prop != null && JSProperty.isProxy(prop)) {
                    // stack PropertyProxy may have side effects
                    continue;
                }
            }
            PropertyDescriptor desc = JSObject.getOwnProperty(obj, key);
            if ((isArrayLike || isStringObj) && key.equals(Strings.LENGTH) ||
                            (isStringObj && JSRuntime.isArrayIndex(key) && JSRuntime.parseArrayIndexIsIndexRaw(key) < length)) {
                // length for arrays is printed as very first item
                // don't print individual characters (and length) for Strings
                continue;
            }
            if (propertyCount > 0) {
                Strings.builderAppend(sb, v8CompatMode ? "," : ", ");
                if (propertyCount >= format.getMaxElements()) {
                    Strings.builderAppend(sb, Strings.DOT_DOT_DOT);
                    break;
                }
            }
            if (isArray) {
                // merge holes to "empty (times) (count)" entries
                if (JSRuntime.isArrayIndex(key)) {
                    long index = JSRuntime.parseArrayIndexIsIndexRaw(key);
                    if ((index < length) && fillEmptyArrayElements(sb, index, prevArrayIndex, false)) {
                        Strings.builderAppend(sb, ", ");
                        propertyCount++;
                        if (propertyCount >= format.getMaxElements()) {
                            Strings.builderAppend(sb, "...");
                            break;
                        }
                    }
                    prevArrayIndex = index;
                } else {
                    if (fillEmptyArrayElements(sb, length, prevArrayIndex, false)) {
                        Strings.builderAppend(sb, Strings.COMMA_SPC);
                        propertyCount++;
                        if (propertyCount >= format.getMaxElements()) {
                            Strings.builderAppend(sb, "...");
                            break;
                        }
                    }
                    prevArrayIndex = Math.max(prevArrayIndex, length);
                }
            }
            if (!isArrayLike || !JSRuntime.isArrayIndex(key)) {
                // print keys, but don't print array-indices
                Strings.builderAppend(sb, Strings.fromObject(key));
                Strings.builderAppend(sb, ": ");
            }
            TruffleString valueStr;
            if (desc.isDataDescriptor()) {
                Object value = desc.getValue();
                valueStr = toDisplayStringInner(value, allowSideEffects, format, depth, obj);
            } else if (desc.isAccessorDescriptor()) {
                valueStr = Strings.ACCESSOR;
            } else {
                valueStr = Strings.EMPTY;
            }
            Strings.builderAppend(sb, valueStr);
            propertyCount++;
        }
        if (isArray && propertyCount < format.getMaxElements()) {
            // fill "empty (times) (count)" entries at the end of the array
            if (fillEmptyArrayElements(sb, length, prevArrayIndex, propertyCount > 0)) {
                propertyCount++;
            }
        }
        if (internalKeys != null) {
            assert internalValues != null && internalKeys.length == internalValues.length;
            for (int i = 0; i < internalKeys.length; i++) {
                if (propertyCount > 0) {
                    Strings.builderAppend(sb, Strings.COMMA_SPC);
                }
                Strings.builderAppend(sb, Strings.BRACKET_OPEN_2);
                Strings.builderAppend(sb, internalKeys[i]);
                Strings.builderAppend(sb, Strings.BRACKET_CLOSE_2_COLON);
                Strings.builderAppend(sb, toDisplayStringInner(internalValues[i], allowSideEffects, format, depth, obj));
                propertyCount++;
            }
        }
        char chr = isArrayLike ? ']' : '}';
        Strings.builderAppend(sb, chr);
        return Strings.builderToString(sb);
    }

    private static TruffleString foreignToString(Object value, boolean allowSideEffects, ToDisplayStringFormat format, int depth) {
        CompilerAsserts.neverPartOfCompilation();
        try {
            InteropLibrary interop = InteropLibrary.getUncached(value);
            if (interop.isNull(value)) {
                return Strings.NULL;
            } else if (interop.hasArrayElements(value)) {
                return foreignArrayToString(value, allowSideEffects, format, depth);
            } else if (interop.isString(value)) {
                return format.quoteString() ? Strings.fromJavaString(quote(interop.asString(value))) : Strings.interopAsTruffleString(value);
            } else if (interop.isBoolean(value)) {
                return booleanToString(interop.asBoolean(value));
            } else if (interop.isNumber(value)) {
                Object unboxed = Strings.UC_NUMBER;
                if (interop.fitsInInt(value)) {
                    unboxed = interop.asInt(value);
                } else if (interop.fitsInLong(value)) {
                    unboxed = interop.asLong(value);
                } else if (interop.fitsInDouble(value)) {
                    unboxed = interop.asDouble(value);
                }
                return JSRuntime.toDisplayString(unboxed, allowSideEffects, format);
            } else if ((JavaScriptLanguage.getCurrentEnv()).isHostObject(value)) {
                return hostObjectToString(value, interop);
            } else if (interop.isMetaObject(value)) {
                return Strings.interopAsTruffleString(interop.getMetaQualifiedName(value));
            } else if (interop.hasMembers(value) && !(interop.isExecutable(value) || interop.isInstantiable(value))) {
                return foreignObjectToString(value, allowSideEffects, format, depth);
            } else {
                return Strings.interopAsTruffleString(interop.toDisplayString(value, allowSideEffects));
            }
        } catch (InteropException e) {
            return Strings.UC_OBJECT;
        }
    }

    private static TruffleString hostObjectToString(Object value, InteropLibrary interop) throws UnsupportedMessageException {
        if (interop.isMetaObject(value)) {
            return Strings.concatAll(Strings.JAVA_CLASS_BRACKET, Strings.interopAsTruffleString(interop.getMetaQualifiedName(value)), Strings.BRACKET_CLOSE);
        } else {
            Object metaObject = interop.getMetaObject(value);
            return Strings.concatAll(Strings.JAVA_OBJECT_BRACKET, Strings.interopAsTruffleString(InteropLibrary.getUncached().getMetaQualifiedName(metaObject)), Strings.BRACKET_CLOSE);
        }
    }

    private static TruffleString foreignArrayToString(Object truffleObject, boolean allowSideEffects, ToDisplayStringFormat format, int depth) throws InteropException {
        CompilerAsserts.neverPartOfCompilation();
        InteropLibrary interop = InteropLibrary.getFactory().getUncached(truffleObject);
        assert interop.hasArrayElements(truffleObject);
        long size = interop.getArraySize(truffleObject);
        if (size == 0) {
            return Strings.EMPTY_ARRAY;
        } else if (depth >= format.getMaxDepth()) {
            return Strings.concatAll(Strings.ARRAY_PAREN_OPEN, Strings.fromLong(size), Strings.PAREN_CLOSE);
        }
        boolean topLevel = depth == 0;
        var sb = Strings.builderCreate();
        if (topLevel && size >= 2 && format.includeArrayLength()) {
            Strings.builderAppend(sb, Strings.PAREN_OPEN);
            Strings.builderAppend(sb, size);
            Strings.builderAppend(sb, Strings.PAREN_CLOSE);
        }
        Strings.builderAppend(sb, '[');
        for (long i = 0; i < size; i++) {
            if (i > 0) {
                Strings.builderAppend(sb, Strings.COMMA_SPC);
                if (i >= format.getMaxElements()) {
                    Strings.builderAppend(sb, Strings.DOT_DOT_DOT);
                    break;
                }
            }
            Object value = interop.readArrayElement(truffleObject, i);
            Strings.builderAppend(sb, toDisplayStringInner(value, allowSideEffects, format, depth, truffleObject));
        }
        Strings.builderAppend(sb, ']');
        return Strings.builderToString(sb);
    }

    private static TruffleString foreignObjectToString(Object truffleObject, boolean allowSideEffects, ToDisplayStringFormat format, int depth) throws InteropException {
        CompilerAsserts.neverPartOfCompilation();
        InteropLibrary objInterop = InteropLibrary.getFactory().getUncached(truffleObject);
        assert objInterop.hasMembers(truffleObject);
        if (allowSideEffects && objInterop.isMemberInvocable(truffleObject, Strings.TO_STRING_JLS)) {
            return toString(objInterop.invokeMember(truffleObject, Strings.TO_STRING_JLS));
        }
        Object keys = objInterop.getMembers(truffleObject);
        InteropLibrary keysInterop = InteropLibrary.getFactory().getUncached(keys);
        long keyCount = keysInterop.getArraySize(keys);
        if (keyCount == 0) {
            return Strings.EMPTY_OBJECT;
        } else if (depth >= format.getMaxDepth()) {
            return Strings.EMPTY_OBJECT_DOTS;
        }
        var sb = Strings.builderCreate();
        Strings.builderAppend(sb, '{');
        for (long i = 0; i < keyCount; i++) {
            if (i > 0) {
                Strings.builderAppend(sb, Strings.COMMA_SPC);
                if (i >= format.getMaxElements()) {
                    Strings.builderAppend(sb, Strings.DOT_DOT_DOT);
                    break;
                }
            }
            Object key = keysInterop.readArrayElement(keys, i);
            assert InteropLibrary.getUncached().isString(key);
            String stringKey = Strings.interopAsString(key);
            Object value = objInterop.readMember(truffleObject, stringKey);
            Strings.builderAppend(sb, stringKey);
            Strings.builderAppend(sb, Strings.COLON_SPACE);
            Strings.builderAppend(sb, toDisplayStringInner(value, allowSideEffects, format, depth, truffleObject));
        }
        Strings.builderAppend(sb, '}');
        return Strings.builderToString(sb);
    }

    private static boolean fillEmptyArrayElements(TruffleStringBuilderUTF16 sb, long index, long prevArrayIndex, boolean prependComma) {
        if (prevArrayIndex < (index - 1)) {
            if (prependComma) {
                Strings.builderAppend(sb, Strings.COMMA_SPC);
            }
            long count = index - prevArrayIndex - 1;
            if (count == 1) {
                Strings.builderAppend(sb, Strings.EMPTY);
            } else {
                Strings.builderAppend(sb, Strings.EMPTY_X);
                Strings.builderAppend(sb, count);
            }
            return true;
        }
        return false;
    }

    public static TruffleString collectionToConsoleString(JSDynamicObject obj, boolean allowSideEffects, ToDisplayStringFormat format, TruffleString name, JSHashMap map, int depth) {
        assert JSMap.isJSMap(obj) || JSSet.isJSSet(obj);
        assert name != null;
        int size = map.size();
        var sb = Strings.builderCreate();
        Strings.builderAppend(sb, name);
        Strings.builderAppend(sb, Strings.PAREN_OPEN);
        Strings.builderAppend(sb, size);
        Strings.builderAppend(sb, Strings.PAREN_CLOSE);
        if (size > 0 && depth < format.getMaxDepth()) {
            Strings.builderAppend(sb, '{');
            boolean isMap = JSMap.isJSMap(obj);
            boolean isFirst = true;
            JSHashMap.Cursor cursor = map.getEntries();
            while (cursor.advance()) {
                Object key = cursor.getKey();
                if (key != null) {
                    if (!isFirst) {
                        Strings.builderAppend(sb, Strings.COMMA_SPC);
                    }
                    Strings.builderAppend(sb, toDisplayStringInner(key, allowSideEffects, format, depth, obj));
                    if (isMap) {
                        Strings.builderAppend(sb, Strings.BIG_ARROW_SPACES);
                        Object value = cursor.getValue();
                        Strings.builderAppend(sb, toDisplayStringInner(value, allowSideEffects, format, depth, obj));
                    }
                    isFirst = false;
                }
            }
            Strings.builderAppend(sb, '}');
        }
        return Strings.builderToString(sb);
    }

    @TruffleBoundary
    private static JSException toStringTypeError(Object value) {
        String what = (value == null ? "null" : (JSDynamicObject.isJSDynamicObject(value) ? Strings.toJavaString(JSObject.defaultToString((JSDynamicObject) value)) : value.getClass().getName()));
        throw Errors.createTypeErrorCannotConvertToString(what);
    }

    public static TruffleString booleanToString(boolean value) {
        return value ? JSBoolean.TRUE_NAME : JSBoolean.FALSE_NAME;
    }

    public static TruffleString toString(JSDynamicObject value) {
        if (value == Undefined.instance) {
            return Undefined.NAME;
        } else if (value == Null.instance) {
            return Null.NAME;
        }
        return toString(toPrimitiveHintString(value));
    }

    public static TruffleString numberToString(Number number) {
        CompilerAsserts.neverPartOfCompilation();
        if (number instanceof Integer) {
            return Strings.fromInt(((Integer) number).intValue());
        } else if (number instanceof SafeInteger) {
            return doubleToString(((SafeInteger) number).doubleValue());
        } else if (number instanceof Double) {
            return doubleToString((Double) number);
        } else if (number instanceof Long) {
            return Strings.fromLong(number.longValue());
        }
        throw new UnsupportedOperationException("unknown number value: " + number.toString() + " " + number.getClass().getSimpleName());
    }

    // avoiding a virtual call for equals(), not fail on SVM.
    // No TruffleBoundary, we want this to partially evaluate.
    public static boolean propertyKeyEquals(TruffleString.EqualNode equalsNode, Object a, Object b) {
        assert isPropertyKey(a);
        if (a instanceof TruffleString) {
            if (b instanceof TruffleString) {
                return Strings.equals(equalsNode, (TruffleString) a, (TruffleString) b);
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
    public static Object doubleToString(double d, int radix) {
        assert radix >= 2 && radix <= 36;
        if (Double.isNaN(d)) {
            return Strings.NAN;
        } else if (d == Double.POSITIVE_INFINITY) {
            return Strings.INFINITY;
        } else if (d == Double.NEGATIVE_INFINITY) {
            return Strings.NEGATIVE_INFINITY;
        } else if (d == 0) {
            return Strings.ZERO;
        }
        return formatDtoA(d, radix);
    }

    /**
     * 9.8.1 ToString Applied to the Number Type.
     * <p>
     * Better use JSDoubleToStringNode where appropriate.
     */
    public static TruffleString doubleToString(double d) {
        if (Double.isNaN(d)) {
            return Strings.NAN;
        } else if (d == Double.POSITIVE_INFINITY) {
            return Strings.INFINITY;
        } else if (d == Double.NEGATIVE_INFINITY) {
            return Strings.NEGATIVE_INFINITY;
        } else if (d == 0) {
            return Strings.ZERO;
        }

        if (doubleIsRepresentableAsInt(d)) {
            return Strings.fromInt((int) d);
        }

        return Strings.fromJavaString(formatDtoA(d));
    }

    @TruffleBoundary
    public static String formatDtoA(double value) {
        return DoubleConversion.toShortest(value);
    }

    @TruffleBoundary
    public static Object formatDtoAPrecision(double value, int precision) {
        return Strings.fromJavaString(DoubleConversion.toPrecision(value, precision));
    }

    @TruffleBoundary
    public static Object formatDtoAExponential(double d, int digits) {
        return Strings.fromJavaString(DoubleConversion.toExponential(d, digits));
    }

    @TruffleBoundary
    public static Object formatDtoAExponential(double d) {
        return Strings.fromJavaString(DoubleConversion.toExponential(d, -1));
    }

    @TruffleBoundary
    public static Object formatDtoAFixed(double value, int digits) {
        return Strings.fromJavaString(DoubleConversion.toFixed(value, digits));
    }

    @TruffleBoundary
    public static Object formatDtoA(double d, int radix) {
        return Strings.fromJavaString(DToA.jsDtobasestr(radix, d));
    }

    /**
     * Implementation of ECMA 9.9 "ToObject".
     *
     * @param value an Object to be converted to an Object
     * @return an Object
     */
    public static Object toObject(Object value) {
        return JSToObjectNode.getUncached().execute(value);
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
        } else if ((isNumber(x) || x instanceof Long) && (isNumber(y) || y instanceof Long)) {
            double xd = doubleValue((Number) x);
            double yd = doubleValue((Number) y);
            return Double.compare(xd, yd) == 0;
        } else if (Strings.isTString(x) && Strings.isTString(y)) {
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
        } else if (isNullOrUndefined(a)) {
            return isNullish(b);
        } else if (isNullOrUndefined(b)) {
            return isNullish(a);
        } else if (a instanceof Boolean && b instanceof Boolean) {
            return a.equals(b);
        } else if (Strings.isTString(a) && Strings.isTString(b)) {
            return Strings.equals((TruffleString) a, (TruffleString) b);
        } else if (isNumber(a) && isNumber(b)) {
            double da = doubleValue((Number) a);
            double db = doubleValue((Number) b);
            return da == db;
        } else if (isNumber(a) && Strings.isTString(b)) {
            return equal(a, stringToNumber((TruffleString) b));
        } else if (Strings.isTString(a) && isNumber(b)) {
            return equal(stringToNumber((TruffleString) a), b);
        } else if (isBigInt(a) && isBigInt(b)) {
            return a.equals(b);
        } else if (isBigInt(a) && Strings.isTString(b)) {
            return a.equals(stringToBigInt((TruffleString) b));
        } else if (Strings.isTString(a) && isBigInt(b)) {
            return b.equals(stringToBigInt((TruffleString) a));
        } else if (isNumber(a) && isBigInt(b)) {
            return equalBigIntAndNumber((BigInt) b, (Number) a);
        } else if (isBigInt(a) && isNumber(b)) {
            return equalBigIntAndNumber((BigInt) a, (Number) b);
        } else if (a instanceof Boolean) {
            return equal(booleanToNumber((Boolean) a), b);
        } else if (b instanceof Boolean) {
            return equal(a, booleanToNumber((Boolean) b));
        } else if (isObject(a)) {
            assert !isNullOrUndefined(b);
            if (JSOverloadedOperatorsObject.hasOverloadedOperators(a)) {
                if (isObject(b) && !JSOverloadedOperatorsObject.hasOverloadedOperators(b)) {
                    return equal(a, JSObject.toPrimitive((JSDynamicObject) b));
                }
                if (isObject(b) || isNumber(b) || isBigInt(b) || Strings.isTString(b)) {
                    return equalOverloaded(a, b);
                } else {
                    return false;
                }
            } else if (IsPrimitiveNode.getUncached().executeBoolean(b)) {
                if (isNullish(b)) {
                    return false;
                }
                return equal(JSObject.toPrimitive((JSDynamicObject) a), b);
            }
        } else if (isObject(b)) {
            assert !isNullOrUndefined(a) && !isObject(a);
            if (JSOverloadedOperatorsObject.hasOverloadedOperators(b)) {
                if (isNumber(a) || isBigInt(a) || Strings.isTString(a)) {
                    return equalOverloaded(a, b);
                } else {
                    return false;
                }
            } else if (IsPrimitiveNode.getUncached().executeBoolean(a)) {
                if (isNullish(a)) {
                    return false;
                }
                return equal(a, JSObject.toPrimitive((JSDynamicObject) b));
            }
        }
        if (JSGuards.isForeignObjectOrNumber(a) || JSGuards.isForeignObjectOrNumber(b)) {
            return equalInterop(a, b);
        }
        return false;
    }

    public static boolean isForeignObject(Object value) {
        return value instanceof TruffleObject && isForeignObject((TruffleObject) value);
    }

    public static boolean isForeignObject(TruffleObject value) {
        return !JSDynamicObject.isJSDynamicObject(value) && !(value instanceof Symbol) && !(value instanceof SafeInteger) &&
                        !(value instanceof BigInt) && !Strings.isTString(value);
    }

    private static boolean equalInterop(Object a, Object b) {
        assert a != null && b != null && (JSGuards.isForeignObjectOrNumber(a) || JSGuards.isForeignObjectOrNumber(b));
        boolean isAPrimitive = IsPrimitiveNode.getUncached().executeBoolean(a);
        boolean isBPrimitive = IsPrimitiveNode.getUncached().executeBoolean(b);
        if (!isAPrimitive && !isBPrimitive) {
            // If both are of type Object, don't attempt ToPrimitive conversion.
            return InteropLibrary.getUncached(a).isIdentical(a, b, InteropLibrary.getUncached(b));
        }
        // If at least one is nullish => both need to be nullish to be equal
        if (isNullish(a)) {
            return isNullish(b);
        } else if (isNullish(b)) {
            assert !isNullish(a);
            return false;
        }
        // If one of them is primitive, we attempt to convert the other one ToPrimitive.
        // Foreign primitive values always have to be converted to JS primitive values.
        Object primA = !isAPrimitive || JSGuards.isForeignObjectOrNumber(a) ? toPrimitive(a) : a;
        Object primB = !isBPrimitive || JSGuards.isForeignObjectOrNumber(b) ? toPrimitive(b) : b;
        // Now that both are primitive values, we can compare them using normal JS semantics.
        assert !isForeignObject(primA) && !isForeignObject(primB);
        primA = primA instanceof Long ? BigInt.valueOf((long) primA) : primA;
        primB = primB instanceof Long ? BigInt.valueOf((long) primB) : primB;
        return equal(primA, primB);
    }

    private static boolean equalBigIntAndNumber(BigInt a, Number b) {
        if (b instanceof Double || b instanceof Float) {
            double numberVal = doubleValue(b);
            return !Double.isNaN(numberVal) && a.compareValueTo(numberVal) == 0;
        } else {
            return a.compareValueTo(longValue(b)) == 0;
        }
    }

    private static boolean equalOverloaded(Object a, Object b) {
        Object operatorImplementation = OperatorSet.getOperatorImplementation(a, b, Strings.SYMBOL_EQUALS_EQUALS);
        if (operatorImplementation == null) {
            return false;
        } else {
            return toBoolean(call(operatorImplementation, Undefined.instance, new Object[]{a, b}));
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
        if (a == Null.instance) {
            assert b != Undefined.instance;
            return InteropLibrary.getUncached(b).isNull(b);
        } else if (b == Null.instance) {
            assert a != Undefined.instance;
            return InteropLibrary.getUncached(a).isNull(a);
        }
        if (isBigInt(a) && isBigInt(b)) {
            return a.equals(b);
        }
        if (isNumber(a) && isNumber(b)) {
            if (a instanceof Integer && b instanceof Integer) {
                return ((Integer) a).intValue() == ((Integer) b).intValue();
            } else {
                return doubleValue((Number) a) == doubleValue((Number) b);
            }
        }
        if ((a instanceof Boolean && b instanceof Boolean)) {
            return a.equals(b);
        }
        if (Strings.isTString(a) && Strings.isTString(b)) {
            return Strings.equals((TruffleString) a, (TruffleString) b);
        }
        if (isObject(a) || isObject(b)) {
            return false;
        }
        boolean isAForeign = JSGuards.isForeignObjectOrNumber(a);
        boolean isBForeign = JSGuards.isForeignObjectOrNumber(b);
        if (!isAForeign && !isBForeign) {
            return false;
        }
        InteropLibrary aInterop = InteropLibrary.getUncached(a);
        InteropLibrary bInterop = InteropLibrary.getUncached(b);
        if (aInterop.isNumber(a) && bInterop.isNumber(b)) {
            try {
                if (isAForeign != isBForeign) {
                    if (a instanceof BigInt) {
                        assert !(b instanceof BigInt) : b;
                        return false;
                    } else if (b instanceof BigInt) {
                        assert !(a instanceof BigInt) : a;
                        return false;
                    }
                } else {
                    assert isAForeign && isBForeign && !(a instanceof BigInt || b instanceof BigInt);
                }
                if (aInterop.fitsInDouble(a) && bInterop.fitsInDouble(b)) {
                    return doubleValue(aInterop.asDouble(a)) == doubleValue(bInterop.asDouble(b));
                } else if (aInterop.fitsInLong(a) && bInterop.fitsInLong(b)) {
                    return aInterop.asLong(a) == bInterop.asLong(b);
                } else if (aInterop.fitsInBigInteger(a) && bInterop.fitsInBigInteger(b)) {
                    return BigInt.fromBigInteger(aInterop.asBigInteger(a)).compareTo(BigInt.fromBigInteger(bInterop.asBigInteger(b))) == 0;
                }
            } catch (UnsupportedMessageException e) {
                assert false : e;
            }
        }
        return aInterop.isIdentical(a, b, bInterop) || (aInterop.isNull(a) && bInterop.isNull(b));
    }

    /**
     * Implementation of the abstract operation RequireObjectCoercible.
     */
    public static <T> T requireObjectCoercible(T argument) {
        if (argument == Undefined.instance || argument == Null.instance || (isForeignObject(argument) && InteropLibrary.getUncached(argument).isNull(argument))) {
            throw Errors.createTypeErrorNotObjectCoercible(argument, null);
        }
        return argument;
    }

    /**
     * Implementation of the ToPropertyDescriptor function as defined in ECMA 8.10.5.
     *
     * @return a property descriptor
     */
    @TruffleBoundary
    public static PropertyDescriptor toPropertyDescriptor(Object property) {
        // 1.
        if (!isObject(property) && !isForeignObject(property)) {
            throw Errors.createTypeErrorNotAnObject(property);
        }
        PropertyDescriptor desc = PropertyDescriptor.createEmpty();

        // 3.
        if (hasProperty(property, JSAttributes.ENUMERABLE)) {
            desc.setEnumerable(toBoolean(get(property, JSAttributes.ENUMERABLE)));
        }
        // 4.
        if (hasProperty(property, JSAttributes.CONFIGURABLE)) {
            desc.setConfigurable(toBoolean(get(property, JSAttributes.CONFIGURABLE)));
        }
        // 5.
        boolean hasValue = hasProperty(property, JSAttributes.VALUE);
        if (hasValue) {
            desc.setValue(get(property, JSAttributes.VALUE));
        }
        // 6.
        boolean hasWritable = hasProperty(property, JSAttributes.WRITABLE);
        if (hasWritable) {
            desc.setWritable(toBoolean(get(property, JSAttributes.WRITABLE)));
        }
        // 7.
        boolean hasGet = hasProperty(property, JSAttributes.GET);
        if (hasGet) {
            Object getter = get(property, JSAttributes.GET);
            if (!JSRuntime.isCallable(getter) && getter != Undefined.instance) {
                throw Errors.createTypeError("Getter must be a function");
            }
            desc.setGet(getter);
        }
        // 8.
        boolean hasSet = hasProperty(property, JSAttributes.SET);
        if (hasSet) {
            Object setter = get(property, JSAttributes.SET);
            if (!JSRuntime.isCallable(setter) && setter != Undefined.instance) {
                throw Errors.createTypeError("Setter must be a function");
            }
            desc.setSet(setter);
        }
        // 9.
        if (hasGet || hasSet) {
            if (hasValue || hasWritable) {
                throw Errors.createTypeError("Invalid property. A property cannot both have accessors and be writable or have a value");
            }
        }
        return desc;
    }

    public static int valueInRadix10(char c) {
        if (isAsciiDigit(c)) {
            return c - '0';
        }
        return -1;
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

    @TruffleBoundary
    public static long parseArrayIndexIsIndexRaw(Object o) {
        assert isArrayIndex(o);
        assert Strings.isTString(o) || o instanceof Number;
        return parseArrayIndexRaw(o instanceof TruffleString str ? str : Strings.fromNumber((Number) o), TruffleString.ReadCharUTF16Node.getUncached());
    }

    /**
     * NB: does not check whether the result fits into the uint32 range. The caller is responsible
     * for the range check and must take care not to pass in too long strings.
     *
     * @return parsed unsigned integer value or INVALID_UINT32 if the string is not parsable.
     * @see #isArrayIndex(long)
     */
    public static long parseArrayIndexRaw(TruffleString string, TruffleString.ReadCharUTF16Node charAtNode) {
        long value = 0;
        int pos = 0;
        int len = Strings.length(string);
        if (len > 1 && Strings.charAt(charAtNode, string, pos) == '0') {
            return INVALID_ARRAY_INDEX;
        }
        while (pos < len) {
            char c = Strings.charAt(charAtNode, string, pos);
            if (!isAsciiDigit(c)) {
                return INVALID_ARRAY_INDEX;
            }
            value *= 10;
            value += c - '0';
            pos++;
        }
        return value;
    }

    @TruffleBoundary
    public static TruffleString trimJSWhiteSpace(TruffleString string) {
        int firstIdx = firstNonWhitespaceIndex(string, TruffleString.ReadCharUTF16Node.getUncached());
        int lastIdx = lastNonWhitespaceIndex(string, TruffleString.ReadCharUTF16Node.getUncached());
        if (firstIdx == 0) {
            if ((lastIdx + 1) == Strings.length(string)) {
                return string;
            }
        } else if (firstIdx > lastIdx) {
            return Strings.EMPTY_STRING;
        }
        // using unconditional lazy substring because the string doesn't escape in the only caller
        // of this method, stringToNumber
        return Strings.lazySubstring(string, firstIdx, lastIdx + 1 - firstIdx);
    }

    public static int firstNonWhitespaceIndex(TruffleString string, TruffleString.ReadCharUTF16Node charAtNode) {
        int idx = 0;
        int len = Strings.length(string);
        while (idx < len) {
            char ch = Strings.charAt(charAtNode, string, idx);
            if (!isWhiteSpaceOrLineTerminator(ch)) {
                break;
            }
            idx++;
        }
        return idx;
    }

    public static int lastNonWhitespaceIndex(TruffleString string, TruffleString.ReadCharUTF16Node charAtNode) {
        int idx = Strings.length(string) - 1;
        while (idx >= 0) {
            char ch = Strings.charAt(charAtNode, string, idx);
            if (!isWhiteSpaceOrLineTerminator(ch)) {
                break;
            }
            idx--;
        }
        return idx;
    }

    /**
     * Union of WhiteSpace and LineTerminator (StrWhiteSpaceChar). Used by TrimString.
     */
    public static boolean isWhiteSpaceOrLineTerminator(int cp) {
        return switch (cp) {
            // @formatter:off
            case 0x0009, 0x000B, 0x000C, 0x0020, 0x00A0, 0x1680, 0x2000, 0x2001, 0x2002, 0x2003, 0x2004, 0x2005, 0x2006, 0x2007, 0x2008, 0x2009, 0x200A, 0x202F, 0x205F, 0x3000, 0xFEFF,
                            0x000A, 0x000D, 0x2028, 0x2029 -> true;
            // @formatter:on
            default -> false;
        };
    }

    /**
     * WhiteSpace (excluding LineTerminator).
     */
    public static boolean isWhiteSpaceExcludingLineTerminator(char cp) {
        return switch (cp) {
            case 0x0009, 0x000B, 0x000C, 0x0020, 0x00A0, 0x1680, 0x2000, 0x2001, 0x2002, 0x2003, 0x2004, 0x2005, 0x2006, 0x2007, 0x2008, 0x2009, 0x200A, 0x202F, 0x205F, 0x3000, 0xFEFF -> true;
            default -> false;
        };
    }

    public static boolean isLineTerminator(char codePoint) {
        return switch (codePoint) {
            case 0x000A, 0x000D, 0x2028, 0x2029 -> true;
            default -> false;
        };
    }

    public static boolean isAsciiWhitespace(char cp) {
        return switch (cp) {
            case 0x0009, 0x000A, 0x000C, 0x000D, 0x0020 -> true;
            default -> false;
        };
    }

    /**
     * Checks whether a long value is within the valid range of array lengths. Note the difference
     * to isArrayIndex, that does not allow the MAX_ARRAY_LENGTH value.
     */
    public static boolean isValidArrayLength(long longValue) {
        return 0L <= longValue && longValue <= MAX_ARRAY_LENGTH; // <= 2^32-1, according to 15.4
    }

    public static boolean isValidArrayLength(double doubleValue) {
        long longValue = (long) doubleValue;
        return doubleValue == longValue && isValidArrayLength(longValue);
    }

    public static boolean isValidArrayLength(int intValue) {
        return intValue >= 0;
    }

    public static boolean isIntegerIndex(long longValue) {
        return 0L <= longValue && longValue <= MAX_SAFE_INTEGER_LONG;
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

    public static boolean isArrayIndexString(TruffleString property) {
        long idx = propertyNameToArrayIndex(property, TruffleString.ReadCharUTF16Node.getUncached());
        return isArrayIndex(idx);
    }

    @Idempotent
    public static boolean isArrayIndex(Object property) {
        if (property instanceof Integer) {
            return isArrayIndex((int) property);
        } else if (property instanceof Long) {
            return isArrayIndex((long) property);
        } else if (property instanceof Double) {
            return isArrayIndex((double) property);
        } else if (property instanceof TruffleString propertyName) {
            long idx = propertyNameToArrayIndex(propertyName, TruffleString.ReadCharUTF16Node.getUncached());
            return isArrayIndex(idx);
        } else {
            return false;
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

    public static boolean isAsciiDigit(int c) {
        return '0' <= c && c <= '9';
    }

    @TruffleBoundary
    public static long propertyNameToArrayIndex(TruffleString propertyName, TruffleString.ReadCharUTF16Node charAtNode) {
        if (propertyName != null && arrayIndexLengthInRange(propertyName)) {
            if (isAsciiDigit(Strings.charAt(propertyName, 0))) {
                return parseArrayIndexRaw(propertyName, charAtNode);
            }
        }
        return INVALID_ARRAY_INDEX;
    }

    public static boolean arrayIndexLengthInRange(TruffleString indexStr) {
        int len = Strings.length(indexStr);
        return 0 < len && len <= JSRuntime.MAX_UINT32_DIGITS;
    }

    public static long propertyKeyToArrayIndex(Object propertyKey) {
        return propertyKey instanceof TruffleString propertyName ? propertyNameToArrayIndex(propertyName, TruffleString.ReadCharUTF16Node.getUncached()) : INVALID_ARRAY_INDEX;
    }

    @TruffleBoundary
    public static long propertyNameToIntegerIndex(TruffleString propertyName) {
        if (propertyName != null && Strings.length(propertyName) > 0 && Strings.length(propertyName) <= MAX_INTEGER_INDEX_DIGITS) {
            if (isAsciiDigit(Strings.charAt(propertyName, 0))) {
                return parseArrayIndexRaw(propertyName, TruffleString.ReadCharUTF16Node.getUncached());
            }
        }
        return INVALID_INTEGER_INDEX;
    }

    public static long propertyKeyToIntegerIndex(Object propertyKey) {
        return propertyKey instanceof TruffleString propertyName ? propertyNameToIntegerIndex(propertyName) : INVALID_INTEGER_INDEX;
    }

    /**
     * Is value a native JavaScript object or primitive?
     */
    public static boolean isJSNative(Object value) {
        return JSDynamicObject.isJSDynamicObject(value) || isJSPrimitive(value);
    }

    public static boolean isJSPrimitive(Object value) {
        return isNumber(value) || value instanceof Long || value instanceof BigInt || value instanceof Boolean || Strings.isTString(value) || value == Undefined.instance || value == Null.instance ||
                        value instanceof Symbol;
    }

    public static Object nullToUndefined(Object value) {
        return value == null ? Undefined.instance : value;
    }

    public static Object toJSNull(Object value) {
        return value == null ? Null.instance : value;
    }

    public static Object toJavaNull(Object value) {
        return value == Null.instance ? null : value;
    }

    public static boolean isPropertyKey(Object key) {
        return Strings.isTString(key) || key instanceof Symbol;
    }

    public static TruffleString propertyKeyToFunctionNameString(Object key) {
        assert JSRuntime.isPropertyKey(key) : key;
        if (key instanceof TruffleString) {
            return (TruffleString) key;
        } else {
            return ((Symbol) key).toFunctionNameString();
        }
    }

    @TruffleBoundary
    public static BigInt stringToBigInt(TruffleString s) {
        try {
            return Strings.parseBigInt(s);
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
        if (number instanceof SafeInteger) {
            return ((SafeInteger) number).doubleValue();
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
        if (n instanceof SafeInteger) {
            return ((SafeInteger) n).longValue();
        }
        return longValueVirtual(n);
    }

    @TruffleBoundary
    private static long longValueVirtual(Number n) {
        return n.longValue();
    }

    // ES2015, 6.2.4.4, FromPropertyDescriptor
    @TruffleBoundary
    public static JSDynamicObject fromPropertyDescriptor(PropertyDescriptor desc, JSContext context) {
        if (desc == null) {
            return Undefined.instance;
        }
        JSObject obj = JSOrdinary.create(context, JSRealm.get(null));
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

    public static long getOffset(long start, long length, Node node, InlinedConditionProfile profile) {
        if (profile.profile(node, start < 0)) {
            return Math.max(start + length, 0);
        } else {
            return Math.min(start, length);
        }
    }

    public static int getOffset(int start, int length, Node node, InlinedConditionProfile profile) {
        if (profile.profile(node, start < 0)) {
            return Math.max(start + length, 0);
        } else {
            return Math.min(start, length);
        }
    }

    @TruffleBoundary
    public static long parseSafeInteger(TruffleString s) {
        return parseSafeInteger(s, 0, Strings.length(s), 10);
    }

    @TruffleBoundary
    public static long parseSafeInteger(TruffleString s, int beginIndex, int endIndex, int radix) {
        return parseLong(s, beginIndex, endIndex, radix, radix == 10, MAX_SAFE_INTEGER_LONG);
    }

    /**
     * Parses the substring as a signed long in the safe integer range in the specified radix.
     *
     * @return parsed integer value or {@link #INVALID_SAFE_INTEGER} if the string is not parsable
     *         or not in the safe integer range.
     */
    private static long parseLong(TruffleString s, int beginIndex, int endIndex, int radix, boolean parseSign, long limit) {
        assert beginIndex >= 0 && beginIndex <= endIndex && endIndex <= Strings.length(s);
        assert radix >= Character.MIN_RADIX && radix <= Character.MAX_RADIX;
        assert limit <= Long.MAX_VALUE / radix - radix;

        boolean negative = false;
        int i = beginIndex;
        if (i >= endIndex) { // ""
            return INVALID_SAFE_INTEGER;
        }
        if (parseSign) {
            char firstChar = Strings.charAt(s, i);
            if (firstChar < '0') {
                if (firstChar == '-') {
                    negative = true;
                } else if (firstChar != '+') {
                    return INVALID_SAFE_INTEGER;
                }
                i++;
            }
            if (i >= endIndex) { // "-", "+"
                return INVALID_SAFE_INTEGER;
            }
        }

        long result = 0;
        while (i < endIndex) {
            char c = Strings.charAt(s, i);
            int digit = JSRuntime.valueInRadix(c, radix);
            if (digit < 0) {
                return INVALID_SAFE_INTEGER;
            }
            result *= radix;
            result += digit;
            if (result > limit) {
                return INVALID_SAFE_INTEGER;
            }
            i++;
        }
        assert result >= 0;
        if (negative && result == 0) { // "-0"
            return INVALID_SAFE_INTEGER;
        }
        return negative ? -result : result;
    }

    @TruffleBoundary
    public static Number parseRawFitsLong(TruffleString string, int radix, int startPos, int endPos, boolean negate) {
        assert startPos < endPos;
        int pos = startPos;

        long value = 0;
        while (pos < endPos) {
            char c = Strings.charAt(string, pos);
            int cval = JSRuntime.valueInRadix(c, radix);
            if (cval < 0) {
                if (pos != startPos) {
                    break;
                } else {
                    return Double.NaN;
                }
            }
            value *= radix;
            value += cval;
            pos++;
        }
        if (value == 0 && negate && Strings.charAt(string, startPos) == '0') {
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

    @TruffleBoundary
    public static double parseRawDontFitLong(TruffleString string, int radix, int startPos, int endPos, boolean negate) {
        assert startPos < endPos;
        int pos = startPos;

        double value = 0;
        while (pos < endPos) {
            char c = Strings.charAt(string, pos);
            int cval = JSRuntime.valueInRadix(c, radix);
            if (cval < 0) {
                if (pos != startPos) {
                    break;
                } else {
                    return Double.NaN;
                }
            }
            value *= radix;
            value += cval;
            pos++;
        }

        assert value >= 0;
        return negate ? -value : value;
    }

    /**
     * ES2015, 7.3.4 CreateDataProperty(O, P, V).
     */
    public static boolean createDataProperty(JSDynamicObject o, Object p, Object v) {
        assert JSRuntime.isObject(o);
        assert JSRuntime.isPropertyKey(p);
        return JSObject.defineOwnProperty(o, p, PropertyDescriptor.createDataDefault(v));
    }

    public static boolean createDataProperty(JSDynamicObject o, Object p, Object v, boolean doThrow) {
        assert JSRuntime.isObject(o);
        assert JSRuntime.isPropertyKey(p);
        boolean success = JSObject.defineOwnProperty(o, p, PropertyDescriptor.createDataDefault(v), doThrow);
        assert !doThrow || success : "should have thrown";
        return success;
    }

    /**
     * ES2015, 7.3.6 CreateDataPropertyOrThrow(O, P, V).
     */
    public static boolean createDataPropertyOrThrow(JSDynamicObject o, Object p, Object v) {
        return createDataProperty(o, p, v, true);
    }

    /**
     * Error Cause 7.3.6 CreateNonEnumerableDataPropertyOrThrow(O, P, V).
     */
    public static void createNonEnumerableDataPropertyOrThrow(JSDynamicObject o, Object p, Object v) {
        PropertyDescriptor newDesc = PropertyDescriptor.createData(v, JSAttributes.getDefaultNotEnumerable());
        definePropertyOrThrow(o, p, newDesc);
    }

    /**
     * ES2016, 7.3.7 DefinePropertyOrThrow(O, P, desc).
     */
    public static void definePropertyOrThrow(JSDynamicObject o, Object key, PropertyDescriptor desc) {
        assert JSRuntime.isObject(o);
        assert JSRuntime.isPropertyKey(key);
        boolean success = JSObject.getJSClass(o).defineOwnProperty(o, key, desc, true);
        assert success; // we should have thrown instead of returning false
    }

    public static boolean isPrototypeOf(JSDynamicObject object, JSDynamicObject prototype) {
        JSDynamicObject prototypeChainObject = object;
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
    public static JSDynamicObject createArrayFromList(JSContext context, JSRealm realm, List<? extends Object> list) {
        return JSArray.createConstant(context, realm, Boundaries.listToArray(list));
    }

    /**
     * ES2015 7.2.3 IsCallable(argument).
     */
    public static boolean isCallable(Object value) {
        if (JSFunction.isJSFunction(value)) {
            return true;
        } else if (JSProxy.isJSProxy(value)) {
            return isCallableProxy((JSDynamicObject) value);
        } else if (value instanceof TruffleObject) {
            return isCallableForeign(value);
        }
        return false;
    }

    public static boolean isCallableIsJSObject(JSDynamicObject value) {
        assert JSDynamicObject.isJSDynamicObject(value);
        if (JSFunction.isJSFunction(value)) {
            return true;
        } else if (JSProxy.isJSProxy(value)) {
            return isCallableProxy(value);
        }
        return false;
    }

    @TruffleBoundary
    public static boolean isCallableForeign(Object value) {
        if (isForeignObject(value)) {
            InteropLibrary interop = InteropLibrary.getUncached();
            return interop.isExecutable(value) || interop.isInstantiable(value);
        }
        return false;
    }

    @TruffleBoundary
    public static boolean isCallableProxy(JSDynamicObject proxy) {
        assert JSProxy.isJSProxy(proxy);
        Object target = JSProxy.getTarget(proxy);
        return isCallable(target);
    }

    /**
     * ES2015 7.2.2 IsArray(argument).
     */
    public static boolean isArray(Object obj) {
        if (JSArray.isJSArray(obj)) {
            return true;
        } else if (JSProxy.isJSProxy(obj)) {
            return isProxyAnArray((JSDynamicObject) obj);
        } else if (isForeignObject(obj)) {
            return InteropLibrary.getUncached().hasArrayElements(obj);
        }
        return false;
    }

    @TruffleBoundary
    public static boolean isProxyAnArray(JSDynamicObject proxy) {
        assert JSProxy.isJSProxy(proxy);
        if (JSProxy.isRevoked(proxy)) {
            throw Errors.createTypeErrorProxyRevoked();
        }
        return isArrayProxyRecurse(proxy);
    }

    @TruffleBoundary
    private static boolean isArrayProxyRecurse(JSDynamicObject proxy) {
        return isArray(JSProxy.getTarget(proxy));
    }

    /**
     * ES2015 7.1.14 ToPropertyKey(argument).
     */
    @TruffleBoundary
    public static Object toPropertyKey(Object arg) {
        if (Strings.isTString(arg)) {
            return arg;
        } else if (arg instanceof Symbol) {
            return arg;
        }
        Object key = toPrimitive(arg);
        if (key instanceof Symbol) {
            return key;
        } else if (Strings.isTString(key)) {
            return key;
        }
        return toString(key);
    }

    /**
     * ES2015 7.3.12 Call(F, V, arguments).
     */
    public static Object call(Object fnObj, Object holder, Object[] arguments) {
        if (JSFunction.isJSFunction(fnObj)) {
            return JSFunction.call((JSFunctionObject) fnObj, holder, arguments);
        } else if (JSProxy.isJSProxy(fnObj)) {
            return JSProxy.call((JSDynamicObject) fnObj, holder, arguments);
        } else if (isForeignObject(fnObj)) {
            return JSInteropUtil.call(fnObj, arguments);
        } else {
            throw Errors.createTypeErrorNotAFunction(fnObj);
        }
    }

    public static Object call(Object fnObj, Object holder, Object[] arguments, Node encapsulatingNode) {
        EncapsulatingNodeReference encapsulating = null;
        Node prev = null;
        if (encapsulatingNode != null) {
            encapsulating = EncapsulatingNodeReference.getCurrent();
            prev = encapsulating.set(encapsulatingNode);
        }
        try {
            return call(fnObj, holder, arguments);
        } finally {
            if (encapsulatingNode != null) {
                encapsulating.set(prev);
            }
        }
    }

    public static Object construct(Object fnObj, Object[] arguments) {
        if (JSFunction.isConstructor(fnObj)) {
            return JSFunction.construct((JSFunctionObject) fnObj, arguments);
        } else if (JSProxy.isJSProxy(fnObj) && isConstructorProxy((JSDynamicObject) fnObj)) {
            return JSProxy.construct((JSDynamicObject) fnObj, arguments);
        } else if (isForeignObject(fnObj)) {
            return JSInteropUtil.construct(fnObj, arguments);
        } else {
            throw Errors.createTypeErrorNotAConstructor(fnObj, JavaScriptLanguage.get(null).getJSContext());
        }
    }

    /**
     * ES2015, 7.1.16 CanonicalNumericIndexString().
     */
    @TruffleBoundary
    public static Object canonicalNumericIndexString(TruffleString s) {
        if (Strings.isEmpty(s) || !isNumericIndexStart(Strings.charAt(s, 0))) {
            return Undefined.instance;
        }
        if (Strings.NEGATIVE_ZERO.equals(s)) {
            return -0.0;
        }
        Number n = stringToNumber(s);
        if (!numberToString(n).equals(s)) {
            return Undefined.instance;
        }
        return n;
    }

    private static boolean isNumericIndexStart(char c) {
        // Start of a number, "Infinity", or "NaN".
        return isAsciiDigit(c) || c == '-' || c == 'I' || c == 'N';
    }

    /**
     * ES2015, 7.2.6 IsInteger.
     */
    public static boolean isInteger(Object obj) {
        if (!JSRuntime.isNumber(obj)) {
            return false;
        }
        double d = doubleValue((Number) obj);
        return d - JSRuntime.truncateDouble(d) == 0.0;
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
        boolean isString1 = Strings.isTString(key1);
        boolean isString2 = Strings.isTString(key2);
        if (isString1 && isString2) {
            long index1 = JSRuntime.propertyNameToArrayIndex((TruffleString) key1, TruffleString.ReadCharUTF16Node.getUncached());
            long index2 = JSRuntime.propertyNameToArrayIndex((TruffleString) key2, TruffleString.ReadCharUTF16Node.getUncached());
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
    public static TruffleString getConstructorName(JSDynamicObject receiver) {
        // Try @@toStringTag first
        Object toStringTag = getDataProperty(receiver, Symbol.SYMBOL_TO_STRING_TAG);
        if (toStringTag instanceof TruffleString str) {
            return str;
        }

        // Try function name of prototype.constructor
        if (!isProxy(receiver)) {
            JSDynamicObject prototype = JSObject.getPrototype(receiver);
            if (prototype != Null.instance) {
                Object constructor = getDataProperty(prototype, JSObject.CONSTRUCTOR);
                if (JSFunction.isJSFunction(constructor)) {
                    return JSFunction.getName((JSFunctionObject) constructor);
                }
            }
        }

        // As a last resort, use class name
        return JSObject.getClassName(receiver);
    }

    public static TruffleString getPrimitiveConstructorName(Object primitive) {
        assert isJSPrimitive(primitive) && !isNullOrUndefined(primitive);
        if (primitive instanceof Boolean) {
            return JSBoolean.CLASS_NAME;
        } else if (isNumber(primitive) || primitive instanceof Long) {
            return JSNumber.CLASS_NAME;
        } else if (isBigInt(primitive)) {
            return JSBigInt.CLASS_NAME;
        } else if (Strings.isTString(primitive)) {
            return JSString.CLASS_NAME;
        } else if (primitive instanceof Symbol) {
            return JSSymbol.CLASS_NAME;
        }
        throw Errors.shouldNotReachHereUnexpectedValue(primitive);
    }

    public static Object getDataProperty(JSDynamicObject thisObj, Object key) {
        assert JSRuntime.isPropertyKey(key);
        JSDynamicObject current = thisObj;
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

    private static boolean isProxy(JSDynamicObject receiver) {
        return JSProxy.isJSProxy(receiver) || JSAdapter.isJSAdapter(receiver);
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
        if (obj instanceof JSObject jsObj) {
            if (jsObj instanceof JSFunctionObject function) {
                if (jsObj instanceof JSFunctionObject.Bound boundFunction) {
                    return getFunctionRealm(boundFunction.getBoundTargetFunction(), currentRealm);
                } else {
                    return JSFunction.getRealm(function);
                }
            } else if (jsObj instanceof JSProxyObject) {
                if (JSProxy.getHandler(jsObj) == Null.instance) {
                    throw Errors.createTypeErrorProxyRevoked();
                }
                return getFunctionRealm(JSProxy.getTarget(jsObj), currentRealm);
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
        } else if (JSProxy.isJSProxy(constrObj)) {
            return isConstructorProxy((JSDynamicObject) constrObj);
        } else if (constrObj instanceof TruffleObject) {
            return isConstructorForeign(constrObj);
        }
        return false;
    }

    @TruffleBoundary
    public static boolean isConstructorForeign(Object value) {
        if (isForeignObject(value)) {
            return InteropLibrary.getUncached().isInstantiable(value);
        }
        return false;
    }

    @TruffleBoundary
    public static boolean isConstructorProxy(JSDynamicObject constrObj) {
        assert JSProxy.isJSProxy(constrObj);
        return isConstructor(JSProxy.getTarget(constrObj));
    }

    public static boolean isGenerator(Object genObj) {
        if (JSFunction.isJSFunction(genObj) && JSFunction.isGenerator((JSFunctionObject) genObj)) {
            return true;
        } else if (JSProxy.isJSProxy(genObj)) {
            return isGeneratorProxy((JSDynamicObject) genObj);
        }
        return false;
    }

    @TruffleBoundary
    public static boolean isGeneratorProxy(JSDynamicObject genObj) {
        assert JSProxy.isJSProxy(genObj);
        return isGenerator(JSProxy.getTarget(genObj));
    }

    // ES2015: 7.3.17 CreateListFromArrayLike
    @TruffleBoundary
    public static List<Object> createListFromArrayLikeAllowSymbolString(Object obj) {
        if (!isObject(obj)) {
            throw Errors.createTypeErrorNotAnObject(obj);
        }
        JSDynamicObject jsObj = (JSDynamicObject) obj;
        long len = JSRuntime.toLength(JSObject.get(jsObj, JSAbstractArray.LENGTH));
        if (len > Integer.MAX_VALUE) {
            throw Errors.createRangeError("range exceeded");
        }
        List<Object> list = new ArrayList<>();
        long index = 0;
        while (index < len) {
            Object next = JSObject.get(jsObj, index);
            if (!(Strings.isTString(next) || next instanceof Symbol)) {
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

    /**
     * Implementation of the Quote(value) operation as defined in the ECMAscript spec. It wraps a
     * String value in double quotes and escapes characters within.
     *
     * @param value string to quote
     *
     * @return quoted and escaped string
     */
    @TruffleBoundary
    public static TruffleString quote(TruffleString value) {
        int len = Strings.length(value);
        int pos = 0;
        while (pos < len) {
            char ch = Strings.charAt(value, pos);
            if (ch < ' ' || ch == '\\' || ch == '"') {
                break;
            }
            pos++;
        }

        var builder = Strings.builderCreate(len + 2);
        Strings.builderAppend(builder, '"');
        Strings.builderAppend(builder, value, 0, pos);
        for (int i = pos; i < len; i++) {
            char ch = Strings.charAt(value, i);
            if (ch < ' ') {
                if (ch == '\b') {
                    Strings.builderAppend(builder, Strings.BACKSLASH_B);
                } else if (ch == '\f') {
                    Strings.builderAppend(builder, Strings.BACKSLASH_F);
                } else if (ch == '\n') {
                    Strings.builderAppend(builder, Strings.BACKSLASH_N);
                } else if (ch == '\r') {
                    Strings.builderAppend(builder, Strings.BACKSLASH_R);
                } else if (ch == '\t') {
                    Strings.builderAppend(builder, Strings.BACKSLASH_T);
                } else {
                    Strings.builderAppend(builder, Strings.BACKSLASH_U00);
                    Strings.builderAppend(builder, Character.forDigit((ch & 0xF0) >> 4, 16));
                    Strings.builderAppend(builder, Character.forDigit((ch & 0x0F), 16));
                }
            } else if (ch == '\\') {
                Strings.builderAppend(builder, Strings.BACKSLASH_BACKSLASH);
            } else if (ch == '"') {
                Strings.builderAppend(builder, Strings.BACKSLASH_DOUBLE_QUOTE);
            } else {
                Strings.builderAppend(builder, ch);
            }
        }
        Strings.builderAppend(builder, '"');
        return Strings.builderToString(builder);
    }

    public static JSDynamicObject expectJSObject(Object to, BranchProfile errorBranch) {
        if (!JSDynamicObject.isJSDynamicObject(to)) {
            errorBranch.enter();
            throw Errors.createTypeErrorJSObjectExpected();
        }
        return (JSDynamicObject) to;
    }

    /**
     * Convert the value to a type valid in Truffle Interop. Use ExportValueNode where possible.
     */
    @TruffleBoundary
    public static Object exportValue(Object value) {
        return ExportValueNode.getUncached().execute(value);
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
     * Use ImportValueNode where possible.
     */
    @TruffleBoundary
    public static Object importValue(Object value) {
        assert value != null;
        return ImportValueNode.getUncached().executeWithTarget(value);
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

    @SuppressWarnings("unchecked")
    public static <E extends Throwable> RuntimeException rethrow(Throwable ex) throws E {
        throw (E) ex;
    }

    public static GraalJSException getException(Object errorObject) {
        return getException(errorObject, null);
    }

    public static GraalJSException getException(Object errorObject, Node node) {
        if (errorObject instanceof JSErrorObject jsErrorObject) {
            return JSError.getException(jsErrorObject);
        } else {
            return UserScriptException.create(errorObject, node, JavaScriptLanguage.get(node).getJSContext().getLanguageOptions().stackTraceLimit());
        }
    }

    public static IteratorRecord getIterator(Object iteratedObject) {
        JSDynamicObject target;
        if (iteratedObject instanceof JSDynamicObject) {
            target = (JSDynamicObject) iteratedObject;
        } else {
            target = ForeignObjectPrototypeNode.getUncached().execute(iteratedObject);
        }
        Object method = JSObject.getOrDefault(target, Symbol.SYMBOL_ITERATOR, iteratedObject, Undefined.instance);
        if (!isCallable(method)) {
            throw Errors.createTypeErrorNotIterable(iteratedObject, null);
        }
        Object iterator = call(method, iteratedObject, new Object[]{});
        if (IsObjectNode.getUncached().executeBoolean(iterator)) {
            return IteratorRecord.create(iterator, get(iterator, Strings.NEXT), false);
        } else {
            throw Errors.createTypeErrorNotAnObject(iterator);
        }
    }

    public static Object iteratorStep(IteratorRecord iteratorRecord) {
        Object nextMethod = iteratorRecord.getNextMethod();
        Object iterator = iteratorRecord.getIterator();
        Object result = call(nextMethod, iterator, new Object[]{});
        if (!IsObjectNode.getUncached().executeBoolean(result)) {
            throw Errors.createTypeErrorIteratorResultNotObject(result, null);
        }
        boolean done = toBoolean(get(result, Strings.DONE));
        if (done) {
            return false;
        }
        return result;
    }

    public static Object iteratorValue(Object iterator) {
        return get(iterator, Strings.VALUE);
    }

    public static void iteratorClose(Object iterator) {
        Object returnMethod = get(iterator, Strings.RETURN);
        if (returnMethod != Undefined.instance) {
            Object innerResult = call(returnMethod, iterator, new Object[]{});
            if (!IsObjectNode.getUncached().executeBoolean(innerResult)) {
                throw Errors.createTypeErrorIterResultNotAnObject(innerResult, null);
            }
        }
    }

    public static boolean isIntegralNumber(double arg) {
        // IsIntegralNumber is defined as:
        // If arg is NaN or +/-Infinity, return false.
        // Return floor(abs(arg)) == abs(arg).

        // floor(abs(arg)) == abs(arg) is equivalent to trunc(arg) == arg;
        // because (Infinity - Infinity) is NaN, IsIntegralNumber can be simplified to:
        // arg - trunc(arg) == 0.
        return arg - JSRuntime.truncateDouble(arg) == 0.0;
    }

    @TruffleBoundary
    public static Object get(Object obj, Object key) {
        assert JSRuntime.isPropertyKey(key);
        if (JSDynamicObject.isJSDynamicObject(obj)) {
            return JSObject.get((JSDynamicObject) obj, key);
        } else {
            return JSInteropUtil.readMemberOrDefault(obj, key, Undefined.instance);
        }
    }

    @TruffleBoundary
    public static Object get(Object obj, long index) {
        if (JSDynamicObject.isJSDynamicObject(obj)) {
            return JSObject.get((JSDynamicObject) obj, index);
        } else {
            return JSInteropUtil.readArrayElementOrDefault(obj, index, Undefined.instance);
        }
    }

    @TruffleBoundary
    public static boolean hasProperty(Object obj, Object key) {
        if (JSDynamicObject.isJSDynamicObject(obj)) {
            return JSObject.hasProperty((JSDynamicObject) obj, key);
        } else {
            return JSInteropUtil.hasProperty(obj, key);
        }
    }

    public static boolean isPrivateSymbol(Object key) {
        return (key instanceof Symbol) && ((Symbol) key).isPrivate();
    }

    @TruffleBoundary
    public static List<Object> filterPrivateSymbols(List<Object> list) {
        boolean containsPrivateSymbol = false;
        for (Object key : list) {
            if (isPrivateSymbol(key)) {
                containsPrivateSymbol = true;
                break;
            }
        }
        if (containsPrivateSymbol) {
            List<Object> filtered = new ArrayList<>(list.size());
            for (Object key : list) {
                if (!isPrivateSymbol(key)) {
                    filtered.add(key);
                }
            }
            return filtered;
        } else {
            return list;
        }
    }

    public static Number toUint32(int value) {
        if (value >= 0) {
            return value;
        } else {
            return (double) (value & 0xFFFFFFFFL);
        }
    }

    public static short toFloat16(Number number) {
        return toFloat16(doubleValue(number));
    }

    public static short toFloat16(double d) {
        float f = (float) d;
        short s = Float.floatToFloat16(f);
        if (f != d) {
            // The conversion to float is not exact. Check if we get the same
            // float16 when using the float on the other side of the original value.
            if (f < d) {
                short sNextUp = Float.floatToFloat16(Math.nextUp(f));
                if (s != sNextUp) {
                    // We need the closer one from low and high
                    float low = Float.float16ToFloat(s);
                    float high = Float.float16ToFloat(sNextUp);
                    return (high - d < d - low) ? sNextUp : s;
                }
            } else {
                short sNextDown = Float.floatToFloat16(Math.nextDown(f));
                if (s != sNextDown) {
                    // We need the closer one from low and high
                    float low = Float.float16ToFloat(sNextDown);
                    float high = Float.float16ToFloat(s);
                    return (high - d < d - low) ? s : sNextDown;
                }
            }
        }
        return s;
    }

    public static Object getBufferElementDirect(ByteBufferAccess bufferAccess, ByteBuffer buffer, TypedArray.ElementType elementType, int index) {
        switch (elementType) {
            case Int8:
                return (int) buffer.get(index);
            case Uint8:
            case Uint8Clamped:
                return buffer.get(index) & 0xff;
            case Int16:
                return bufferAccess.getInt16(buffer, index);
            case Uint16:
                return bufferAccess.getUint16(buffer, index);
            case Int32:
                return bufferAccess.getInt32(buffer, index);
            case Uint32:
                return toUint32(bufferAccess.getInt32(buffer, index));
            case BigInt64:
            case BigUint64:
                return BigInt.valueOf(bufferAccess.getInt64(buffer, index));
            case Float16:
                return (double) Float.float16ToFloat(bufferAccess.getFloat16(buffer, index));
            case Float32:
                return (double) bufferAccess.getFloat(buffer, index);
            case Float64:
                return bufferAccess.getDouble(buffer, index);
            default:
                throw CompilerDirectives.shouldNotReachHere();
        }
    }

    public static void setBufferElementDirect(ByteBufferAccess bufferAccess, ByteBuffer buffer, TypedArray.ElementType elementType, int index, Object value) {
        switch (elementType) {
            case Int8:
            case Uint8:
                buffer.put(index, (byte) toInt32((Number) value));
                break;
            case Uint8Clamped:
                int intValue;
                if (value instanceof Integer) {
                    intValue = (Integer) value;
                } else {
                    intValue = (int) Math.rint(toDouble((Number) value));
                }
                int clampedValue = intValue < 0 ? 0 : (intValue > 0xff ? 0xff : intValue);
                buffer.put(index, (byte) clampedValue);
                break;
            case Int16:
                bufferAccess.putInt16(buffer, index, (short) toInt32((Number) value));
                break;
            case Uint16:
                bufferAccess.putInt16(buffer, index, (char) toInt32((Number) value));
                break;
            case Int32:
            case Uint32:
                bufferAccess.putInt32(buffer, index, toInt32((Number) value));
                break;
            case BigInt64:
            case BigUint64:
                bufferAccess.putInt64(buffer, index, toBigInt(value).longValue());
                break;
            case Float16:
                bufferAccess.putFloat16(buffer, index, toFloat16((Number) value));
                break;
            case Float32:
                bufferAccess.putFloat(buffer, index, floatValue((Number) value));
                break;
            case Float64:
                bufferAccess.putDouble(buffer, index, doubleValue((Number) value));
                break;
            default:
                throw CompilerDirectives.shouldNotReachHere();
        }
    }
}
