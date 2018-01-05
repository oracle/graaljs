/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.intl;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.access.PropertyGetNode;
import com.oracle.truffle.js.nodes.cast.JSToStringNode;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.objects.Undefined;
import java.util.Arrays;
import java.util.List;

public abstract class GetStringOptionNode extends JavaScriptBaseNode {

    private final List<String> validValues;
    private final String fallback;
    @Child PropertyGetNode propertyGetNode;

    @Child JSToStringNode toStringNode = JSToStringNode.create();

    protected GetStringOptionNode(JSContext context, String property, String[] values, String fallback) {
        this.validValues = values != null ? Arrays.asList(values) : null;
        this.fallback = fallback;
        this.propertyGetNode = PropertyGetNode.create(property, false, context);
    }

    public abstract String executeValue(Object options);

    public static GetStringOptionNode create(JSContext context, String property, String[] values, String fallback) {
        return GetStringOptionNodeGen.create(context, property, values, fallback);
    }

    protected String makeFinalSelection(String value) {
        if (validValues == null) {
            return value;
        } else {
            ensureSelectedValueIsValid(value);
        }
        return value;
    }

    @TruffleBoundary
    private void ensureSelectedValueIsValid(String value) {
        if (!validValues.contains(value)) {
            throw Errors.createRangeError(String.format("invalid option %s found where only %s is allowed", value, validValues.toString()));
        }
    }

    @Specialization
    public String getOption(Object options) {
        Object propertyValue = propertyGetNode.getValue(options);
        if (propertyValue == Undefined.instance) {
            return fallback;
        }
        return makeFinalSelection(toOptionType(propertyValue));
    }

    protected String toOptionType(Object propertyValue) {
        return toStringNode.executeString(propertyValue);
    }

}
