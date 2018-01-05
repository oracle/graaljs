/*
 * Copyright (c) 2017, 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.regex.tregex.nodes.input;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.nodes.Node;

public abstract class InputToStringNode extends Node {

    public static InputToStringNode create() {
        return InputToStringNodeGen.create();
    }

    public abstract String execute(Object input);

    @Specialization
    public String doString(String input) {
        return input;
    }

    @Specialization
    public String doTruffleObject(TruffleObject input,
                    @Cached("create()") InputLengthNode lengthNode,
                    @Cached("create()") InputCharAtNode charAtNode) {
        final int inputLength = lengthNode.execute(input);
        StringBuilder sb = createStringBuilder(inputLength);
        for (int i = 0; i < inputLength; i++) {
            stringBuilderAppend(sb, charAtNode.execute(input, i));
        }
        return stringBuilderToString(sb);
    }

    @TruffleBoundary
    private static StringBuilder createStringBuilder(int inputLength) {
        return new StringBuilder(inputLength);
    }

    @TruffleBoundary
    private static void stringBuilderAppend(StringBuilder stringBuilder, char c) {
        stringBuilder.append(c);
    }

    @TruffleBoundary
    private static String stringBuilderToString(StringBuilder stringBuilder) {
        return stringBuilder.toString();
    }
}
