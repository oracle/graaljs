/*
 * Copyright (c) 2017, 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.regex.tregex.nodes.input;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.nodes.Node;

public abstract class InputEndsWithNode extends Node {

    public static InputEndsWithNode create() {
        return InputEndsWithNodeGen.create();
    }

    public abstract boolean execute(Object input, String suffix);

    @Specialization
    public boolean endsWith(String input, String suffix) {
        return input.endsWith(suffix);
    }

    @Specialization
    public boolean endsWith(TruffleObject input, String suffix,
                    @Cached("create()") InputLengthNode lengthNode,
                    @Cached("create()") InputCharAtNode charAtNode) {
        final int inputLength = lengthNode.execute(input);
        if (lengthNode.execute(input) < suffix.length()) {
            return false;
        }
        final int offset = inputLength - suffix.length();
        for (int i = 0; i < suffix.length(); i++) {
            if (charAtNode.execute(input, offset + i) != suffix.charAt(i)) {
                return false;
            }
        }
        return true;
    }
}
