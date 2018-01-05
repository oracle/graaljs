/*
 * Copyright (c) 2017, 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.regex.tregex.nodes.input;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.nodes.Node;

public abstract class InputEqualsNode extends Node {

    public static InputEqualsNode create() {
        return InputEqualsNodeGen.create();
    }

    public abstract boolean execute(Object input, String string);

    @Specialization
    public boolean execEquals(String input, String string) {
        return input.equals(string);
    }

    @Specialization
    public boolean execEquals(TruffleObject input, String string,
                    @Cached("create()") InputLengthNode lengthNode,
                    @Cached("create()") InputCharAtNode charAtNode) {
        if (lengthNode.execute(input) != string.length()) {
            return false;
        }
        for (int i = 0; i < string.length(); i++) {
            if (charAtNode.execute(input, i) != string.charAt(i)) {
                return false;
            }
        }
        return true;
    }
}
