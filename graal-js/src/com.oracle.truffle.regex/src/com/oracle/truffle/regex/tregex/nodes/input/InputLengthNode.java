/*
 * Copyright (c) 2017, 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.regex.tregex.nodes.input;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.regex.tregex.util.ForeignAccessUtil;

@ImportStatic(ForeignAccessUtil.class)
public abstract class InputLengthNode extends Node {

    public static InputLengthNode create() {
        return InputLengthNodeGen.create();
    }

    public abstract int execute(Object input);

    @Specialization
    public int getLength(String input) {
        return input.length();
    }

    @Specialization
    public int getLength(TruffleObject input, @Cached("createGetSizeMessageNode()") Node readNode) {
        try {
            Object length = ForeignAccess.sendGetSize(readNode, input);
            if (length instanceof Integer) {
                return (int) length;
            }
            CompilerDirectives.transferToInterpreter();
            throw UnsupportedTypeException.raise(new Object[]{length});
        } catch (UnsupportedMessageException e) {
            CompilerDirectives.transferToInterpreter();
            throw new RuntimeException(e);
        }
    }
}
