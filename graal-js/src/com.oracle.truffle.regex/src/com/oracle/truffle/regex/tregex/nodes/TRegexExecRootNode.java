/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The Universal Permissive License (UPL), Version 1.0
 *
 * Subject to the condition set forth below, permission is hereby granted to any
 * person obtaining a copy of this software, associated documentation and/or
 * data (collectively the "Software"), free of charge and under any and all
 * copyright rights in the Software, and any and all patent rights owned or
 * freely licensable by each licensor hereunder covering either (i) the
 * unmodified Software as contributed to or provided by such licensor, or (ii)
 * the Larger Works (as defined below), to deal in both
 *
 * (a) the Software, and
 *
 * (b) any piece of software and/or hardware listed in the lrgrwrks.txt file if
 * one is included with the Software each a "Larger Work" to which the Software
 * is contributed by such licensors),
 *
 * without restriction, including without limitation the rights to copy, create
 * derivative works of, display, perform, and distribute the Software and make,
 * use, sell, offer for sale, import, export, have made, and have sold the
 * Software and the Larger Work(s), and to sublicense the foregoing rights on
 * either these or other terms.
 *
 * This license is subject to the following condition:
 *
 * The above copyright notice and either this complete permission notice or at a
 * minimum a reference to the UPL must be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.oracle.truffle.regex.tregex.nodes;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.regex.CompiledRegex;
import com.oracle.truffle.regex.RegexExecRootNode;
import com.oracle.truffle.regex.RegexLanguage;
import com.oracle.truffle.regex.RegexObject;
import com.oracle.truffle.regex.RegexProfile;
import com.oracle.truffle.regex.RegexRootNode;
import com.oracle.truffle.regex.RegexSource;
import com.oracle.truffle.regex.UnsupportedRegexException;
import com.oracle.truffle.regex.result.LazyCaptureGroupsResult;
import com.oracle.truffle.regex.result.PreCalculatedResultFactory;
import com.oracle.truffle.regex.result.RegexResult;
import com.oracle.truffle.regex.result.SingleResult;
import com.oracle.truffle.regex.result.SingleResultLazyStart;
import com.oracle.truffle.regex.result.TraceFinderResult;
import com.oracle.truffle.regex.tregex.TRegexCompiler;
import com.oracle.truffle.regex.tregex.nodes.input.InputLengthNode;
import com.oracle.truffle.regex.tregex.util.DebugUtil;

import java.util.Arrays;

public class TRegexExecRootNode extends RegexExecRootNode implements CompiledRegex {

    private static final DebugUtil.DebugLogger LOG_BAILOUT = new DebugUtil.DebugLogger("TRegex Bailout: ", DebugUtil.LOG_BAILOUT_MESSAGES);

    private static final EagerCaptureGroupRegexSearchNode EAGER_SEARCH_BAILED_OUT = new EagerCaptureGroupRegexSearchNode(null);

    private final CallTarget regexCallTarget;
    private final LazyCaptureGroupRegexSearchNode lazySearchNode;
    private EagerCaptureGroupRegexSearchNode eagerSearchNode;
    private final TRegexCompiler tRegexCompiler;
    private final boolean eagerCompilation;

    @Child private RunRegexSearchNode runRegexSearchNode;

    public TRegexExecRootNode(RegexLanguage language,
                    TRegexCompiler tRegexCompiler,
                    RegexSource source,
                    boolean eagerCompilation,
                    PreCalculatedResultFactory[] preCalculatedResults,
                    TRegexDFAExecutorNode forwardExecutor,
                    TRegexDFAExecutorNode backwardExecutor,
                    TRegexDFAExecutorNode captureGroupExecutor) {
        super(language, source);
        lazySearchNode = new LazyCaptureGroupRegexSearchNode(language, source, preCalculatedResults, forwardExecutor, backwardExecutor, captureGroupExecutor);
        runRegexSearchNode = insert(lazySearchNode);
        regexCallTarget = Truffle.getRuntime().createCallTarget(new RegexRootNode(language, forwardExecutor.getProperties().getFrameDescriptor(), this));
        this.tRegexCompiler = tRegexCompiler;
        this.eagerCompilation = eagerCompilation;
        if (eagerCompilation && captureGroupExecutor != null) {
            compileEagerSearchNode();
        }
        if (DebugUtil.DEBUG_ALWAYS_EAGER) {
            switchToEagerSearch(null);
        }
    }

    @Override
    public final RegexResult execute(VirtualFrame frame, RegexObject regex, Object input, int fromIndex) {
        final RegexResult result = runRegexSearchNode.run(frame, regex, input, fromIndex);
        assert !eagerCompilation || eagerAndLazySearchNodesProduceSameResult(frame, regex, input, fromIndex, result);
        if (CompilerDirectives.inInterpreter() && runRegexSearchNode == lazySearchNode) {
            RegexProfile profile = regex.getRegexProfile();
            if (profile.atEvaluationTripPoint() && profile.shouldUseEagerMatching()) {
                switchToEagerSearch(profile);
            }
            profile.incCalls();
            if (result != RegexResult.NO_MATCH) {
                profile.incMatches();
            }
        }
        return result;
    }

    private void switchToEagerSearch(RegexProfile profile) {
        compileEagerSearchNode();
        if (eagerSearchNode != EAGER_SEARCH_BAILED_OUT) {
            if (DebugUtil.LOG_SWITCH_TO_EAGER) {
                System.out.println("regex " + getSource() + ": switching to eager matching." + (profile == null ? "" : "profile: " + profile));
            }
            runRegexSearchNode = insert(eagerSearchNode);
        }
    }

    private boolean eagerAndLazySearchNodesProduceSameResult(VirtualFrame frame, RegexObject regex, Object input, int fromIndex, RegexResult resultOfCurrentSearchNode) {
        if (lazySearchNode.captureGroupExecutorNode == null || eagerSearchNode == EAGER_SEARCH_BAILED_OUT) {
            return true;
        }
        RegexResult lazyResult;
        RegexResult eagerResult;
        if (runRegexSearchNode == lazySearchNode) {
            lazyResult = resultOfCurrentSearchNode;
            eagerResult = eagerSearchNode.run(frame, regex, input, fromIndex);
        } else {
            lazyResult = lazySearchNode.run(frame, regex, input, fromIndex);
            eagerResult = resultOfCurrentSearchNode;
        }
        if (lazyResult == RegexResult.NO_MATCH) {
            return eagerResult == RegexResult.NO_MATCH;
        }
        LazyCaptureGroupsResult lazyResultC = (LazyCaptureGroupsResult) lazyResult;
        LazyCaptureGroupsResult eagerResultC = (LazyCaptureGroupsResult) eagerResult;
        if (lazyResultC.getFindStartCallTarget() == null) {
            lazyResultC.getCaptureGroupCallTarget().call(lazyResultC.createArgsCGNoFindStart());
        } else {
            lazyResultC.getCaptureGroupCallTarget().call(lazyResultC.createArgsCG((int) lazyResultC.getFindStartCallTarget().call(lazyResultC.createArgsFindStart())));
        }
        boolean equal = Arrays.equals(lazyResultC.getResult(), eagerResultC.getResult());
        if (!equal) {
            System.out.println("ERROR:");
            System.out.println("Regex: " + getSource());
            System.out.println("Input: " + input);
            System.out.println("fromIndex: " + fromIndex);
            System.out.println("Lazy  Result: " + Arrays.toString(lazyResultC.getResult()));
            System.out.println("Eager Result: " + Arrays.toString(eagerResultC.getResult()));
        }
        return equal;
    }

    private void compileEagerSearchNode() {
        if (eagerSearchNode == null) {
            try {
                TRegexDFAExecutorNode executorNode = tRegexCompiler.compileEagerDFAExecutor(getSource());
                eagerSearchNode = new EagerCaptureGroupRegexSearchNode(executorNode);
            } catch (UnsupportedRegexException e) {
                LOG_BAILOUT.log(e.getMessage() + ": " + source);
                eagerSearchNode = EAGER_SEARCH_BAILED_OUT;
            }
        }
    }

    @Override
    public CallTarget getRegexCallTarget() {
        return regexCallTarget;
    }

    @Override
    public final String getEngineLabel() {
        return "TRegex fwd";
    }

    abstract static class RunRegexSearchNode extends Node {

        @Child InputLengthNode inputLengthNode = InputLengthNode.create();

        abstract RegexResult run(VirtualFrame frame, RegexObject regex, Object input, int fromIndexArg);
    }

    static final class LazyCaptureGroupRegexSearchNode extends RunRegexSearchNode {

        private final RegexSource source;
        private final PreCalculatedResultFactory[] preCalculatedResults;

        @Child private TRegexDFAExecutorNode forwardExecutorNode;
        private final TRegexDFAExecutorNode backwardExecutorNode;
        private final TRegexDFAExecutorNode captureGroupExecutorNode;

        private final CallTarget backwardCallTarget;
        private final CallTarget captureGroupCallTarget;

        LazyCaptureGroupRegexSearchNode(RegexLanguage language,
                        RegexSource source,
                        PreCalculatedResultFactory[] preCalculatedResults,
                        TRegexDFAExecutorNode forwardNode,
                        TRegexDFAExecutorNode backwardNode,
                        TRegexDFAExecutorNode captureGroupExecutor) {
            this.forwardExecutorNode = forwardNode;
            this.source = source;
            this.preCalculatedResults = preCalculatedResults;
            this.backwardExecutorNode = backwardNode;
            if (backwardNode == null) {
                assert singlePreCalcResult();
                backwardCallTarget = null;
            } else {
                backwardCallTarget = Truffle.getRuntime().createCallTarget(new RegexRootNode(language, backwardNode.getProperties().getFrameDescriptor(),
                                new TRegexLazyFindStartRootNode(language, source, forwardNode.getPrefixLength(), backwardNode)));
            }
            this.captureGroupExecutorNode = captureGroupExecutor;
            if (captureGroupExecutor == null) {
                captureGroupCallTarget = null;
            } else {
                captureGroupCallTarget = Truffle.getRuntime().createCallTarget(new RegexRootNode(language, captureGroupExecutor.getProperties().getFrameDescriptor(),
                                new TRegexLazyCaptureGroupsRootNode(language, source, captureGroupExecutor)));
            }
        }

        @Override
        RegexResult run(VirtualFrame frame, RegexObject regex, Object input, int fromIndexArg) {
            if (backwardExecutorNode != null && backwardExecutorNode.isAnchored()) {
                return executeBackwardAnchored(frame, regex, input, fromIndexArg);
            } else {
                return executeForward(frame, regex, input, fromIndexArg);
            }
        }

        private RegexResult executeForward(VirtualFrame frame, RegexObject regex, Object input, int fromIndexArg) {
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

        private RegexResult executeBackwardAnchored(VirtualFrame frame, RegexObject regex, Object input, int fromIndexArg) {
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
        RegexResult run(VirtualFrame frame, RegexObject regex, Object input, int fromIndexArg) {
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
