package com.oracle.truffle.js.runtime.util;

import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import com.oracle.truffle.js.nodes.access.IsObjectNode;
import com.oracle.truffle.js.nodes.cast.JSToBooleanNode;
import com.oracle.truffle.js.nodes.cast.JSToNumberNode;
import com.oracle.truffle.js.nodes.cast.JSToStringNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class TemporalUtil {

    private static final String YEARS = "years";
    private static final String MONTHS = "months";
    private static final String WEEKS = "weeks";
    private static final String DAYS = "days";
    private static final String HOURS = "hours";
    private static final String MINUTES = "minutes";
    private static final String SECONDS = "seconds";
    private static final String MILLISECONDS = "milliseconds";
    private static final String MICROSECONDS = "microseconds";
    private static final String NANOSECONDS = "nanoseconds";

    private static final Set<String> singularUnits = toSet("year", "month", "day", "hour", "minute", "second",
            "millisecond", "microsecond", "nanosecond");
    private static final Set<String> pluralUnits = toSet(YEARS, MONTHS, DAYS, HOURS, MINUTES, SECONDS,
            MILLISECONDS, MICROSECONDS, NANOSECONDS);
    private static final Map<String, String> pluralToSingular = toMap(
            new String[] {YEARS, MONTHS, DAYS, HOURS, MINUTES, SECONDS, MILLISECONDS, MICROSECONDS, NANOSECONDS},
            new String[] {"year", "month", "day", "hour", "minute", "second", "millisecond", "microsecond", "nanosecond"}
    );

    // 15.1
    public static DynamicObject normalizeOptionsObject(DynamicObject options,
                                                       JSRealm realm,
                                                       IsObjectNode isObject) {
        if(JSRuntime.isNullOrUndefined(options)) {
            DynamicObject newOptions = JSObjectUtil.createOrdinaryPrototypeObject(realm);
            return  newOptions;
        }
        if(isObject.executeBoolean(options)) {
            return options;
        }
        throw Errors.createTypeError("Options is not undefined and not an object.");
    }

    // 15.2
    public static Object getOptions(DynamicObject options, String property, String type,
                                    Set<Object> values, Object fallback,
                                    DynamicObjectLibrary dol, IsObjectNode isObjectNode,
                                    JSToBooleanNode toBoolean, JSToStringNode toString) {
        assert isObjectNode.executeBoolean(options);
        Object value = dol.getOrDefault(options, property, null);
        if(value == null) {
            return fallback;
        }
        assert type.equals("boolean") || type.equals("string");
        if(type.equals("boolean")) {
            value = toBoolean.executeBoolean(value);
        } else if(type.equals("string")) {
            value = toString.executeString(value);
        }
        if(values != null && !values.contains(value)) {
            throw Errors.createRangeError(
                    String.format("Given options value: %s is not contained in values: %s", value, values));
        }
        return value;
    }

    // 15.3
    public static double defaultNumberOptions(Object value, double minimum, double maximum, double fallback,
                                             JSToNumberNode toNumber) {
        if (value == null) {
            return fallback;
        }
        double numberValue = toNumber.executeNumber(value).doubleValue();
        if (numberValue == Double.NaN || numberValue < minimum || numberValue > maximum) {
            throw Errors.createRangeError("Numeric value out of range.");
        }
        return Math.floor(numberValue);
    }

    // 15.4
    public static double getNumberOption(DynamicObject options, String property, double minimum, double maximum,
                                   double fallback, DynamicObjectLibrary dol, IsObjectNode isObject,
                                   JSToNumberNode numberNode) {
        assert isObject.executeBoolean(options);
        Object value = dol.getOrDefault(options, property, null);
        return defaultNumberOptions(value, minimum, maximum, fallback, numberNode);
    }

    // 15.8
    public static String toTemporalOverflow(DynamicObject normalizedOptions,
                                            DynamicObjectLibrary dol,
                                            IsObjectNode isObjectNode,
                                            JSToBooleanNode toBoolean, JSToStringNode toString) {
        Set<Object> values = new HashSet<>();
        values.add("constrain");
        values.add("reject");
        return (String) getOptions(normalizedOptions, "overflow", "string", values, "constrain", dol,
                isObjectNode, toBoolean, toString);
    }

    // 15.11
    public static String toTemporalRoundingMode(DynamicObject normalizedOptions, String fallback,
                                                DynamicObjectLibrary dol, IsObjectNode isObjectNode,
                                                JSToBooleanNode toBoolean, JSToStringNode toString) {
        return (String) getOptions(normalizedOptions, "roundingMode", "string", toSet("ceil", "floor", "trunc", "nearest"),
                fallback, dol, isObjectNode, toBoolean, toString);
    }

    // 15.17
    public static double toTemporalRoundingIncrement(DynamicObject normalizedOptions, Double dividend, boolean inclusive,
                                                     DynamicObjectLibrary dol, IsObjectNode isObject,
                                                     JSToNumberNode toNumber) {
        double maximum;
        if(dividend == null) {
            maximum = Double.POSITIVE_INFINITY;
        } else if (inclusive) {
            maximum = dividend;
        } else if (dividend > 1) {
            maximum = dividend - 1;
        } else {
            maximum = 1;
        }
        double increment = getNumberOption(normalizedOptions, "roundingIncrement", 1, maximum, 1,
                dol, isObject, toNumber);
        if (dividend != null && dividend % increment != 0) {
            throw Errors.createRangeError("Increment out of range.");
        }
        return increment;
    }

    // 15.21
    public static String toSmallestTemporalUnit(DynamicObject normalizedOptions, Set<String> disallowedUnits,
                                                DynamicObjectLibrary dol, IsObjectNode isObjectNode,
                                                JSToBooleanNode toBoolean, JSToStringNode toString) {
        String smallestUnit = (String) getOptions(normalizedOptions, "smallestUnit", "string", toSet("day", "days", "hour",
                "hours", "minute", "minutes", "second", "seconds", "millisecond", "milliseconds", "microsecond",
                "microseconds", "nanosecond", "nanoseconds"), null, dol, isObjectNode, toBoolean, toString);
        if (smallestUnit == null) {
            throw Errors.createRangeError("No smallest unit found.");
        }
        if(pluralUnits.contains(smallestUnit)) {
            smallestUnit = pluralToSingular.get(smallestUnit);
        }
        if(disallowedUnits.contains(smallestUnit)) {
            throw Errors.createRangeError("Smallest unit not allowed");
        }
        return smallestUnit;
    }

    // 15.25
    public static DynamicObject toRelativeTemporalObject(DynamicObject options, IsObjectNode isObject,
                                                         DynamicObjectLibrary dol) {
        assert isObject.executeBoolean(options);
        DynamicObject value = (DynamicObject) dol.getOrDefault(options, "relativeTo", null);
        if (value == null) {
            return value;
        }
        // TODO: https://tc39.es/proposal-temporal/#sec-temporal-torelativetemporalobject
        return null;
    }

    // 15.27
    public static String largerOfTwoTemporalDurationUnits(String u1, String u2) {
        if(u1.equals(YEARS) || u2.equals(YEARS)) {
            return YEARS;
        }
        if(u1.equals(MONTHS) || u2.equals(MONTHS)) {
            return MONTHS;
        }
        if(u1.equals(WEEKS) || u2.equals(WEEKS)) {
            return WEEKS;
        }
        if(u1.equals(DAYS) || u2.equals(DAYS)) {
            return DAYS;
        }
        if(u1.equals(HOURS) || u2.equals(HOURS)) {
            return HOURS;
        }
        if(u1.equals(MINUTES) || u2.equals(MINUTES)) {
            return MINUTES;
        }
        if(u1.equals(SECONDS) || u2.equals(SECONDS)) {
            return SECONDS;
        }
        if(u1.equals(MILLISECONDS) || u2.equals(MILLISECONDS)) {
            return MILLISECONDS;
        }
        if(u1.equals(MICROSECONDS) || u2.equals(MICROSECONDS)) {
            return MICROSECONDS;
        }
        return NANOSECONDS;
    }

    // 15.30
    public static double nonNegativeModulo(double x, double y) {
        double result = x % y;
        if (result == -0) {
            return 0;
        }
        if (result < 0) {
            result = result + y;
        }
        return result;
    }

    // 15.32
    public static long constraintToRange(long x, long minimum, long maximum) {
        return Math.min(Math.max(x, minimum), maximum);
    }

    // 15.34
    public static double roundHalfAwayFromZero(double x) {
        return Math.round(x);
    }

    // 15.35
    public static double roundNumberToIncrement(double x, double increment, String roundingMode) {
        assert roundingMode.equals("ceil") || roundingMode.equals("floor") || roundingMode.equals("trunc")
                || roundingMode.equals("nearest");
        double quotient = x / increment;
        double rounded;
        if (roundingMode.equals("ceil")) {
            rounded = -Math.floor(-quotient);
        } else if (roundingMode.equals("floor")) {
            rounded = Math.floor(quotient);
        } else if (roundingMode.equals("trunc")) {
            rounded = (long) quotient;
        } else {
            rounded = roundHalfAwayFromZero(x);
        }
        return rounded * increment;
    }

    private static <T> Set<T> toSet(T... values) {
        return Arrays.stream(values).collect(Collectors.toSet());
    }

    private static <T,I> Map<T,I> toMap(T[] keys, I[] values) {
        Map<T, I> map = new HashMap<>();
        for (int i = 0; i < keys.length; i++) {
            map.put(keys[i], values[i]);
        }
        return map;
    }
}
