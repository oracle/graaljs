/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.trufflenode.info;

/**
 *
 * @author Jan Stola
 */
public final class Value {

    private final Object name;
    private final Object value;
    private final int attributes;

    public Value(Object name, Object value, int attributes) {
        this.name = name;
        this.value = value;
        this.attributes = attributes;
    }

    public Object getValue() {
        return value;
    }

    public Object getName() {
        return name;
    }

    public int getAttributes() {
        return attributes;
    }

}
