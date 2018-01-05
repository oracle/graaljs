/*
 * Copyright (c) 2017, 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.regex.tregex.nodes.input;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.nodes.Node;

public abstract class InputStartsWithNode extends Node {

    public static InputStartsWithNode create() {
        return InputStartsWithNodeGen.create();
    }

    public abstract boolean execute(Object input, String prefix);

    @Specialization
    public boolean startsWith(String input, String prefix) {
        return input.startsWith(prefix);
    }

    @Specialization
    public boolean startsWith(TruffleObject input, String prefix,
                    @Cached("create()") InputLengthNode lengthNode,
                    @Cached("create()") InputCharAtNode charAtNode) {
        if (lengthNode.execute(input) < prefix.length()) {
            return false;
        }
        for (int i = 0; i < prefix.length(); i++) {
            if (charAtNode.execute(input, i) != prefix.charAt(i)) {
                return false;
            }
        }
        return true;
    }
}
