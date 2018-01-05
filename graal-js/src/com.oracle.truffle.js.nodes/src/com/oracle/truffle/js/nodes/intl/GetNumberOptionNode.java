/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.intl;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.access.PropertyGetNode;
import com.oracle.truffle.js.nodes.cast.JSToNumberNode;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.objects.Undefined;

public abstract class GetNumberOptionNode extends JavaScriptBaseNode {

    private final Number maximum;
    @Child PropertyGetNode propertyGetNode;

    @Child JSToNumberNode toNumberNode = JSToNumberNode.create();

    protected GetNumberOptionNode(JSContext context, String property, Number maximum) {
        this.maximum = maximum;
        this.propertyGetNode = PropertyGetNode.create(property, false, context);
    }

    public abstract Number executeValue(Object options, Number minimum, Number fallback);

    public static GetNumberOptionNode create(JSContext context, String property, Number maximum) {
        return GetNumberOptionNodeGen.create(context, property, maximum);
    }

    protected Number makeFinalSelection(Number value, Number minimum) {
        ensureSelectedValueIsValid(value, minimum);
        return value.intValue();
    }

    @TruffleBoundary
    private void ensureSelectedValueIsValid(Number value, Number minimum) {
        if (JSRuntime.isNaN(value) || minimum.doubleValue() > value.doubleValue() || maximum.doubleValue() < value.doubleValue()) {
            throw Errors.createRangeError(String.format("invalid value %d found where only values between %d and %d are allowed", value, minimum, maximum));
        }
    }

    @Specialization(guards = "!isUndefined(options)")
    public Number getOption(Object options, Number minimum, Number fallback) {
        Object propertyValue = propertyGetNode.getValue(options);
        if (propertyValue == Undefined.instance) {
            return fallback;
        }
        return makeFinalSelection(toOptionType(propertyValue), minimum);
    }

    @Specialization(guards = "isUndefined(options)")
    @SuppressWarnings("unused")
    public Number getOptionFromUndefined(Object options, Number minimum, Number fallback) {
        return fallback;
    }

    protected Number toOptionType(Object propertyValue) {
        return toNumberNode.executeNumber(propertyValue);
    }
}
