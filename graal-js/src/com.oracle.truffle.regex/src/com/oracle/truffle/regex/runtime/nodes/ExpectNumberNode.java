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

public abstract class ExpectNumberNode extends Node {

    public abstract Number execute(Object arg);

    @Specialization
    Number expectNumberInt(int arg) {
        return arg;
    }

    @Specialization
    Number expectNumberLong(long arg) {
        return arg;
    }

    @Specialization
    Number expectNumber(Number arg) {
        return arg;
    }

    @Specialization
    Number expectNumberTruffleObject(TruffleObject arg,
                    @Cached("createIsBoxedNode()") Node isBoxed,
                    @Cached("createUnboxNode()") Node unbox) {
        try {
            if (ForeignAccess.sendIsBoxed(isBoxed, arg)) {
                Object unboxedObject = ForeignAccess.sendUnbox(unbox, arg);
                if (unboxedObject instanceof Number) {
                    return (Number) unboxedObject;
                }
            }
            CompilerDirectives.transferToInterpreter();
            throw UnsupportedTypeException.raise(new Object[]{arg});
        } catch (UnsupportedMessageException e) {
            CompilerDirectives.transferToInterpreter();
            throw UnsupportedTypeException.raise(new Object[]{arg});
        }
    }

    @Fallback
    Number fallback(Object arg) {
        CompilerDirectives.transferToInterpreter();
        throw UnsupportedTypeException.raise(new Object[]{arg});
    }

    static Node createIsBoxedNode() {
        return Message.IS_BOXED.createNode();
    }

    static Node createUnboxNode() {
        return Message.UNBOX.createNode();
    }

    public static ExpectNumberNode create() {
        return ExpectNumberNodeGen.create();
    }
}
