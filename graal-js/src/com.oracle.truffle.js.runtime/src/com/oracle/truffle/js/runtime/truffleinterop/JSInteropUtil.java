/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.runtime.truffleinterop;

import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.nodes.Node;

/**
 * Utility class for interop operations. Provides methods that can be used in Cached annotations of
 * the TruffleDSL to create interop nodes just for specific specializations.
 *
 */
public final class JSInteropUtil {
    private JSInteropUtil() {
        // this class should not be instantiated
    }

    public static Node createHasSize() {
        return Message.HAS_SIZE.createNode();
    }

    public static Node createGetSize() {
        return Message.GET_SIZE.createNode();
    }

    public static Node createRead() {
        return Message.READ.createNode();
    }

    public static Node createWrite() {
        return Message.WRITE.createNode();
    }

    public static Node createKeys() {
        return Message.KEYS.createNode();
    }

    public static Node createIsBoxed() {
        return Message.IS_BOXED.createNode();
    }

    public static Node createUnbox() {
        return Message.UNBOX.createNode();
    }

    public static Node createIsNull() {
        return Message.IS_NULL.createNode();
    }

    public static Node createCall(int length) {
        return Message.createExecute(length).createNode();
    }

    public static Node createInvoke(int length) {
        return Message.createInvoke(length).createNode();
    }

    public static Node createNew(int length) {
        return Message.createNew(length).createNode();
    }
}
