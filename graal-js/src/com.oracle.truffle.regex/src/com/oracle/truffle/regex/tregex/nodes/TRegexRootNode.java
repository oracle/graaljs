/*
 * Copyright (c) 2016, 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.regex.tregex.nodes;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.regex.RegexCompiledRegex;
import com.oracle.truffle.regex.RegexProfile;
import com.oracle.truffle.regex.RegexSource;
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

    protected final RegexSource source;

    @Child InputLengthNode inputLengthNode = InputLengthNode.create();
    @Child InputCharAtNode inputCharAtNode = InputCharAtNode.create();

    public TRegexRootNode(FrameDescriptor frameDescriptor, RegexSource source) {
        super(null, frameDescriptor);
        this.source = source;
    }

    public static class TRegexForwardSearchRootNode extends TRegexRootNode {

        @Child RunRegexSearchNode runRegexSearchNode;
        private final LazyCaptureGroupRegexSearchNode lazySearchNode;
        private final EagerCaptureGroupRegexSearchNode eagerSearchNode;

        public TRegexForwardSearchRootNode(RegexSource source,
                        PreCalculatedResultFactory[] preCalculatedResults,
                        TRegexDFAExecutorNode forwardExecutor,
                        TRegexDFAExecutorNode backwardExecutor,
                        TRegexDFAExecutorNode captureGroupExecutor,
                        TRegexDFAExecutorNode eagerCaptureGroupExecutor) {
            super(forwardExecutor.getProperties().getFrameDescriptor(), source);
            lazySearchNode = new LazyCaptureGroupRegexSearchNode(source, preCalculatedResults, forwardExecutor, backwardExecutor, captureGroupExecutor);
            if (eagerCaptureGroupExecutor == null) {
                eagerSearchNode = null;
            } else {
                eagerSearchNode = new EagerCaptureGroupRegexSearchNode(eagerCaptureGroupExecutor);
            }
            runRegexSearchNode = insert(lazySearchNode);
        }

        @Override
        public final RegexResult execute(VirtualFrame frame) {
            final Object[] args = frame.getArguments();
            assert args.length == 3;
            final RegexCompiledRegex regex = (RegexCompiledRegex) args[0];
            final Object input = args[1];
            int fromIndexArg = NumberConversion.intValue((Number) args[2]);

            if (source.getFlags().isUnicode() && fromIndexArg > 0 && fromIndexArg < inputLengthNode.execute(input)) {
                if (Character.isLowSurrogate(inputCharAtNode.execute(input, fromIndexArg)) &&
                                Character.isHighSurrogate(inputCharAtNode.execute(input, fromIndexArg - 1))) {
                    fromIndexArg = fromIndexArg - 1;
                }
            }
            final RegexResult result = runRegexSearchNode.run(frame, regex, input, fromIndexArg);
            if (CompilerDirectives.inInterpreter() && eagerSearchNode != null && runRegexSearchNode != eagerSearchNode) {
                RegexProfile profile = regex.getRegexProfile();
                if (profile.atEvaluationTripPoint() && profile.shouldUseEagerMatching()) {
                    runRegexSearchNode = insert(eagerSearchNode);
                }
                profile.incCalls();
                if (result != RegexResult.NO_MATCH) {
                    profile.incMatches();
                }
            }
            return result;
        }

        @Override
        @CompilerDirectives.TruffleBoundary
        public final String toString() {
            return "TRegex fwd " + source;
        }

        abstract static class RunRegexSearchNode extends Node {

            @Child InputLengthNode inputLengthNode = InputLengthNode.create();

            abstract RegexResult run(VirtualFrame frame, RegexCompiledRegex regex, Object input, int fromIndexArg);
        }

        static final class LazyCaptureGroupRegexSearchNode extends RunRegexSearchNode {

            private final RegexSource source;
            private final PreCalculatedResultFactory[] preCalculatedResults;

            @Child private TRegexDFAExecutorNode forwardExecutorNode;
            private final TRegexDFAExecutorNode backwardExecutorNode;
            private final TRegexDFAExecutorNode captureGroupExecutorNode;

            private final CallTarget backwardCallTarget;
            private final CallTarget captureGroupCallTarget;

            LazyCaptureGroupRegexSearchNode(RegexSource source,
                            PreCalculatedResultFactory[] preCalculatedResults,
                            TRegexDFAExecutorNode forwardNode,
                            TRegexDFAExecutorNode backwardNode,
                            TRegexDFAExecutorNode captureGroupExecutor) {
                this.forwardExecutorNode = forwardNode;
                this.source = source;
                this.preCalculatedResults = preCalculatedResults;
                this.backwardExecutorNode = backwardNode;
                backwardCallTarget = Truffle.getRuntime().createCallTarget(new TRegexBackwardSearchRootNode(source, backwardNode, forwardNode.getPrefixLength()));
                this.captureGroupExecutorNode = captureGroupExecutor;
                if (captureGroupExecutor == null) {
                    captureGroupCallTarget = null;
                } else {
                    captureGroupCallTarget = Truffle.getRuntime().createCallTarget(new TRegexLazyCaptureGroupsRootNode(source, captureGroupExecutor));
                }
            }

            @Override
            RegexResult run(VirtualFrame frame, RegexCompiledRegex regex, Object input, int fromIndexArg) {
                if (backwardExecutorNode.isAnchored()) {
                    return executeBackwardAnchored(frame, regex, input, fromIndexArg);
                } else {
                    return executeForward(frame, regex, input, fromIndexArg);
                }
            }

            private RegexResult executeForward(VirtualFrame frame, RegexCompiledRegex regex, Object input, int fromIndexArg) {
                forwardExecutorNode.setInput(frame, input);
                forwardExecutorNode.setFromIndex(frame, fromIndexArg);
                forwardExecutorNode.setIndex(frame, fromIndexArg);
                forwardExecutorNode.setMaxIndex(frame, inputLengthNode.execute(input));
                forwardExecutorNode.execute(frame);
                final int end = forwardExecutorNode.getResultInt(frame);
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
                    if (forwardExecutorNode.isAnchored() || source.getFlags().isSticky()) {
                        return new SingleResult(regex, input, fromIndexArg, end);
                    }
                    return new SingleResultLazyStart(regex, input, fromIndexArg, end, backwardCallTarget);
                } else {
                    if (preCalculatedResults != null) { // traceFinder
                        return new TraceFinderResult(regex, input, fromIndexArg, end, backwardCallTarget, preCalculatedResults);
                    } else {
                        if (forwardExecutorNode.isAnchored() || (source.getFlags().isSticky() && forwardExecutorNode.getPrefixLength() == 0)) {
                            return new LazyCaptureGroupsResult(regex, input, fromIndexArg, end, captureGroupExecutorNode.getNumberOfCaptureGroups(), null, captureGroupCallTarget);
                        }
                        return new LazyCaptureGroupsResult(regex, input, fromIndexArg, end, captureGroupExecutorNode.getNumberOfCaptureGroups(), backwardCallTarget, captureGroupCallTarget);
                    }
                }
            }

            private RegexResult executeBackwardAnchored(VirtualFrame frame, RegexCompiledRegex regex, Object input, int fromIndexArg) {
                final int inputLength = inputLengthNode.execute(input);
                backwardExecutorNode.setInput(frame, input);
                backwardExecutorNode.setFromIndex(frame, 0);
                backwardExecutorNode.setIndex(frame, inputLength - 1);
                backwardExecutorNode.setMaxIndex(frame, Math.max(-1, fromIndexArg - 1 - forwardExecutorNode.getPrefixLength()));
                backwardExecutorNode.execute(frame);
                final int backwardResult = backwardExecutorNode.getResultInt(frame);
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
        }

        static final class EagerCaptureGroupRegexSearchNode extends RunRegexSearchNode {

            @Child private TRegexDFAExecutorNode executorNode;

            EagerCaptureGroupRegexSearchNode(TRegexDFAExecutorNode executorNode) {
                this.executorNode = executorNode;
            }

            @Override
            RegexResult run(VirtualFrame frame, RegexCompiledRegex regex, Object input, int fromIndexArg) {
                executorNode.setInput(frame, input);
                executorNode.setFromIndex(frame, fromIndexArg);
                executorNode.setIndex(frame, fromIndexArg);
                executorNode.setMaxIndex(frame, inputLengthNode.execute(input));
                executorNode.execute(frame);
                final int[] resultArray = executorNode.getResultCaptureGroups(frame);
                if (resultArray == null) {
                    return RegexResult.NO_MATCH;
                }
                return new LazyCaptureGroupsResult(regex, input, resultArray);
            }
        }
    }

    public static class TRegexBackwardSearchRootNode extends TRegexRootNode {

        @Child TRegexDFAExecutorNode executorNode;
        private final int prefixLength;

        public TRegexBackwardSearchRootNode(RegexSource source, TRegexDFAExecutorNode backwardNode, int prefixLength) {
            super(backwardNode.getProperties().getFrameDescriptor(), source);
            this.executorNode = backwardNode;
            this.prefixLength = prefixLength;
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
        @CompilerDirectives.TruffleBoundary
        public final String toString() {
            return "TRegex bck " + source;
        }
    }

    public static class TRegexLazyCaptureGroupsRootNode extends TRegexRootNode {

        @Child TRegexDFAExecutorNode executorNode;

        public TRegexLazyCaptureGroupsRootNode(RegexSource source, TRegexDFAExecutorNode captureGroupNode) {
            super(captureGroupNode.getProperties().getFrameDescriptor(), source);
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
        @CompilerDirectives.TruffleBoundary
        public final String toString() {
            return "TRegex cg " + source;
        }
    }
}
