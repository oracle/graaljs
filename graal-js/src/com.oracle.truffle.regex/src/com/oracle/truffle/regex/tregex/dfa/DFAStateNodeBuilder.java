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
package com.oracle.truffle.regex.tregex.dfa;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.regex.tregex.matchers.MatcherBuilder;
import com.oracle.truffle.regex.tregex.nodes.TraceFinderDFAStateNode;
import com.oracle.truffle.regex.tregex.util.DebugUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class DFAStateNodeBuilder {

    private final short id;
    private final NFATransitionSet nfaStateSet;
    private byte unAnchoredResult;
    private boolean overrideFinalState = false;
    private DFAStateNodeBuilder[] successors;
    private MatcherBuilder[] matcherBuilders;
    private DFACaptureGroupTransitionBuilder[] captureGroupTransitions;
    private boolean isFinalStateSuccessor = false;
    private List<DFACaptureGroupTransitionBuilder> precedingTransitions;
    private short backwardPrefixState = -1;

    DFAStateNodeBuilder(short id, NFATransitionSet nfaStateSet) {
        this.id = id;
        this.nfaStateSet = nfaStateSet;
        this.unAnchoredResult = nfaStateSet.getPreCalculatedUnAnchoredResult();
    }

    public short getId() {
        return id;
    }

    public NFATransitionSet getNfaStateSet() {
        return nfaStateSet;
    }

    public boolean isFinalState() {
        return nfaStateSet.containsFinalState() || overrideFinalState;
    }

    public void setOverrideFinalState(boolean overrideFinalState) {
        this.overrideFinalState = overrideFinalState;
    }

    public boolean isAnchoredFinalState() {
        return nfaStateSet.containsAnchoredFinalState();
    }

    public DFAStateNodeBuilder[] getSuccessors() {
        return successors;
    }

    public int getNumberOfSuccessors() {
        return successors.length + (hasBackwardPrefixState() ? 1 : 0);
    }

    public boolean hasBackwardPrefixState() {
        return backwardPrefixState >= 0;
    }

    public void setSuccessors(DFAStateNodeBuilder[] successors) {
        this.successors = successors;
    }

    public MatcherBuilder[] getMatcherBuilders() {
        return matcherBuilders;
    }

    public void setMatcherBuilders(MatcherBuilder[] matcherBuilders) {
        this.matcherBuilders = matcherBuilders;
    }

    public DFACaptureGroupTransitionBuilder[] getCaptureGroupTransitions() {
        return captureGroupTransitions;
    }

    public void setCaptureGroupTransitions(DFACaptureGroupTransitionBuilder[] captureGroupTransitions) {
        this.captureGroupTransitions = captureGroupTransitions;
    }

    /**
     * Used in pruneUnambiguousPaths mode. States that are NOT final states or successors of final
     * states may have their last matcher replaced with an AnyMatcher.
     */
    public boolean isFinalStateSuccessor() {
        return isFinalStateSuccessor;
    }

    public void setFinalStateSuccessor() {
        isFinalStateSuccessor = true;
    }

    public byte getUnAnchoredResult() {
        return unAnchoredResult;
    }

    public void setUnAnchoredResult(byte unAnchoredResult) {
        this.unAnchoredResult = unAnchoredResult;
    }

    public byte getAnchoredResult() {
        return nfaStateSet.getPreCalculatedAnchoredResult();
    }

    public void addPrecedingTransition(DFACaptureGroupTransitionBuilder transitionBuilder) {
        if (precedingTransitions == null) {
            precedingTransitions = new ArrayList<>();
        }
        precedingTransitions.add(transitionBuilder);
    }

    public List<DFACaptureGroupTransitionBuilder> getPrecedingTransitions() {
        if (precedingTransitions == null) {
            return Collections.emptyList();
        }
        return precedingTransitions;
    }

    public short getBackwardPrefixState() {
        return backwardPrefixState;
    }

    public void setBackwardPrefixState(short backwardPrefixState) {
        this.backwardPrefixState = backwardPrefixState;
    }

    public String stateSetToString() {
        StringBuilder sb = new StringBuilder(nfaStateSet.toString());
        if (unAnchoredResult != TraceFinderDFAStateNode.NO_PRE_CALC_RESULT) {
            sb.append("_r").append(unAnchoredResult);
        }
        if (getAnchoredResult() != TraceFinderDFAStateNode.NO_PRE_CALC_RESULT) {
            sb.append("_rA").append(getAnchoredResult());
        }
        return sb.toString();
    }

    @Override
    @CompilerDirectives.TruffleBoundary
    public String toString() {
        StringBuilder sb = new StringBuilder();
        return DebugUtil.appendNodeId(sb, id).append(": ").append(stateSetToString()).toString();
    }

    public DebugUtil.Table toTable() {
        DebugUtil.Table table = new DebugUtil.Table("DFAState",
                        new DebugUtil.Value("stateSet", stateSetToString()),
                        new DebugUtil.Value("finalState", isFinalState()),
                        new DebugUtil.Value("anchoredFinalState", isAnchoredFinalState()));
        if (successors != null) {
            for (int i = 0; i < successors.length; i++) {
                DebugUtil.Table transition = new DebugUtil.Table("Transition",
                                new DebugUtil.Value("target", successors[i].stateSetToString()),
                                new DebugUtil.Value("matcher", matcherBuilders[i]));
                if (captureGroupTransitions != null) {
                    transition.append(captureGroupTransitions[i].toTable());
                }
                table.append(transition);
            }
        }
        return table;
    }
}
