/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.runtime.interop;

import java.util.*;

import com.oracle.truffle.js.runtime.JSTruffleOptions;

public final class JavaSuperAdapter {
    private final Object adapter;

    JavaSuperAdapter(Object adapter) {
        assert JSTruffleOptions.NashornJavaInterop;
        this.adapter = Objects.requireNonNull(adapter);
        assert !(adapter instanceof JavaSuperAdapter);
    }

    public Object getAdapter() {
        return adapter;
    }
}
