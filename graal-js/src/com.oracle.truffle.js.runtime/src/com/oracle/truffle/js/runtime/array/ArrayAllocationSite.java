/*
 * Copyright (c) 2015, 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.runtime.array;

public interface ArrayAllocationSite {
    default void notifyArrayTransition(@SuppressWarnings("unused") ScriptArray arrayType, @SuppressWarnings("unused") int length) {
    }

    default ScriptArray getInitialArrayType() {
        return null;
    }
}
