/*
 * Copyright (c) 2017, 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.regex.tregex.nodes.input;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.nodes.Node;

public abstract class InputRegionMatchesNode extends Node {

    public static InputRegionMatchesNode create() {
        return InputRegionMatchesNodeGen.create();
    }

    public abstract boolean execute(Object input, String match, int fromIndex);

    @Specialization
    public boolean regionMatches(String input, String match, int fromIndex) {
        return input.regionMatches(fromIndex, match, 0, match.length());
    }

    @Specialization
    public boolean regionMatches(TruffleObject input, String match, int fromIndex,
                    @Cached("create()") InputLengthNode lengthNode,
                    @Cached("create()") InputCharAtNode charAtNode) {
        if (fromIndex + match.length() > lengthNode.execute(input)) {
            return false;
        }
        for (int i = 0; i < match.length(); i++) {
            if (charAtNode.execute(input, fromIndex + i) != match.charAt(i)) {
                return false;
            }
        }
        return true;
    }
}
