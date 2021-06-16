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
package com.oracle.truffle.js.runtime;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.ExactMath;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.interop.ArityException;
import com.oracle.truffle.api.interop.InteropException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.api.nodes.EncapsulatingNodeReference;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.api.object.Property;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.nodes.access.IsPrimitiveNode;
import com.oracle.truffle.js.nodes.interop.ExportValueNode;
import com.oracle.truffle.js.nodes.interop.ImportValueNode;
import com.oracle.truffle.js.runtime.array.TypedArrayFactory;
import com.oracle.truffle.js.runtime.builtins.JSAbstractArray;
import com.oracle.truffle.js.runtime.builtins.JSAdapter;
import com.oracle.truffle.js.runtime.builtins.JSArray;
import com.oracle.truffle.js.runtime.builtins.JSArrayBufferView;
import com.oracle.truffle.js.runtime.builtins.JSBigInt;
import com.oracle.truffle.js.runtime.builtins.JSBoolean;
import com.oracle.truffle.js.runtime.builtins.JSClass;
import com.oracle.truffle.js.runtime.builtins.JSDate;
import com.oracle.truffle.js.runtime.builtins.JSError;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSMap;
import com.oracle.truffle.js.runtime.builtins.JSNumber;
import com.oracle.truffle.js.runtime.builtins.JSOrdinary;
import com.oracle.truffle.js.runtime.builtins.JSOverloadedOperatorsObject;
import com.oracle.truffle.js.runtime.builtins.JSProxy;
import com.oracle.truffle.js.runtime.builtins.JSSet;
import com.oracle.truffle.js.runtime.builtins.JSString;
import com.oracle.truffle.js.runtime.builtins.JSSymbol;
import com.oracle.truffle.js.runtime.doubleconv.DoubleConversion;
import com.oracle.truffle.js.runtime.external.DToA;
import com.oracle.truffle.js.runtime.interop.InteropFunction;
import com.oracle.truffle.js.runtime.interop.JSInteropUtil;
import com.oracle.truffle.js.runtime.objects.JSAttributes;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSLazyString;
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
    public static final int MAX_INTEGER_INDEX_DIGITS = 16;
    public static final int MAX_SAFE_INTEGER_DIGITS = 16;
    public static final int MAX_SAFE_INTEGER_IN_FLOAT = 1 << 24;
    public static final int MIN_SAFE_INTEGER_IN_FLOAT = -MAX_SAFE_INTEGER_IN_FLOAT;
    public static final long MAX_BIG_INT_EXPONENT = Integer.MAX_VALUE;
    public static final long INVALID_SAFE_INTEGER = Long.MIN_VALUE;

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
    public static final HiddenKey FOR_IN_ITERATOR_ID = new HiddenKey("ForInIterator");
    public static final HiddenKey FINALIZATION_GROUP_CLEANUP_ITERATOR_ID = new HiddenKey("CleanupIterator");

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
        } else if (JSDynamicObject.isJSDynamicObject(value)) {
            DynamicObject object = (DynamicObject) value;
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
            CompilerDirectives.transferToInterpreter();
            throw new UnsupportedOperationException("typeof: don't know " + value.getClass().getSimpleName());
        }
    }

    /**
     * Returns whether object is a DynamicObject. JS-Null and JS-Undefined are not considered
     * objects.
     */
    public static boolean isObject(Object vo) {
        assert vo instanceof JSObject == hasJSDynamicType(vo);
        return vo instanceof JSObject;
    }

    private static boolean hasJSDynamicType(Object vo) {
        if (JSDynamicObject.isJSDynamicObject(vo)) {
            Object type = ((JSDynamicObject) vo).getShape().getDynamicType();
            return (type instanceof JSClass) && (type != Null.NULL_CLASS);
        } else {
            return false;
        }
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
     * @return an Object representing the primitive value of the parameter
     * @see com.oracle.truffle.js.nodes.cast.JSToPrimitiveNode
     */
    @TruffleBoundary
    public static Object toPrimitive(Object value, String hint) {
        if (value == Null.instance || value == Undefined.instance) {
            return value;
        } else if (JSDynamicObject.isJSDynamicObject(value)) {
            return JSObject.toPrimitive((DynamicObject) value, hint);
        } else if (isForeignObject(value)) {
            return toPrimitiveFromForeign(value, hint);
        }
        return value;

    }

    /**
     * Converts a foreign object to a primitive value.
     */
    @TruffleBoundary
    public static Object toPrimitiveFromForeign(Object tObj, String hint) {
        assert isForeignObject(tObj);
        InteropLibrary interop = InteropLibrary.getFactory().getUncached(tObj);
        if (interop.isNull(tObj)) {
            return Null.instance;
        } else if (JSInteropUtil.isBoxedPrimitive(tObj, interop)) {
            return JSInteropUtil.toPrimitiveOrDefault(tObj, Null.instance, interop, null);
        } else if (JavaScriptLanguage.getCurrentEnv().isHostObject(tObj)) {
            if (JSRuntime.HINT_NUMBER.equals(hint) && JavaScriptLanguage.getCurrentLanguage().getJSContext().isOptionNashornCompatibilityMode() &&
                            interop.isMemberInvocable(tObj, "doubleValue")) {
                try {
                    return interop.invokeMember(tObj, "doubleValue");
                } catch (UnsupportedMessageException | ArityException | UnknownIdentifierException | UnsupportedTypeException e) {
                    throw Errors.createTypeErrorInteropException(tObj, e, "doubleValue()", null);
                }
            } else if (interop.isInstant(tObj)) {
                return JSDate.getDateValueFromInstant(tObj, interop);
            } else if (isJavaArray(tObj, interop)) {
                return formatJavaArray(tObj, interop);
            } else if (interop.isMetaObject(tObj)) {
                return javaClassToString(tObj, interop);
            } else if (interop.isException(tObj)) {
                return javaExceptionToString(tObj, interop);
            }
        }
        return foreignOrdinaryToPrimitive(tObj, hint);
    }

    private static boolean isJavaArray(Object obj, InteropLibrary interop) {
        return interop.hasArrayElements(obj) && interop.isMemberReadable(obj, "length");
    }

    @TruffleBoundary
    private static Object formatJavaArray(Object obj, InteropLibrary interop) {
        assert isJavaArray(obj, interop);
        return JSRuntime.toDisplayString(obj, true, ToDisplayStringFormat.getArrayFormat());
    }

    @TruffleBoundary
    private static Object javaClassToString(Object object, InteropLibrary interop) {
        try {
            String qualifiedName = InteropLibrary.getUncached().asString(interop.getMetaQualifiedName(object));
            if (JavaScriptLanguage.getCurrentLanguage().getJSContext().isOptionNashornCompatibilityMode() && qualifiedName.endsWith("[]")) {
                Object hostObject = JavaScriptLanguage.getCurrentEnv().asHostObject(object);
                qualifiedName = ((Class<?>) hostObject).getName();
            }
            return "class " + qualifiedName;
        } catch (UnsupportedMessageException e) {
            throw Errors.createTypeErrorInteropException(object, e, "getTypeName", null);
        }
    }

    @TruffleBoundary
    private static String javaExceptionToString(Object object, InteropLibrary interop) {
        try {
            return InteropLibrary.getUncached().asString(interop.toDisplayString(object, true));
        } catch (UnsupportedMessageException e) {
            throw Errors.createTypeErrorInteropException(object, e, "toString", null);
        }
    }

    @TruffleBoundary
    private static Object foreignOrdinaryToPrimitive(Object obj, String hint) {
        JSRealm realm = JavaScriptLanguage.getCurrentJSRealm();
        InteropLibrary interop = InteropLibrary.getFactory().getUncached(obj);
        String[] methodNames;
        if (hint.equals(HINT_STRING)) {
            methodNames = new String[]{TO_STRING, VALUE_OF};
        } else {
            assert JSRuntime.HINT_NUMBER.equals(hint);
            methodNames = new String[]{VALUE_OF, TO_STRING};
        }
        DynamicObject proto;
        if (interop.hasArrayElements(obj)) {
            proto = realm.getArrayPrototype();
        } else if (interop.isExecutable(obj)) {
            proto = realm.getFunctionPrototype();
        } else if (interop.isInstant(obj)) {
            proto = realm.getDatePrototype();
        } else {
            proto = realm.getObjectPrototype();
        }

        for (String name : methodNames) {
            if (interop.hasMembers(obj) && interop.isMemberInvocable(obj, name)) {
                Object result;
                try {
                    result = importValue(interop.invokeMember(obj, name));
                } catch (InteropException e) {
                    result = null;
                }
                if (result != null && IsPrimitiveNode.getUncached().executeBoolean(result)) {
                    return result;
                }
            }

            Object method = JSObject.getMethod(proto, name);
            if (isCallable(method)) {
                Object result = call(method, obj, new Object[]{});
                if (IsPrimitiveNode.getUncached().executeBoolean(result)) {
                    return result;
                }
            }
        }
        throw Errors.createTypeErrorCannotConvertToPrimitiveValue();
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
        } else if (isForeignObject(value)) {
            InteropLibrary interop = InteropLibrary.getFactory().getUncached(value);
            if (interop.isNull(value)) {
                return false;
            } else if (JSInteropUtil.isBoxedPrimitive(value, interop)) {
                return toBoolean(JSInteropUtil.toPrimitiveOrDefault(value, Null.instance, interop, null));
            } else {
                return true;
            }
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
        Object primitive;
        if (isObject(value)) {
            primitive = JSObject.toPrimitive((DynamicObject) value, HINT_NUMBER);
        } else if (isForeignObject(value)) {
            primitive = toPrimitiveFromForeign(value, HINT_NUMBER);
        } else {
            primitive = value;
        }
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
        } else if (value instanceof BigInt) {
            throw Errors.createTypeErrorCannotConvertToNumber("a BigInt value");
        } else if (value instanceof Number) {
            assert isJavaPrimitive(value) : value.getClass().getName();
            return (Number) value;
        }
        assert false : "should never reach here, type " + value.getClass().getName() + " not handled.";
        throw Errors.createTypeErrorCannotConvertToNumber(safeToString(value));
    }

    public static int booleanToNumber(boolean value) {
        return value ? 1 : 0;
    }

    public static boolean isNumber(Object value) {
        return value instanceof Integer || value instanceof Double || value instanceof Long || value instanceof SafeInteger;
    }

    @TruffleBoundary
    public static BigInt toBigInt(Object value) {
        Object primitive = toPrimitive(value, HINT_NUMBER);
        if (primitive instanceof String) {
            try {
                return BigInt.valueOf((String) primitive);
            } catch (NumberFormatException e) {
                throw Errors.createErrorCanNotConvertToBigInt(JSErrorType.SyntaxError, primitive);
            }
        } else if (primitive instanceof BigInt) {
            return (BigInt) primitive;
        } else if (primitive instanceof Boolean) {
            return (Boolean) primitive ? BigInt.ONE : BigInt.ZERO;
        } else {
            throw Errors.createErrorCanNotConvertToBigInt(JSErrorType.TypeError, primitive);
        }
    }

    @TruffleBoundary
    public static BigInt toBigIntSpec(Object value) {
        Object primitive = toPrimitive(value, HINT_NUMBER);
        if (primitive instanceof Number) {
            return new BigInt(BigInteger.valueOf(((Number) primitive).longValue()));
        } else if (primitive instanceof String) {
            try {
                return BigInt.valueOf((String) primitive);
            } catch (NumberFormatException e) {
                throw Errors.createErrorCanNotConvertToBigInt(JSErrorType.SyntaxError, primitive);
            }
        } else if (primitive instanceof BigInt) {
            return (BigInt) primitive;
        } else if (primitive instanceof Boolean) {
            return (Boolean) primitive ? BigInt.ONE : BigInt.ZERO;
        } else {
            throw Errors.createErrorCanNotConvertToBigInt(JSErrorType.TypeError, primitive);
        }
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
    public static Number stringToNumber(String string) {
        // "Infinity" written exactly like this
        String strCamel = trimJSWhiteSpace(string);
        if (strCamel.length() == 0) {
            return 0;
        }
        char firstChar = strCamel.charAt(0);
        if (strCamel.length() >= INFINITY_STRING.length() && strCamel.length() <= INFINITY_STRING.length() + 1 && strCamel.endsWith(INFINITY_STRING)) {
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
            if (!sci && str.length() <= 18 && str.indexOf('.') == -1) {
                // 18 digits always fit into long
                if (hex) {
                    return Long.valueOf(str.substring(2), 16);
                } else {
                    return stringToNumberLong(str);
                }
            } else {
                return parseDoubleOrNaN(str);
            }
        } catch (NumberFormatException e) {
            return Double.NaN;
        }
    }

    private static Number stringToNumberLong(String strLower) throws NumberFormatException {
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

    /**
     * Like {@link Double#parseDouble(String)}, but does not allow trailing {@code d} or {@code f}.
     *
     * @return double value or {@link Double#NaN} if not parsable.
     */
    @TruffleBoundary
    public static double parseDoubleOrNaN(String input) {
        // A valid JS number must end with either a digit or '.'.
        // Double.parseDouble also accepts a trailing 'd', 'D', 'f', 'F'.
        if (input.isEmpty() || input.charAt(input.length() - 1) > '9') {
            return Double.NaN;
        }
        try {
            return Double.parseDouble(input);
        } catch (NumberFormatException e) {
            return Double.NaN;
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

    @SuppressWarnings("cast")
    @TruffleBoundary
    public static int toInt8(long number) {
        int res = floorMod(number, 256);
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

    @SuppressWarnings("cast")
    @TruffleBoundary()
    public static int toInt16(long number) {
        int res = floorMod(number, 65536);
        if (res >= 32768) {
            res = res - 65536;
        }
        return res;
    }

    public static int floorMod(long x, int y) {
        // Result cannot overflow the range of int.
        long divisor = y;
        return (int) Math.floorMod(x, divisor);
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

    /**
     * The abstract operation ToString. Converts a value to a string.
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
        } else if (value instanceof BigInt) {
            return value.toString();
        } else if (JSDynamicObject.isJSDynamicObject(value)) {
            return toString(JSObject.toPrimitive((DynamicObject) value, HINT_STRING));
        } else if (value instanceof TruffleObject) {
            assert !isJSNative(value);
            return toString(toPrimitiveFromForeign(value, HINT_STRING));
        }
        throw toStringTypeError(value);
    }

    @TruffleBoundary
    public static String safeToString(Object value) {
        return toDisplayString(value, false, ToDisplayStringFormat.getDefaultFormat());
    }

    @TruffleBoundary
    public static String toDisplayString(Object value, boolean allowSideEffects) {
        return toDisplayString(value, allowSideEffects, ToDisplayStringFormat.getDefaultFormat());
    }

    @TruffleBoundary
    public static String toDisplayString(Object value, boolean allowSideEffects, ToDisplayStringFormat format) {
        return toDisplayStringImpl(value, allowSideEffects, format, 0, null);
    }

    @TruffleBoundary
    public static String toDisplayStringInner(Object value, boolean allowSideEffects, ToDisplayStringFormat format, int currentDepth, Object parent) {
        return toDisplayStringImpl(value, allowSideEffects, format.withQuoteString(true), currentDepth + 1, parent);
    }

    /**
     * Converts the value to a String that can be printed on the console and used in error messages.
     *
     * @param format formatting parameters
     * @param depth current recursion depth (starts at 0 = top level)
     * @param parent parent object or null
     */
    public static String toDisplayStringImpl(Object value, boolean allowSideEffects, ToDisplayStringFormat format, int depth, Object parent) {
        CompilerAsserts.neverPartOfCompilation();
        if (value == parent) {
            return "(this)";
        } else if (value == Undefined.instance) {
            return Undefined.NAME;
        } else if (value == Null.instance) {
            return Null.NAME;
        } else if (value instanceof Boolean) {
            return booleanToString((Boolean) value);
        } else if (isString(value)) {
            String string = value.toString();
            return format.quoteString() ? quote(string) : string;
        } else if (JSDynamicObject.isJSDynamicObject(value)) {
            return ((JSDynamicObject) value).toDisplayStringImpl(allowSideEffects, format, depth);
        } else if (value instanceof Symbol) {
            return value.toString();
        } else if (value instanceof BigInt) {
            return value.toString() + 'n';
        } else if (isNumber(value)) {
            Number number = (Number) value;
            if (JSRuntime.isNegativeZero(number.doubleValue())) {
                return "-0";
            } else {
                return numberToString(number);
            }
        } else if (value instanceof InteropFunction) {
            return toDisplayStringImpl(((InteropFunction) value).getFunction(), allowSideEffects, format, depth, parent);
        } else if (value instanceof TruffleObject) {
            assert !isJSNative(value) : value;
            return foreignToString(value, allowSideEffects, format, depth);
        } else {
            return String.valueOf(value);
        }
    }

    @TruffleBoundary
    public static String objectToDisplayString(DynamicObject obj, boolean allowSideEffects, ToDisplayStringFormat format, int depth, String name) {
        return objectToDisplayString(obj, allowSideEffects, format, depth, name, null, null);
    }

    @TruffleBoundary
    public static String objectToDisplayString(DynamicObject obj, boolean allowSideEffects, ToDisplayStringFormat format, int depth, String name, String[] internalKeys, Object[] internalValues) {
        assert JSDynamicObject.isJSDynamicObject(obj) && !JSFunction.isJSFunction(obj) && !JSProxy.isJSProxy(obj);
        boolean v8CompatMode = JSObject.getJSContext(obj).isOptionV8CompatibilityMode();
        StringBuilder sb = new StringBuilder();

        if (name != null) {
            sb.append(name);
        }
        boolean isArrayLike = false; // also TypedArrays
        boolean isArray = false;
        long length = -1;
        if (JSArray.isJSArray(obj)) {
            isArrayLike = true;
            isArray = true;
            length = JSArray.arrayGetLength(obj);
        } else if (JSArrayBufferView.isJSArrayBufferView(obj)) {
            isArrayLike = true;
            length = JSArrayBufferView.typedArrayGetLength(obj);
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
                        sb.append("Array");
                    }
                    sb.append('(').append(length).append(')');
                    return sb.toString();
                } else if (topLevel && length >= 2 && !v8CompatMode && format.includeArrayLength()) {
                    sb.append('(').append(length).append(')');
                }
            }
        } else if (depth >= format.getMaxDepth()) {
            sb.append("{...}");
            return sb.toString();
        }

        sb.append(isArrayLike ? '[' : '{');
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
            if ((isArrayLike || isStringObj) && key.equals("length") || (isStringObj && JSRuntime.isArrayIndex(key) && JSRuntime.parseArrayIndexRaw(key.toString()) < length)) {
                // length for arrays is printed as very first item
                // don't print individual characters (and length) for Strings
                continue;
            }
            if (propertyCount > 0) {
                sb.append(v8CompatMode ? "," : ", ");
                if (propertyCount >= format.getMaxElements()) {
                    sb.append("...");
                    break;
                }
            }
            if (isArray) {
                // merge holes to "empty (times) (count)" entries
                if (JSRuntime.isArrayIndex(key)) {
                    long index = JSRuntime.parseArrayIndexRaw(key.toString());
                    if ((index < length) && fillEmptyArrayElements(sb, index, prevArrayIndex, false)) {
                        sb.append(", ");
                        propertyCount++;
                        if (propertyCount >= format.getMaxElements()) {
                            sb.append("...");
                            break;
                        }
                    }
                    prevArrayIndex = index;
                } else {
                    if (fillEmptyArrayElements(sb, length, prevArrayIndex, false)) {
                        sb.append(", ");
                        propertyCount++;
                        if (propertyCount >= format.getMaxElements()) {
                            sb.append("...");
                            break;
                        }
                    }
                    prevArrayIndex = Math.max(prevArrayIndex, length);
                }
            }
            if (!isArrayLike || !JSRuntime.isArrayIndex(key)) {
                // print keys, but don't print array-indices
                sb.append(key);
                sb.append(": ");
            }
            String valueStr = null;
            if (desc.isDataDescriptor()) {
                Object value = desc.getValue();
                valueStr = toDisplayStringInner(value, allowSideEffects, format, depth, obj);
            } else if (desc.isAccessorDescriptor()) {
                valueStr = "accessor";
            } else {
                valueStr = "empty";
            }
            sb.append(valueStr);
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
                    sb.append(", ");
                }
                sb.append("[[").append(internalKeys[i]).append("]]: ").append(toDisplayStringInner(internalValues[i], allowSideEffects, format, depth, obj));
                propertyCount++;
            }
        }
        sb.append(isArrayLike ? ']' : '}');
        return sb.toString();
    }

    private static String foreignToString(Object value, boolean allowSideEffects, ToDisplayStringFormat format, int depth) {
        CompilerAsserts.neverPartOfCompilation();
        try {
            InteropLibrary interop = InteropLibrary.getUncached(value);
            if (interop.isNull(value)) {
                return "null";
            } else if (interop.hasArrayElements(value)) {
                return foreignArrayToString(value, allowSideEffects, format, depth);
            } else if (interop.isString(value)) {
                String string = interop.asString(value);
                return format.quoteString() ? quote(string) : string;
            } else if (interop.isBoolean(value)) {
                return booleanToString(interop.asBoolean(value));
            } else if (interop.isNumber(value)) {
                Object unboxed = "Number";
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
                return InteropLibrary.getUncached().asString(interop.getMetaQualifiedName(value));
            } else if (interop.hasMembers(value) && !(interop.isExecutable(value) || interop.isInstantiable(value))) {
                return foreignObjectToString(value, allowSideEffects, format, depth);
            } else {
                return InteropLibrary.getUncached().asString(interop.toDisplayString(value, allowSideEffects));
            }
        } catch (InteropException e) {
            return "Object";
        }
    }

    private static String hostObjectToString(Object value, InteropLibrary interop) throws UnsupportedMessageException {
        if (interop.isMetaObject(value)) {
            return "JavaClass[" + InteropLibrary.getUncached().asString(interop.getMetaQualifiedName(value)) + "]";
        } else {
            Object metaObject = interop.getMetaObject(value);
            return "JavaObject[" + InteropLibrary.getUncached().asString(InteropLibrary.getUncached().getMetaQualifiedName(metaObject)) + "]";
        }
    }

    private static String foreignArrayToString(Object truffleObject, boolean allowSideEffects, ToDisplayStringFormat format, int depth) throws InteropException {
        CompilerAsserts.neverPartOfCompilation();
        InteropLibrary interop = InteropLibrary.getFactory().getUncached(truffleObject);
        assert interop.hasArrayElements(truffleObject);
        long size = interop.getArraySize(truffleObject);
        if (size == 0) {
            return "[]";
        } else if (depth >= format.getMaxDepth()) {
            return "Array(" + size + ")";
        }
        boolean topLevel = depth == 0;
        StringBuilder sb = new StringBuilder();
        if (topLevel && size >= 2 && format.includeArrayLength()) {
            sb.append('(').append(size).append(')');
        }
        sb.append('[');
        for (long i = 0; i < size; i++) {
            if (i > 0) {
                sb.append(", ");
                if (i >= format.getMaxElements()) {
                    sb.append("...");
                    break;
                }
            }
            Object value = interop.readArrayElement(truffleObject, i);
            sb.append(toDisplayStringInner(value, allowSideEffects, format, depth, truffleObject));
        }
        sb.append(']');
        return sb.toString();
    }

    private static String foreignObjectToString(Object truffleObject, boolean allowSideEffects, ToDisplayStringFormat format, int depth) throws InteropException {
        CompilerAsserts.neverPartOfCompilation();
        InteropLibrary objInterop = InteropLibrary.getFactory().getUncached(truffleObject);
        assert objInterop.hasMembers(truffleObject);
        if (allowSideEffects && objInterop.isMemberInvocable(truffleObject, TO_STRING)) {
            return objInterop.invokeMember(truffleObject, TO_STRING).toString();
        }
        Object keys = objInterop.getMembers(truffleObject);
        InteropLibrary keysInterop = InteropLibrary.getFactory().getUncached(keys);
        long keyCount = keysInterop.getArraySize(keys);
        if (keyCount == 0) {
            return "{}";
        } else if (depth >= format.getMaxDepth()) {
            return "{...}";
        }
        StringBuilder sb = new StringBuilder();
        sb.append('{');
        for (long i = 0; i < keyCount; i++) {
            if (i > 0) {
                sb.append(", ");
                if (i >= format.getMaxElements()) {
                    sb.append("...");
                    break;
                }
            }
            Object key = keysInterop.readArrayElement(keys, i);
            assert InteropLibrary.getUncached().isString(key);
            String stringKey = key instanceof String ? (String) key : InteropLibrary.getUncached().asString(key);
            Object value = objInterop.readMember(truffleObject, stringKey);
            sb.append(stringKey);
            sb.append(": ");
            sb.append(toDisplayStringInner(value, allowSideEffects, format, depth, truffleObject));
        }
        sb.append('}');
        return sb.toString();
    }

    private static boolean fillEmptyArrayElements(StringBuilder sb, long index, long prevArrayIndex, boolean prependComma) {
        if (prevArrayIndex < (index - 1)) {
            if (prependComma) {
                sb.append(", ");
            }
            long count = index - prevArrayIndex - 1;
            if (count == 1) {
                sb.append("empty");
            } else {
                sb.append("empty \u00d7 ");
                sb.append(count);
            }
            return true;
        }
        return false;
    }

    public static String collectionToConsoleString(DynamicObject obj, boolean allowSideEffects, ToDisplayStringFormat format, String name, JSHashMap map, int depth) {
        assert JSMap.isJSMap(obj) || JSSet.isJSSet(obj);
        assert name != null;
        int size = map.size();
        StringBuilder sb = new StringBuilder();
        sb.append(name);
        sb.append('(').append(size).append(')');
        if (size > 0 && depth < format.getMaxDepth()) {
            sb.append('{');
            boolean isMap = JSMap.isJSMap(obj);
            boolean isFirst = true;
            JSHashMap.Cursor cursor = map.getEntries();
            while (cursor.advance()) {
                Object key = cursor.getKey();
                if (key != null) {
                    if (!isFirst) {
                        sb.append(", ");
                    }
                    sb.append(toDisplayStringInner(key, allowSideEffects, format, depth, obj));
                    if (isMap) {
                        sb.append(" => ");
                        Object value = cursor.getValue();
                        sb.append(toDisplayStringInner(value, allowSideEffects, format, depth, obj));
                    }
                    isFirst = false;
                }
            }
            sb.append('}');
        }
        return sb.toString();
    }

    @TruffleBoundary
    public static JSException toStringTypeError(Object value) {
        String what = (value == null ? Null.NAME : (JSDynamicObject.isJSDynamicObject(value) ? JSObject.defaultToString((DynamicObject) value) : value.getClass().getName()));
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
        } else if (number instanceof SafeInteger) {
            return doubleToString(((SafeInteger) number).doubleValue());
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

    public static int length(CharSequence cs, ConditionProfile stringProfile, ConditionProfile lazyStringProfile) {
        if (stringProfile.profile(cs instanceof String)) {
            return ((String) cs).length();
        } else if (lazyStringProfile.profile(cs instanceof JSLazyString)) {
            return ((JSLazyString) cs).length();
        }
        return lengthIntl(cs);
    }

    @TruffleBoundary
    private static int lengthIntl(CharSequence cs) {
        return cs.length();
    }

    public static char charAt(CharSequence cs, int index) {
        if (cs instanceof String) {
            return ((String) cs).charAt(index);
        } else if (cs instanceof JSLazyString) {
            return ((JSLazyString) cs).charAt(index);
        }
        return charAtIntl(cs, index);
    }

    @TruffleBoundary
    private static char charAtIntl(CharSequence cs, int index) {
        return cs.charAt(index);
    }

    public static String javaToString(Object obj) {
        if (obj instanceof String) {
            return (String) obj;
        } else if (obj instanceof JSLazyString) {
            return ((JSLazyString) obj).toString();
        }
        return Boundaries.javaToString(obj);
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
        return DoubleConversion.toShortest(value);
    }

    @TruffleBoundary
    public static String formatDtoAPrecision(double value, int precision) {
        return DoubleConversion.toPrecision(value, precision);
    }

    @TruffleBoundary
    public static String formatDtoAExponential(double d, int digits) {
        return DoubleConversion.toExponential(d, digits);
    }

    @TruffleBoundary
    public static String formatDtoAExponential(double d) {
        return DoubleConversion.toExponential(d, -1);
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
    public static TruffleObject toObject(JSContext ctx, Object value) {
        requireObjectCoercible(value, ctx);
        if (CompilerDirectives.injectBranchProbability(CompilerDirectives.LIKELY_PROBABILITY, JSDynamicObject.isJSDynamicObject(value))) {
            return (DynamicObject) value;
        }
        Object unboxedValue = value;
        if (isForeignObject(value)) {
            InteropLibrary interop = InteropLibrary.getUncached(value);
            assert !interop.isNull(value);
            unboxedValue = JSInteropUtil.toPrimitiveOrDefault(value, null, interop, null);
            if (unboxedValue == null) {
                return (TruffleObject) value; // not a boxed primitive value
            }
        }
        return toObjectFromPrimitive(ctx, unboxedValue, true);
    }

    @TruffleBoundary
    public static TruffleObject toObjectFromPrimitive(JSContext ctx, Object value, boolean useJavaWrapper) {
        JSRealm realm = JSRealm.get(null);
        if (value instanceof Boolean) {
            return JSBoolean.create(ctx, realm, (Boolean) value);
        } else if (value instanceof String) {
            return JSString.create(ctx, realm, (String) value);
        } else if (value instanceof JSLazyString) {
            return JSString.create(ctx, realm, (JSLazyString) value);
        } else if (value instanceof BigInt) {
            return JSBigInt.create(ctx, realm, (BigInt) value);
        } else if (isNumber(value)) {
            return JSNumber.create(ctx, realm, (Number) value);
        } else if (value instanceof Symbol) {
            return JSSymbol.create(ctx, realm, (Symbol) value);
        } else {
            assert !isJSNative(value) && isJavaPrimitive(value) : value;
            if (useJavaWrapper) {
                return (TruffleObject) realm.getEnv().asBoxedGuestValue(value);
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
        } else if (a == Undefined.instance || a == Null.instance) {
            return isNullish(b);
        } else if (b == Undefined.instance || b == Null.instance) {
            return isNullish(a);
        } else if (a instanceof Boolean && b instanceof Boolean) {
            return a.equals(b);
        } else if (isString(a) && isString(b)) {
            return a.toString().equals(b.toString());
        } else if (isJavaNumber(a) && isJavaNumber(b)) {
            double da = doubleValue((Number) a);
            double db = doubleValue((Number) b);
            return da == db;
        } else if (JSDynamicObject.isJSDynamicObject(a) && JSDynamicObject.isJSDynamicObject(b)) {
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
            return equalBigIntAndNumber((BigInt) b, (Number) a);
        } else if (isBigInt(a) && isJavaNumber(b)) {
            return equalBigIntAndNumber((BigInt) a, (Number) b);
        } else if (a instanceof Boolean) {
            return equal(booleanToNumber((Boolean) a), b);
        } else if (b instanceof Boolean) {
            return equal(a, booleanToNumber((Boolean) b));
        } else if (isObject(a)) {
            assert b != Undefined.instance && b != Null.instance; // covered by (DynOb, DynOb)
            if (JSOverloadedOperatorsObject.hasOverloadedOperators(a)) {
                if (isObject(b) && !JSOverloadedOperatorsObject.hasOverloadedOperators(b)) {
                    return equal(a, JSObject.toPrimitive((DynamicObject) b));
                }
                if (isObject(b) || isNumber(b) || isBigInt(b) || isString(b)) {
                    return equalOverloaded(a, b);
                } else {
                    return false;
                }
            } else {
                return equal(JSObject.toPrimitive((DynamicObject) a), b);
            }
        } else if (isObject(b)) {
            assert a != Undefined.instance && a != Null.instance; // covered by (DynOb, DynOb)
            assert !isObject(a);
            if (JSOverloadedOperatorsObject.hasOverloadedOperators(b)) {
                if (isNumber(a) || isBigInt(a) || isString(a)) {
                    return equalOverloaded(a, b);
                } else {
                    return false;
                }
            } else {
                return equal(a, JSObject.toPrimitive((DynamicObject) b));
            }
        } else if (isForeignObject(a) || isForeignObject(b)) {
            return equalInterop(a, b);
        } else {
            return false;
        }
    }

    public static boolean isForeignObject(Object value) {
        return value instanceof TruffleObject && isForeignObject((TruffleObject) value);
    }

    public static boolean isForeignObject(TruffleObject value) {
        return !JSDynamicObject.isJSDynamicObject(value) && !(value instanceof Symbol) && !(value instanceof JSLazyString) && !(value instanceof SafeInteger) &&
                        !(value instanceof BigInt);
    }

    private static boolean equalInterop(Object a, Object b) {
        assert (a != null) && (b != null);
        final Object defaultValue = null;
        Object primLeft;
        if (isForeignObject(a)) {
            primLeft = JSInteropUtil.toPrimitiveOrDefault(a, defaultValue, InteropLibrary.getUncached(a), null);
        } else {
            primLeft = isNullOrUndefined(a) ? Null.instance : a;
        }
        Object primRight;
        if (isForeignObject(b)) {
            primRight = JSInteropUtil.toPrimitiveOrDefault(b, defaultValue, InteropLibrary.getUncached(b), null);
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

    private static boolean equalBigIntAndNumber(BigInt a, Number b) {
        if (b instanceof Double || b instanceof Float) {
            double numberVal = doubleValue(b);
            return !Double.isNaN(numberVal) && a.compareValueTo(numberVal) == 0;
        } else {
            return a.compareValueTo(longValue(b)) == 0;
        }
    }

    private static boolean equalOverloaded(Object a, Object b) {
        Object operatorImplementation = OperatorSet.getOperatorImplementation(a, b, "==");
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
        if (isObject(a) || isObject(b)) {
            return false;
        }
        InteropLibrary aInterop = InteropLibrary.getUncached(a);
        InteropLibrary bInterop = InteropLibrary.getUncached(b);
        return aInterop.isIdentical(a, b, bInterop) || (aInterop.isNull(a) && bInterop.isNull(b));
    }

    /**
     * Implementation of the abstract operation RequireObjectCoercible.
     */
    public static <T> T requireObjectCoercible(T argument, JSContext context) {
        if (argument == Undefined.instance || argument == Null.instance || (isForeignObject(argument) && InteropLibrary.getUncached(argument).isNull(argument))) {
            throw Errors.createTypeErrorNotObjectCoercible(argument, null, context);
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
            if ((lastIdx + 1) == string.length()) {
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

    @SuppressWarnings("unused")
    public static boolean isWhiteSpace(char cp) {
        if (isAsciiDigit(cp)) {
            return false; // fastpath
        }
        return (0x0009 <= cp && cp <= 0x000D) || (0x2000 <= cp && cp <= 0x200A) || cp == 0x0020 || cp == 0x00A0 || cp == 0x1680 || cp == 0x2028 || cp == 0x2029 || cp == 0x202F ||
                        cp == 0x205F || cp == 0x3000 || cp == 0xFEFF || (JSConfig.U180EWhitespace && cp == 0x180E);
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
        } else if (isString(property)) {
            long idx = propertyNameToArrayIndex(toStringIsString(property));
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

    public static boolean isAsciiDigit(char c) {
        return '0' <= c && c <= '9';
    }

    @TruffleBoundary
    public static long propertyNameToArrayIndex(String propertyName) {
        if (propertyName != null && arrayIndexLengthInRange(propertyName)) {
            if (isAsciiDigit(propertyName.charAt(0))) {
                return parseArrayIndexRaw(propertyName);
            }
        }
        return INVALID_ARRAY_INDEX;
    }

    public static boolean arrayIndexLengthInRange(String index) {
        int len = index.length();
        return 0 < len && len <= JSRuntime.MAX_UINT32_DIGITS;
    }

    public static long propertyKeyToArrayIndex(Object propertyKey) {
        return propertyKey instanceof String ? propertyNameToArrayIndex((String) propertyKey) : INVALID_ARRAY_INDEX;
    }

    @TruffleBoundary
    public static long propertyNameToIntegerIndex(String propertyName) {
        if (propertyName != null && propertyName.length() > 0 && propertyName.length() <= MAX_INTEGER_INDEX_DIGITS) {
            if (isAsciiDigit(propertyName.charAt(0))) {
                return parseArrayIndexRaw(propertyName);
            }
        }
        return INVALID_INTEGER_INDEX;
    }

    public static long propertyKeyToIntegerIndex(Object propertyKey) {
        return propertyKey instanceof String ? propertyNameToIntegerIndex((String) propertyKey) : INVALID_INTEGER_INDEX;
    }

    /**
     * Is value a native JavaScript object or primitive?
     */
    public static boolean isJSNative(Object value) {
        return JSDynamicObject.isJSDynamicObject(value) || isJSPrimitive(value);
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

    public static String toStringIsString(Object value) {
        assert isString(value);
        if (value instanceof String) {
            return (String) value;
        } else {
            assert isLazyString(value);
            return ((JSLazyString) value).toString();
        }
    }

    /**
     * Is value is a {@link CharSequence} that lazily evaluates to a {@link String}).
     */
    public static boolean isLazyString(Object value) {
        return value instanceof JSLazyString;
    }

    public static boolean isStringClass(Class<?> clazz) {
        return String.class.isAssignableFrom(clazz) || JSLazyString.class.isAssignableFrom(clazz);
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

    public static double doubleValue(Number number, BranchProfile profile) {
        if (number instanceof Double) {
            return ((Double) number).doubleValue();
        }
        if (number instanceof Integer) {
            return ((Integer) number).doubleValue();
        }
        profile.enter();
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

    /**
     * Convert JS number to long.
     */
    public static long toLong(Number value) {
        return longValue(value);
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
        DynamicObject obj = JSOrdinary.create(context, JSRealm.get(null));
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

    public static int getOffset(int start, int length, ConditionProfile profile) {
        if (profile.profile(start < 0)) {
            return Math.max(start + length, 0);
        } else {
            return Math.min(start, length);
        }
    }

    @TruffleBoundary
    public static long parseSafeInteger(String s) {
        return parseSafeInteger(s, 0, s.length(), 10);
    }

    @TruffleBoundary
    public static long parseSafeInteger(String s, int beginIndex, int endIndex, int radix) {
        return parseLong(s, beginIndex, endIndex, radix, radix == 10, MAX_SAFE_INTEGER_LONG);
    }

    /**
     * Parses the substring as a signed long in the safe integer range in the specified radix.
     *
     * @return parsed integer value or {@link #INVALID_SAFE_INTEGER} if the string is not parsable
     *         or not in the safe integer range.
     */
    private static long parseLong(String s, int beginIndex, int endIndex, int radix, boolean parseSign, long limit) {
        assert beginIndex >= 0 && beginIndex <= endIndex && endIndex <= s.length();
        assert radix >= Character.MIN_RADIX && radix <= Character.MAX_RADIX;
        assert limit <= Long.MAX_VALUE / radix - radix;

        boolean negative = false;
        int i = beginIndex;
        if (i >= endIndex) { // ""
            return INVALID_SAFE_INTEGER;
        }
        if (parseSign) {
            char firstChar = s.charAt(i);
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
            char c = s.charAt(i);
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
    public static Number parseRawFitsLong(String string, int radix, int startPos, int endPos, boolean negate) {
        assert startPos < endPos;
        int pos = startPos;

        long value = 0;
        while (pos < endPos) {
            char c = string.charAt(pos);
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
        if (value == 0 && negate && string.charAt(startPos) == '0') {
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
    public static double parseRawDontFitLong(String string, int radix, int startPos, int endPos, boolean negate) {
        assert startPos < endPos;
        int pos = startPos;

        double value = 0;
        while (pos < endPos) {
            char c = string.charAt(pos);
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
    public static boolean createDataProperty(DynamicObject o, Object p, Object v) {
        assert JSRuntime.isObject(o);
        assert JSRuntime.isPropertyKey(p);
        return JSObject.defineOwnProperty(o, p, PropertyDescriptor.createDataDefault(v));
    }

    public static boolean createDataProperty(DynamicObject o, Object p, Object v, boolean doThrow) {
        assert JSRuntime.isObject(o);
        assert JSRuntime.isPropertyKey(p);
        boolean success = JSObject.defineOwnProperty(o, p, PropertyDescriptor.createDataDefault(v), doThrow);
        assert !doThrow || success : "should have thrown";
        return success;
    }

    /**
     * ES2015, 7.3.6 CreateDataPropertyOrThrow(O, P, V).
     */
    public static boolean createDataPropertyOrThrow(DynamicObject o, Object p, Object v) {
        return createDataProperty(o, p, v, true);
    }

    /**
     * Error Cause 7.3.6 CreateNonEnumerableDataPropertyOrThrow(O, P, V).
     */
    public static void createNonEnumerableDataPropertyOrThrow(DynamicObject o, Object p, Object v) {
        PropertyDescriptor newDesc = PropertyDescriptor.createData(v, JSAttributes.getDefaultNotEnumerable());
        definePropertyOrThrow(o, p, newDesc);
    }

    /**
     * ES2016, 7.3.7 DefinePropertyOrThrow(O, P, desc).
     */
    public static void definePropertyOrThrow(DynamicObject o, Object key, PropertyDescriptor desc) {
        assert JSRuntime.isObject(o);
        assert JSRuntime.isPropertyKey(key);
        boolean success = JSObject.getJSClass(o).defineOwnProperty(o, key, desc, true);
        assert success; // we should have thrown instead of returning false
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
    public static DynamicObject createArrayFromList(JSContext context, JSRealm realm, List<? extends Object> list) {
        return JSArray.createConstant(context, realm, Boundaries.listToArray(list));
    }

    /**
     * ES2015 7.2.3 IsCallable(argument).
     */
    public static boolean isCallable(Object value) {
        if (JSFunction.isJSFunction(value)) {
            return true;
        } else if (JSProxy.isJSProxy(value)) {
            return isCallableProxy((DynamicObject) value);
        } else if (value instanceof TruffleObject) {
            return isCallableForeign(value);
        }
        return false;
    }

    public static boolean isCallableIsJSObject(DynamicObject value) {
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
    public static boolean isCallableProxy(DynamicObject proxy) {
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
            return isProxyAnArray((DynamicObject) obj);
        } else if (isForeignObject(obj)) {
            return InteropLibrary.getUncached().hasArrayElements(obj);
        }
        return false;
    }

    @TruffleBoundary
    public static boolean isProxyAnArray(DynamicObject proxy) {
        assert JSProxy.isJSProxy(proxy);
        if (JSProxy.isRevoked(proxy)) {
            throw Errors.createTypeErrorProxyRevoked();
        }
        return isArrayProxyRecurse(proxy);
    }

    @TruffleBoundary
    private static boolean isArrayProxyRecurse(DynamicObject proxy) {
        return isArray(JSProxy.getTarget(proxy));
    }

    /**
     * ES2015 7.1.14 ToPropertyKey(argument).
     */
    @TruffleBoundary
    public static Object toPropertyKey(Object arg) {
        if (arg instanceof String) {
            return arg;
        } else if (arg instanceof Symbol) {
            return arg;
        }
        Object key = toPrimitive(arg);
        if (key instanceof Symbol) {
            return key;
        } else if (isString(key)) {
            return key.toString();
        }
        return toString(key);
    }

    /**
     * ES2015 7.3.12 Call(F, V, arguments).
     */
    public static Object call(Object fnObj, Object holder, Object[] arguments) {
        if (JSFunction.isJSFunction(fnObj)) {
            return JSFunction.call((DynamicObject) fnObj, holder, arguments);
        } else if (JSProxy.isJSProxy(fnObj)) {
            return JSProxy.call((DynamicObject) fnObj, holder, arguments);
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
        if (JSFunction.isJSFunction(fnObj)) {
            return JSFunction.construct((DynamicObject) fnObj, arguments);
        } else if (JSProxy.isJSProxy(fnObj)) {
            return JSProxy.construct((DynamicObject) fnObj, arguments);
        } else if (isForeignObject(fnObj)) {
            return JSInteropUtil.construct(fnObj, arguments);
        } else {
            throw Errors.createTypeErrorNotAFunction(fnObj);
        }
    }

    /**
     * ES2015, 7.1.16 CanonicalNumericIndexString().
     */
    @TruffleBoundary
    public static Object canonicalNumericIndexString(String s) {
        if (s.isEmpty() || !isNumericIndexStart(s.charAt(0))) {
            return Undefined.instance;
        }
        if ("-0".equals(s)) {
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
        if (JSDynamicObject.isJSDynamicObject(obj)) {
            DynamicObject dynObj = (DynamicObject) obj;
            if (JSFunction.isJSFunction(dynObj)) {
                if (JSFunction.isBoundFunction(dynObj)) {
                    return getFunctionRealm(JSFunction.getBoundTargetFunction(dynObj), currentRealm);
                } else {
                    return JSFunction.getRealm(dynObj);
                }
            } else if (JSProxy.isJSProxy(dynObj)) {
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
        } else if (JSProxy.isJSProxy(constrObj)) {
            return isConstructorProxy((DynamicObject) constrObj);
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
    public static boolean isConstructorProxy(DynamicObject constrObj) {
        assert JSProxy.isJSProxy(constrObj);
        return isConstructor(JSProxy.getTarget(constrObj));
    }

    public static boolean isGenerator(Object genObj) {
        if (JSFunction.isJSFunction(genObj) && JSFunction.isGenerator((DynamicObject) genObj)) {
            return true;
        } else if (JSProxy.isJSProxy(genObj)) {
            return isGeneratorProxy((DynamicObject) genObj);
        }
        return false;
    }

    @TruffleBoundary
    public static boolean isGeneratorProxy(DynamicObject genObj) {
        assert JSProxy.isJSProxy(genObj);
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

    public static DynamicObject expectJSObject(Object to, BranchProfile errorBranch) {
        if (!JSDynamicObject.isJSDynamicObject(to)) {
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

    public static boolean isTypedArrayBigIntFactory(TypedArrayFactory factory) {
        return factory == TypedArrayFactory.BigInt64Array || factory == TypedArrayFactory.BigUint64Array;
    }

    public static GraalJSException getException(Object errorObject) {
        if (JSError.isJSError(errorObject)) {
            return JSError.getException((DynamicObject) errorObject);
        } else {
            return UserScriptException.create(errorObject);
        }
    }

}
