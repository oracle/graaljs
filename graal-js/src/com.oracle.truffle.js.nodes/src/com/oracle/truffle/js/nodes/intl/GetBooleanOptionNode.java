/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.intl;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.access.PropertyGetNode;
import com.oracle.truffle.js.nodes.cast.JSToBooleanNode;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.objects.Undefined;

public abstract class GetBooleanOptionNode extends JavaScriptBaseNode {

    private final Boolean fallback;

    @Child PropertyGetNode propertyGetNode;
    @Child JSToBooleanNode toBooleanNode = JSToBooleanNode.create();

    protected GetBooleanOptionNode(JSContext context, String property, Boolean fallback) {
        this.fallback = fallback;
        this.propertyGetNode = PropertyGetNode.create(property, false, context);
    }

    public abstract Boolean executeValue(Object options);

    public static GetBooleanOptionNode create(JSContext context, String property, Boolean fallback) {
        return GetBooleanOptionNodeGen.create(context, property, fallback);
    }

    @Specialization
    public Boolean getOption(Object options) {
        Object propertyValue = propertyGetNode.getValue(options);
        if (propertyValue == Undefined.instance) {
            return fallback;
        }
        return toOptionType(propertyValue);
    }

    protected Boolean toOptionType(Object propertyValue) {
        return toBooleanNode.executeBoolean(propertyValue);
    }
}
