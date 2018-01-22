/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.regex.tregex.nodes;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.regex.RegexLanguage;
import com.oracle.truffle.regex.RegexProfile;
import com.oracle.truffle.regex.RegexRootNode;
import com.oracle.truffle.regex.RegexSource;
import com.oracle.truffle.regex.result.LazyCaptureGroupsResult;

public class TRegexLazyCaptureGroupsRootNode extends RegexRootNode {

    @Child private TRegexDFAExecutorNode executorNode;

    public TRegexLazyCaptureGroupsRootNode(RegexLanguage language, RegexSource source, TRegexDFAExecutorNode captureGroupNode) {
        super(language, captureGroupNode.getProperties().getFrameDescriptor(), source);
        this.executorNode = captureGroupNode;
    }

    @Override
    public final int[] execute(VirtualFrame frame) {
        final Object[] args = frame.getArguments();
        assert args.length == 3;
        final LazyCaptureGroupsResult receiver = (LazyCaptureGroupsResult) args[0];
        final int startIndex = (int) args[1];
        final int max = (int) args[2];
        executorNode.setInput(frame, receiver.getInput());
        executorNode.setFromIndex(frame, receiver.getFromIndex());
        executorNode.setIndex(frame, startIndex);
        executorNode.setMaxIndex(frame, max);
        executorNode.execute(frame);
        final int[] result = executorNode.getResultCaptureGroups(frame);
        if (CompilerDirectives.inInterpreter()) {
            RegexProfile profile = receiver.getCompiledRegex().getRegexProfile();
            profile.incCaptureGroupAccesses();
            profile.addMatchedPortionOfSearchSpace((double) (result[1] - result[0]) / (result[1] - (receiver.getFromIndex() + 1)));
        }
        receiver.setResult(result);
        return result;
    }

    @Override
    protected String getEngineLabel() {
        return "TRegex cg";
    }
}
