/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.regex;

import com.oracle.truffle.api.CompilerDirectives;

/**
 * This profile tracks how
 */
public final class RegexProfile {

    private int calls = 0;
    private int matches = 0;
    private int captureGroupAccesses = 0;
    private double avgMatchedPortionOfSearchSpace = 0;

    private boolean atOverflow() {
        return calls == Integer.MAX_VALUE;
    }

    public void incCalls() {
        if (atOverflow()) {
            return;
        }
        calls++;
    }

    public void incMatches() {
        if (atOverflow()) {
            return;
        }
        matches++;
        assert matches <= calls;
    }

    public void incCaptureGroupAccesses() {
        if (atOverflow()) {
            return;
        }
        captureGroupAccesses++;
        assert captureGroupAccesses <= matches;
    }

    public void addMatchedPortionOfSearchSpace(double matchedPortion) {
        if (atOverflow()) {
            return;
        }
        assert captureGroupAccesses > 0;
        avgMatchedPortionOfSearchSpace += (matchedPortion - avgMatchedPortionOfSearchSpace) / captureGroupAccesses;
    }

    public boolean atEvaluationTripPoint() {
        // evaluate profile after every 4096 calls
        return calls > 0 && (calls & 0xfff) == 0;
    }

    private double matchRatio() {
        return (double) matches / calls;
    }

    private double cgAccessRatio() {
        return (double) captureGroupAccesses / matches;
    }

    public boolean shouldUseEagerMatching() {
        return matchRatio() > 0.5 && cgAccessRatio() > 0.5 && avgMatchedPortionOfSearchSpace > 0.4;
    }

    @CompilerDirectives.TruffleBoundary
    @Override
    public String toString() {
        return String.format("calls: %d, matches: %d (%.2f%%), cg accesses: %d (%.2f%%), avg matched portion of search space: %.2f%%",
                        calls, matches, matchRatio() * 100, captureGroupAccesses, cgAccessRatio() * 100, avgMatchedPortionOfSearchSpace * 100);
    }
}
