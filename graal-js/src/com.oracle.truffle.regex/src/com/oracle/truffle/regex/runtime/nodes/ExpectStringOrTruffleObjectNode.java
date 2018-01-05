/*
 * Copyright (c) 2017, 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.regex.runtime.nodes;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.api.nodes.Node;

public abstract class ExpectStringOrTruffleObjectNode extends Node {

    public abstract Object execute(Object arg);

    @Specialization
    Object doString(String arg) {
        return arg;
    }

    @Specialization
    Object doTruffleObject(TruffleObject arg,
                    @Cached("createIsBoxedNode()") Node isBoxed,
                    @Cached("createUnboxNode()") Node unbox) {
        try {
            if (ForeignAccess.sendIsBoxed(isBoxed, arg)) {
                Object unboxedObject = ForeignAccess.sendUnbox(unbox, arg);
                if (unboxedObject instanceof String) {
                    return unboxedObject;
                }
                CompilerDirectives.transferToInterpreter();
                throw UnsupportedTypeException.raise(new Object[]{arg});
            }
            return arg;
        } catch (UnsupportedMessageException e) {
            CompilerDirectives.transferToInterpreter();
            throw UnsupportedTypeException.raise(new Object[]{arg});
        }
    }

    @Fallback
    Object doPrimitive(Object arg) {
        throw UnsupportedTypeException.raise(new Object[]{arg});
    }

    static Node createIsBoxedNode() {
        return Message.IS_BOXED.createNode();
    }

    static Node createUnboxNode() {
        return Message.UNBOX.createNode();
    }

    public static ExpectStringOrTruffleObjectNode create() {
        return ExpectStringOrTruffleObjectNodeGen.create();
    }
}
