/*
 * Copyright (c) 2012, 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.regex;

import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.regex.result.RegexResult;
import com.oracle.truffle.regex.tregex.nodes.input.InputCharAtNode;
import com.oracle.truffle.regex.tregex.nodes.input.InputLengthNode;
import com.oracle.truffle.regex.util.NumberConversion;

public abstract class RegexExecRootNode extends RegexRootNode {

    @Child private InputLengthNode inputLengthNode = InputLengthNode.create();
    @Child private InputCharAtNode inputCharAtNode = InputCharAtNode.create();

    public RegexExecRootNode(RegexLanguage language, FrameDescriptor frameDescriptor, RegexSource source) {
        super(language, frameDescriptor, source);
    }

    public RegexExecRootNode(RegexLanguage language, RegexSource source) {
        super(language, source);
    }

    @Override
    public final RegexResult execute(VirtualFrame frame) {
        Object[] args = frame.getArguments();
        assert args.length == 3;

        RegexObject regex = (RegexObject) args[0];
        Object input = args[1];
        int fromIndex = NumberConversion.intValue((Number) args[2]);
        if (sourceIsUnicode(regex) && fromIndex > 0 && fromIndex < inputLengthNode.execute(input)) {
            if (Character.isLowSurrogate(inputCharAtNode.execute(input, fromIndex)) &&
                            Character.isHighSurrogate(inputCharAtNode.execute(input, fromIndex - 1))) {
                fromIndex = fromIndex - 1;
            }
        }

        return execute(frame, regex, input, fromIndex);
    }

    protected abstract RegexResult execute(VirtualFrame frame, RegexObject regex, Object input, int fromIndex);

    @SuppressWarnings("unused")
    protected boolean sourceIsUnicode(RegexObject regex) {
        return getSource().getFlags().isUnicode();
    }
}
