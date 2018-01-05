/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.runtime.objects;

import com.oracle.truffle.api.object.*;

public interface PropertyProxy {

    Object get(DynamicObject store);

    default boolean set(@SuppressWarnings("unused") DynamicObject store, @SuppressWarnings("unused") Object value) {
        return true;
    }
}
