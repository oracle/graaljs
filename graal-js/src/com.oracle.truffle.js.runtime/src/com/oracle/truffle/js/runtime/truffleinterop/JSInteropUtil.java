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
    public static final Message EXECUTE = Message.createExecute(0);
    public static final Message INVOKE = Message.createInvoke(0);
    public static final Message NEW = Message.createNew(0);

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

    public static Node createHasKeys() {
        return Message.HAS_KEYS.createNode();
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

    public static Node createCall() {
        return EXECUTE.createNode();
    }

    public static Node createInvoke() {
        return INVOKE.createNode();
    }

    public static Node createNew() {
        return NEW.createNode();
    }

    public static Node createRemove() {
        return Message.REMOVE.createNode();
    }

    public static Node createIsExecutable() {
        return Message.IS_EXECUTABLE.createNode();
    }

    public static Node createIsInstantiable() {
        return Message.IS_INSTANTIABLE.createNode();
    }

    public static Node createKeyInfo() {
        return Message.KEY_INFO.createNode();
    }
}
