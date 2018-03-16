/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.instrumentation;

import java.util.HashMap;
import java.util.Map;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.TruffleObject;

/**
 * A container class used to store per-node attributes used by the instrumentation framework.
 *
 */
public class NodeObjectDescriptor implements TruffleObject {

    private final Map<String, Object> data = new HashMap<>();

    @Override
    public ForeignAccess getForeignAccess() {
        return NodeObjectDescriptorFactoryForeign.ACCESS;
    }

    @TruffleBoundary
    public void addProperty(String name, Object value) {
        data.put(name, value);
    }

    @TruffleBoundary
    public int size() {
        return data.size();
    }

    @TruffleBoundary
    public Object getProperty(String name) {
        assert hasProperty(name);
        return data.get(name);
    }

    @TruffleBoundary
    public boolean hasProperty(String name) {
        return data.containsKey(name);
    }

    @TruffleBoundary
    public TruffleObject getPropertyNames() {
        return new NodeObjectDescriptorKeys(data);
    }

    static boolean isInstance(TruffleObject object) {
        return object instanceof NodeObjectDescriptor;
    }
}
