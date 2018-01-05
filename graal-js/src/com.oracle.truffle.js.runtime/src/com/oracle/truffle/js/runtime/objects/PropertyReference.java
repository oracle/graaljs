/*
 * Copyright (c) 2014, 2014, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.runtime.objects;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.object.Property;
import com.oracle.truffle.api.object.Shape;

public final class PropertyReference implements CharSequence {
    private final Property property;
    private final Shape shape;
    private final int depth;

    public PropertyReference(Property property, Shape shape, int depth) {
        this.property = property;
        this.shape = shape;
        this.depth = depth;
    }

    public Property getProperty() {
        return property;
    }

    public Shape getShape() {
        return shape;
    }

    public int getDepth() {
        return depth;
    }

    public Object getKey() {
        return property.getKey();
    }

    @Override
    @TruffleBoundary
    public String toString() {
        return (String) getKey();
    }

    @Override
    public int length() {
        return toString().length();
    }

    @Override
    public char charAt(int index) {
        return toString().charAt(index);
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        return toString().subSequence(start, end);
    }
}
