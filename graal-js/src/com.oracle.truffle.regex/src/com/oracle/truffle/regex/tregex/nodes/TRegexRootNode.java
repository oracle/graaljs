/*
 * Copyright (c) 2016, 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.regex.tregex.nodes;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameUtil;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.regex.RegexCompiledRegex;
import com.oracle.truffle.regex.result.LazyCaptureGroupsResult;
import com.oracle.truffle.regex.result.PreCalculatedResultFactory;
import com.oracle.truffle.regex.result.RegexResult;
import com.oracle.truffle.regex.result.SingleResult;
import com.oracle.truffle.regex.result.SingleResultLazyStart;
import com.oracle.truffle.regex.result.TraceFinderResult;
import com.oracle.truffle.regex.tregex.nodes.input.InputCharAtNode;
import com.oracle.truffle.regex.tregex.nodes.input.InputLengthNode;
import com.oracle.truffle.regex.util.NumberConversion;

public abstract class TRegexRootNode extends RootNode {

    protected final FrameSlot inputSeq;
    protected final FrameSlot fromIndex;
    protected final FrameSlot index;
    protected final FrameSlot maxIndex;
    protected final FrameSlot result;

    protected final String patternSource;

    @Child InputLengthNode inputLengthNode = InputLengthNode.create();
    @Child InputCharAtNode inputCharAtNode = InputCharAtNode.create();
    @Child protected TRegexDFAExecutorNode executorNode;

    public TRegexRootNode(FrameDescriptor frameDescriptor,
                    FrameSlot inputSeq,
                    FrameSlot fromIndex,
                    FrameSlot index,
                    FrameSlot maxIndex,
                    FrameSlot result,
                    String patternSource,
                    TRegexDFAExecutorNode executorNode) {
        super(null, frameDescriptor);
        this.inputSeq = inputSeq;
        this.fromIndex = fromIndex;
        this.index = index;
        this.maxIndex = maxIndex;
        this.result = result;
        this.patternSource = patternSource;
        this.executorNode = executorNode;
    }

    public static class TRegexForwardSearchRootNode extends TRegexRootNode {

        private final boolean unicodePattern;
        private final PreCalculatedResultFactory[] preCalculatedResults;
        private final TRegexDFAExecutorNode backwardExecutorNode;
        private final CallTarget backwardCallTarget;
        private final TRegexDFAExecutorNode captureGroupExecutorNode;
        private final CallTarget captureGroupCallTarget;

        public TRegexForwardSearchRootNode(FrameDescriptor frameDescriptor,
                        FrameSlot inputSeq,
                        FrameSlot initialIndex,
                        FrameSlot index,
                        FrameSlot maxIndex,
                        FrameSlot result,
                        FrameSlot captureGroupResult,
                        String patternSource,
                        boolean unicodePattern,
                        PreCalculatedResultFactory[] preCalculatedResults,
                        TRegexDFAExecutorNode forwardNode,
                        TRegexDFAExecutorNode backwardNode,
                        TRegexDFAExecutorNode captureGroupExecutor) {
            super(frameDescriptor, inputSeq, initialIndex, index, maxIndex, result, patternSource, forwardNode);
            this.unicodePattern = unicodePattern;
            this.preCalculatedResults = preCalculatedResults;
            this.backwardExecutorNode = backwardNode;
            backwardCallTarget = Truffle.getRuntime().createCallTarget(new TRegexBackwardSearchRootNode(
                            frameDescriptor, inputSeq, initialIndex, index, maxIndex, result, patternSource, backwardNode, forwardNode.getPrefixLength()));
            this.captureGroupExecutorNode = captureGroupExecutor;
            if (captureGroupExecutor == null) {
                captureGroupCallTarget = null;
            } else {
                captureGroupCallTarget = Truffle.getRuntime().createCallTarget(new TRegexCaptureGroupRootNode(
                                frameDescriptor, inputSeq, initialIndex, index, maxIndex, captureGroupResult, patternSource, captureGroupExecutor));
            }
        }

        @Override
        public final Object execute(VirtualFrame frame) {
            final Object[] args = frame.getArguments();
            assert args.length == 3;
            final RegexCompiledRegex regex = (RegexCompiledRegex) args[0];
            final Object input = args[1];
            int fromIndexArg = NumberConversion.intValue((Number) args[2]);

            if (unicodePattern && fromIndexArg > 0 && fromIndexArg < inputLengthNode.execute(input)) {
                if (Character.isLowSurrogate(inputCharAtNode.execute(input, fromIndexArg)) &&
                                Character.isHighSurrogate(inputCharAtNode.execute(input, fromIndexArg - 1))) {
                    fromIndexArg = fromIndexArg - 1;
                }
            }

            if (!backwardExecutorNode.hasUnAnchoredEntry()) {
                return executeBackwardAnchored(frame, regex, input, fromIndexArg);
            } else {
                return executeForward(frame, regex, input, fromIndexArg);
            }
        }

        private Object executeForward(VirtualFrame frame, RegexCompiledRegex regex, Object input, int fromIndexArg) {
            frame.setObject(inputSeq, input);
            frame.setInt(fromIndex, fromIndexArg);
            frame.setInt(index, fromIndexArg);
            frame.setInt(maxIndex, inputLengthNode.execute(input));

            executorNode.execute(frame);
            final int end = FrameUtil.getIntSafe(frame, result);
            if (end == TRegexDFAExecutorNode.NO_MATCH) {
                return RegexResult.NO_MATCH;
            }
            if (singlePreCalcResult()) {
                return preCalculatedResults[0].createFromEnd(regex, input, end);
            }
            if (preCalculatedResults == null && captureGroupExecutorNode == null) {
                if (end == fromIndexArg) { // zero-length match
                    return new SingleResult(regex, input, end, end);
                }
                if (!executorNode.hasUnAnchoredEntry()) {
                    return new SingleResult(regex, input, 0, end);
                }
                return new SingleResultLazyStart(regex, input, fromIndexArg, end, backwardCallTarget);
            } else {
                if (preCalculatedResults != null) { // traceFinder
                    return new TraceFinderResult(regex, input, fromIndexArg, end, backwardCallTarget, preCalculatedResults);
                } else {
                    if (!executorNode.hasUnAnchoredEntry()) {
                        return new LazyCaptureGroupsResult(regex, input, 0, end, captureGroupExecutorNode.getNumberOfCaptureGroups(), null, captureGroupCallTarget);
                    }
                    return new LazyCaptureGroupsResult(regex, input, fromIndexArg, end, captureGroupExecutorNode.getNumberOfCaptureGroups(), backwardCallTarget, captureGroupCallTarget);
                }
            }
        }

        private Object executeBackwardAnchored(VirtualFrame frame, RegexCompiledRegex regex, Object input, int fromIndexArg) {
            final int inputLength = inputLengthNode.execute(input);
            frame.setObject(inputSeq, input);
            frame.setInt(fromIndex, 0);
            frame.setInt(index, inputLength - 1);
            frame.setInt(maxIndex, Math.max(-1, fromIndexArg - 1 - executorNode.getPrefixLength()));

            backwardExecutorNode.execute(frame);
            final int backwardResult = FrameUtil.getIntSafe(frame, result);
            if (backwardResult == TRegexDFAExecutorNode.NO_MATCH) {
                return RegexResult.NO_MATCH;
            }
            if (multiplePreCalcResults()) { // traceFinder
                return preCalculatedResults[backwardResult].createFromEnd(regex, input, inputLength);
            }
            final int start = backwardResult + 1;
            if (singlePreCalcResult()) {
                return preCalculatedResults[0].createFromStart(regex, input, start);
            }
            if (captureGroupExecutorNode != null) {
                return new LazyCaptureGroupsResult(regex, input, start, inputLength, captureGroupExecutorNode.getNumberOfCaptureGroups(), null, captureGroupCallTarget);
            }
            return new SingleResult(regex, input, start, inputLength);
        }

        private boolean singlePreCalcResult() {
            return preCalculatedResults != null && preCalculatedResults.length == 1;
        }

        private boolean multiplePreCalcResults() {
            return preCalculatedResults != null && preCalculatedResults.length > 1;
        }

        @Override
        @CompilerDirectives.TruffleBoundary
        public final String toString() {
            return "TRegex fwd " + patternSource;
        }

    }

    public static class TRegexBackwardSearchRootNode extends TRegexRootNode {

        private final int prefixLength;

        public TRegexBackwardSearchRootNode(FrameDescriptor frameDescriptor,
                        FrameSlot inputSeq,
                        FrameSlot initialIndex,
                        FrameSlot index,
                        FrameSlot maxIndex,
                        FrameSlot result,
                        String patternSource,
                        TRegexDFAExecutorNode backwardNode,
                        int prefixLength) {
            super(frameDescriptor, inputSeq, initialIndex, index, maxIndex, result, patternSource, backwardNode);
            this.prefixLength = prefixLength;
        }

        @Override
        public final Object execute(VirtualFrame frame) {
            final Object[] args = frame.getArguments();
            assert args.length == 3;
            final Object input = args[0];
            frame.setObject(inputSeq, input);
            final int fromIndexArg = (int) args[1];
            frame.setInt(index, fromIndexArg);
            final int max = (int) args[2];
            frame.setInt(fromIndex, max);
            frame.setInt(maxIndex, Math.max(-1, max - 1 - prefixLength));
            executorNode.execute(frame);
            return FrameUtil.getIntSafe(frame, result);
        }

        @Override
        @CompilerDirectives.TruffleBoundary
        public final String toString() {
            return "TRegex bck " + patternSource;
        }
    }

    public static class TRegexCaptureGroupRootNode extends TRegexRootNode {

        public TRegexCaptureGroupRootNode(FrameDescriptor frameDescriptor,
                        FrameSlot inputSeq,
                        FrameSlot initialIndex,
                        FrameSlot index,
                        FrameSlot maxIndex,
                        FrameSlot captureGroupResult,
                        String patternSource,
                        TRegexDFAExecutorNode captureGroupNode) {
            super(frameDescriptor, inputSeq, initialIndex, index, maxIndex, captureGroupResult, patternSource, captureGroupNode);
        }

        @Override
        public final Object execute(VirtualFrame frame) {
            final Object[] args = frame.getArguments();
            assert args.length == 4;
            final Object input = args[0];
            frame.setObject(inputSeq, input);
            final int fromIndexArg = (int) args[1];
            frame.setInt(fromIndex, fromIndexArg);
            final int startIndex = (int) args[2];
            frame.setInt(index, startIndex);
            final int max = (int) args[3];
            frame.setInt(maxIndex, max);
            executorNode.execute(frame);
            return FrameUtil.getObjectSafe(frame, result);
        }

        @Override
        @CompilerDirectives.TruffleBoundary
        public final String toString() {
            return "TRegex cg " + patternSource;
        }
    }
}
