/*
 * Copyright (c) 2017, 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.regex.tregex.nodes.input;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.regex.tregex.util.Boundaries;

public abstract class InputLastIndexOfNode extends Node {

    public static InputLastIndexOfNode create() {
        return InputLastIndexOfNodeGen.create();
    }

    public abstract int execute(Object input, char c, int fromIndex, int maxIndex);

    @Specialization
    public int lastIndexOf(String input, char c, int fromIndex, int maxIndex) {
        int index = Boundaries.stringLastIndexOf(input, c, fromIndex);
        if (index < maxIndex) {
            return -1;
        }
        return index;
    }

    @Specialization
    public int lastIndexOf(TruffleObject input, char c, int fromIndex, int maxIndex,
                    @Cached("create()") InputCharAtNode charAtNode) {
        for (int i = fromIndex; i >= maxIndex; i--) {
            if (charAtNode.execute(input, i) == c) {
                return i;
            }
        }
        return -1;
    }
}
