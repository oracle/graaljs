/*
 * Copyright (c) 2015, 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.runtime.objects;

public final class Dead {
    private static final Dead INSTANCE = new Dead();

    private Dead() {
    }

    public static Dead instance() {
        return INSTANCE;
    }
}
