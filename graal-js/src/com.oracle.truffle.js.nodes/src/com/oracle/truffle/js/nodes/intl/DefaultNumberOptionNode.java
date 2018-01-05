/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.intl;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.cast.JSToNumberNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSException;

public abstract class DefaultNumberOptionNode extends JavaScriptBaseNode {

    @Child JSToNumberNode toNumberNode;
    int maximum;
    Number fallback;

    protected DefaultNumberOptionNode(int maximum, Number fallback) {
        this.maximum = maximum;
        this.fallback = fallback;
    }

    public abstract Number executeValue(Object value, int minimum);

    public static DefaultNumberOptionNode create(int maximum, Number fallback) {
        return DefaultNumberOptionNodeGen.create(maximum, fallback);
    }

    @Specialization(guards = "!isUndefined(value)")
    public Number getOption(Object value, int minimum) {
        Number numValue = toNumber(value);
        int intValue = numValue.intValue();
        if (minimum > intValue || maximum < intValue) {
            createRangeError(value, minimum);
        }
        return intValue;
    }

    @TruffleBoundary
    private void createRangeError(Object value, int minimum) throws JSException {
        throw Errors.createRangeError(String.format("invalid value %d found where only values between %d and %d are allowed", value, minimum, maximum));
    }

    @Specialization(guards = "isUndefined(value)")
    @SuppressWarnings("unused")
    public Number getOptionFromUndefined(Object value, int minimum) {
        return fallback;
    }

    protected Number toNumber(Object value) {
        if (toNumberNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            toNumberNode = insert(JSToNumberNode.create());
        }
        return toNumberNode.executeNumber(value);
    }
}
