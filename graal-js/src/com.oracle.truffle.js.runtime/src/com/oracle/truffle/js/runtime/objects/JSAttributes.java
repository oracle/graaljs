/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.runtime.objects;

public final class JSAttributes {
    /**
     * ES5 8.6.1 Property Attributes.
     */
    public static final String VALUE = "value";
    public static final String GET = "get";
    public static final String SET = "set";
    public static final String WRITABLE = "writable";
    public static final String ENUMERABLE = "enumerable";
    public static final String CONFIGURABLE = "configurable";

    /** ES5 8.6.1 - Is this property not enumerable? */
    public static final int NOT_ENUMERABLE = 1 << 0;

    /** ES5 8.6.1 - Is this property not configurable? */
    public static final int NOT_CONFIGURABLE = 1 << 1;

    /** ES5 8.6.1 - Is this property not writable? */
    public static final int NOT_WRITABLE = 1 << 2;

    public static final int ATTRIBUTES_MASK = NOT_ENUMERABLE | NOT_CONFIGURABLE | NOT_WRITABLE;

    private JSAttributes() {
    }

    public static int getDefault() {
        return configurableEnumerableWritable();
    }

    public static int getDefaultNotEnumerable() {
        return configurableNotEnumerableWritable();
    }

    public static int configurableEnumerableWritable() {
        return CONFIGURABLE_ENUMERABLE_WRITABLE;
    }

    public static int configurableNotEnumerableWritable() {
        return CONFIGURABLE_NOT_ENUMERABLE_WRITABLE;
    }

    public static int configurableEnumerableNotWritable() {
        return CONFIGURABLE_ENUMERABLE_NOT_WRITABLE;
    }

    public static int configurableNotEnumerableNotWritable() {
        return CONFIGURABLE_NOT_ENUMERABLE_NOT_WRITABLE;
    }

    public static int notConfigurableNotEnumerableNotWritable() {
        return NOT_CONFIGURABLE_NOT_ENUMERABLE_NOT_WRITABLE;
    }

    public static int notConfigurableNotEnumerableWritable() {
        return NOT_CONFIGURABLE_NOT_ENUMERABLE_WRITABLE;
    }

    public static int notConfigurableEnumerableWritable() {
        return NOT_CONFIGURABLE_ENUMERABLE_WRITABLE;
    }

    public static int notConfigurableEnumerableNotWritable() {
        return NOT_CONFIGURABLE_ENUMERABLE_NOT_WRITABLE;
    }

    public static int getAccessorDefault() {
        return configurableEnumerableWritable();
    }

    public static int notConfigurableNotEnumerable() {
        return notConfigurableNotEnumerableWritable();
    }

    public static int configurableNotEnumerable() {
        return configurableNotEnumerableWritable();
    }

    public static int fromConfigurableEnumerableWritable(boolean configurable, boolean enumerable, boolean writable) {
        return (!configurable ? NOT_CONFIGURABLE : 0) | (!enumerable ? NOT_ENUMERABLE : 0) | (!writable ? NOT_WRITABLE : 0);
    }

    public static int fromConfigurableEnumerable(boolean configurable, boolean enumerable) {
        return (!configurable ? NOT_CONFIGURABLE : 0) | (!enumerable ? NOT_ENUMERABLE : 0);
    }

    private static final int NOT_CONFIGURABLE_ENUMERABLE_WRITABLE = NOT_CONFIGURABLE;
    private static final int NOT_CONFIGURABLE_ENUMERABLE_NOT_WRITABLE = NOT_CONFIGURABLE | NOT_WRITABLE;
    private static final int NOT_CONFIGURABLE_NOT_ENUMERABLE_WRITABLE = NOT_CONFIGURABLE | NOT_ENUMERABLE;
    private static final int NOT_CONFIGURABLE_NOT_ENUMERABLE_NOT_WRITABLE = NOT_CONFIGURABLE | NOT_ENUMERABLE | NOT_WRITABLE;
    private static final int CONFIGURABLE_NOT_ENUMERABLE_WRITABLE = NOT_ENUMERABLE;
    private static final int CONFIGURABLE_NOT_ENUMERABLE_NOT_WRITABLE = NOT_ENUMERABLE | NOT_WRITABLE;
    private static final int CONFIGURABLE_ENUMERABLE_NOT_WRITABLE = NOT_WRITABLE;
    private static final int CONFIGURABLE_ENUMERABLE_WRITABLE = 0;
}
