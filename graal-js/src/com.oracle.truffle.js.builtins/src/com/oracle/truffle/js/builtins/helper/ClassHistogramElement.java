/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.builtins.helper;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;

/**
 * Class histogram element for IR / Java object instrumentation.
 */
public class ClassHistogramElement {
    private final Class<?> clazz;
    private long instances;
    private long bytes;

    /**
     * Constructor.
     *
     * @param clazz class for which to construct histogram
     */
    public ClassHistogramElement(final Class<?> clazz) {
        this.clazz = clazz;
    }

    /**
     * Add an instance.
     *
     * @param sizeInBytes byte count
     */
    public void addInstance(final long sizeInBytes) {
        instances++;
        this.bytes += sizeInBytes;
    }

    /**
     * Get size in bytes.
     *
     * @return size in bytes
     */
    public long getBytes() {
        return bytes;
    }

    /**
     * Get class.
     *
     * @return class
     */
    public Class<?> getClazz() {
        return clazz;
    }

    /**
     * Get number of instances.
     *
     * @return number of instances
     */
    public long getInstances() {
        return instances;
    }

    @Override
    @TruffleBoundary
    public String toString() {
        return "ClassHistogramElement[class=" + clazz.getCanonicalName() + ", instances=" + instances + ", bytes=" + bytes + "]";
    }
}
