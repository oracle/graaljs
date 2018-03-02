/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.regex.tregex.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.regex.RegexBodyNode;
import com.oracle.truffle.regex.RegexLanguage;
import com.oracle.truffle.regex.RegexSource;

public class TRegexLazyFindStartRootNode extends RegexBodyNode {

    private final int prefixLength;
    @Child private TRegexDFAExecutorNode executorNode;

    public TRegexLazyFindStartRootNode(RegexLanguage language, RegexSource source, int prefixLength, TRegexDFAExecutorNode backwardNode) {
        super(language, source);
        this.prefixLength = prefixLength;
        this.executorNode = backwardNode;
    }

    @Override
    public final Object execute(VirtualFrame frame) {
        final Object[] args = frame.getArguments();
        assert args.length == 3;
        final Object input = args[0];
        final int fromIndexArg = (int) args[1];
        final int max = (int) args[2];
        executorNode.setInput(frame, input);
        executorNode.setIndex(frame, fromIndexArg);
        executorNode.setFromIndex(frame, max);
        executorNode.setMaxIndex(frame, Math.max(-1, max - 1 - prefixLength));
        executorNode.execute(frame);
        return executorNode.getResultInt(frame);
    }

    @Override
    public String getEngineLabel() {
        return "TRegex bck";
    }
}
