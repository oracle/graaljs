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

public abstract class RegexRootNode extends RootNode {

    private static final FrameDescriptor SHARED_EMPTY_FRAMEDESCRIPTOR = new FrameDescriptor();

    private final RegexSource source;
    @Child private InputLengthNode inputLengthNode = InputLengthNode.create();
    @Child private InputCharAtNode inputCharAtNode = InputCharAtNode.create();

    public RegexRootNode(RegexLanguage language, FrameDescriptor frameDescriptor, RegexSource source) {
        super(language, frameDescriptor);
        this.source = source;
    }

    public RegexRootNode(RegexLanguage language, RegexSource source) {
        this(language, SHARED_EMPTY_FRAMEDESCRIPTOR, source);
    }

    public RegexSource getSource() {
        return source;
    }

    @Override
    public final RegexResult execute(VirtualFrame frame) {
        Object[] args = frame.getArguments();
        assert args.length == 3;

        RegexObject regex = (RegexObject) args[0];
        Object input = args[1];
        int fromIndex = NumberConversion.intValue((Number) args[2]);
        if ((source == null ? regex.getSource() : source).getFlags().isUnicode() && fromIndex > 0 && fromIndex < inputLengthNode.execute(input)) {
            if (Character.isLowSurrogate(inputCharAtNode.execute(input, fromIndex)) &&
                            Character.isHighSurrogate(inputCharAtNode.execute(input, fromIndex - 1))) {
                fromIndex = fromIndex - 1;
            }
        }

        return execute(frame, regex, input, fromIndex);
    }

    protected abstract RegexResult execute(VirtualFrame frame, RegexObject regex, Object input, int fromIndex);

    @Override
    @TruffleBoundary
    public final String toString() {
        return "regex " + getEngineLabel() + ": " + source;
    }

    protected abstract String getEngineLabel();
}
