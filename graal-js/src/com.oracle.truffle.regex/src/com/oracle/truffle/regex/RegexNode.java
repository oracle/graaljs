/*
 * Copyright (c) 2012, 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.regex;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.regex.result.RegexResult;
import com.oracle.truffle.regex.tregex.nodes.input.InputCharAtNode;
import com.oracle.truffle.regex.tregex.nodes.input.InputLengthNode;
import com.oracle.truffle.regex.util.NumberConversion;

public abstract class RegexNode extends RootNode {

    private static final FrameDescriptor SHARED_EMPTY_FRAMEDESCRIPTOR = new FrameDescriptor();

    @Child private InputLengthNode inputLengthNode = InputLengthNode.create();
    @Child private InputCharAtNode inputCharAtNode = InputCharAtNode.create();

    public RegexNode() {
        super(null, SHARED_EMPTY_FRAMEDESCRIPTOR);
    }

    @Override
    public final Object execute(VirtualFrame frame) {
        Object[] args = frame.getArguments();
        assert args.length == 3;

        RegexCompiledRegex regex = (RegexCompiledRegex) args[0];
        Object input = args[1];
        int fromIndex = NumberConversion.intValue((Number) args[2]);

        if (regex.getSource().getFlags().isUnicode() && fromIndex > 0 && fromIndex < inputLengthNode.execute(input)) {
            if (Character.isLowSurrogate(inputCharAtNode.execute(input, fromIndex)) &&
                            Character.isHighSurrogate(inputCharAtNode.execute(input, fromIndex - 1))) {
                fromIndex = fromIndex - 1;
            }
        }

        return execute(regex, input, fromIndex);
    }

    protected abstract RegexResult execute(RegexCompiledRegex regex, Object input, int fromIndex);

    @Override
    @TruffleBoundary
    public final String toString() {
        return getEngineLabel() + ": " + getPatternSource();
    }

    protected abstract String getEngineLabel();

    protected abstract String getPatternSource();
}
