package com.oracle.truffle.js.runtime.util;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import com.oracle.truffle.js.nodes.access.IsJSObjectNode;
import com.oracle.truffle.js.nodes.access.IsObjectNode;
import com.oracle.truffle.js.nodes.cast.JSToBooleanNode;
import com.oracle.truffle.js.nodes.cast.JSToStringNode;
import com.oracle.truffle.js.nodes.unary.IsIdenticalUndefinedNode;
import com.oracle.truffle.js.nodes.unary.IsIdenticalUndefinedNodeGen;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;

import java.util.HashSet;
import java.util.Set;

public final class TemporalUtil {

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

    // 15.32
    public static long constraintToRange(long x, long minimum, long maximum) {
        return Math.min(Math.max(x, minimum), maximum);
    }

}
