/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.runtime.util;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;

public final class DirectByteBufferHelper {
    private static final Class<? extends ByteBuffer> DIRECT_BYTE_BUFFER_CLASS = ByteBuffer.allocateDirect(0).getClass();

    private DirectByteBufferHelper() {
    }

    @TruffleBoundary
    private static ByteBuffer allocateDirectImpl(int length) {
        return ByteBuffer.allocateDirect(length).order(ByteOrder.nativeOrder());
    }

    public static ByteBuffer allocateDirect(int length) {
        return cast(allocateDirectImpl(length));
    }

    public static ByteBuffer cast(ByteBuffer buffer) {
        return CompilerDirectives.castExact(buffer, DIRECT_BYTE_BUFFER_CLASS);
    }
}
