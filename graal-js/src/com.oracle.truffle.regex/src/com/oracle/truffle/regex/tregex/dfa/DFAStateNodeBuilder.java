/*
 * Copyright (c) 2017, 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
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
