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
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.regex.tregex.util.ForeignAccessUtil;

@ImportStatic(ForeignAccessUtil.class)
public abstract class InputCharAtNode extends Node {

    public static InputCharAtNode create() {
        return InputCharAtNodeGen.create();
    }

    public abstract char execute(Object input, int index);

    @Specialization
    public char doCharAt(String input, int index) {
        return input.charAt(index);
    }

    @Specialization
    public char doCharAt(TruffleObject input, int index, @Cached("createReadMessageNode()") Node readNode) {
        try {
            Object c = ForeignAccess.sendRead(readNode, input, index);
            if (c instanceof Character) {
                return (char) c;
            }
            CompilerDirectives.transferToInterpreter();
            throw UnsupportedTypeException.raise(new Object[]{c});
        } catch (UnknownIdentifierException | UnsupportedMessageException e) {
            CompilerDirectives.transferToInterpreter();
            throw new RuntimeException(e);
        }
    }
}
