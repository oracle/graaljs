/*
 * Copyright (c) 2016, 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.regex.tregex.parser;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.regex.tregex.util.DebugUtil;

public class RegexProperties {

    private boolean alternations = false;
    private boolean backReferences = false;
    private boolean captureGroups = false;
    private boolean charClasses = false;
    private boolean lookAheadAssertions = false;
    private boolean complexLookAheadAssertions = false;
    private boolean lookBehindAssertions = false;
    private boolean complexLookBehindAssertions = false;
    private boolean negativeLookAheadAssertions = false;
    private boolean loops = false;
    private boolean largeCountedRepetitions = false;

    public boolean hasAlternations() {
        return alternations;
    }

    public void setAlternations() {
        alternations = true;
    }

    public boolean hasBackReferences() {
        return backReferences;
    }

    public void setBackReferences() {
        backReferences = true;
    }

    public boolean hasCaptureGroups() {
        return captureGroups;
    }

    public void setCaptureGroups() {
        captureGroups = true;
    }

    public boolean hasCharClasses() {
        return charClasses;
    }

    public void setCharClasses() {
        charClasses = true;
    }

    public boolean hasLookAroundAssertions() {
        return hasLookAheadAssertions() || hasLookBehindAssertions();
    }

    public boolean hasLookAheadAssertions() {
        return lookAheadAssertions;
    }

    public void setLookAheadAssertions() {
        lookAheadAssertions = true;
    }

    public boolean hasComplexLookAheadAssertions() {
        return complexLookAheadAssertions;
    }

    public void setComplexLookAheadAssertions() {
        complexLookAheadAssertions = true;
    }

    public boolean hasLookBehindAssertions() {
        return lookBehindAssertions;
    }

    public void setLookBehindAssertions() {
        lookBehindAssertions = true;
    }

    public boolean hasComplexLookBehindAssertions() {
        return complexLookBehindAssertions;
    }

    public void setComplexLookBehindAssertions() {
        complexLookBehindAssertions = true;
    }

    public boolean hasNegativeLookAheadAssertions() {
        return negativeLookAheadAssertions;
    }

    public void setNegativeLookAheadAssertions() {
        negativeLookAheadAssertions = true;
    }

    public boolean hasLoops() {
        return loops;
    }

    public void setLoops() {
        loops = true;
    }

    public boolean hasLargeCountedRepetitions() {
        return largeCountedRepetitions;
    }

    public void setLargeCountedRepetitions() {
        largeCountedRepetitions = true;
    }

    @CompilerDirectives.TruffleBoundary
    public DebugUtil.Table toTable() {
        return new DebugUtil.Table("RegexProperties",
                        new DebugUtil.Value("Alternations", alternations),
                        new DebugUtil.Value("BackReferences", backReferences),
                        new DebugUtil.Value("CharClasses", charClasses),
                        new DebugUtil.Value("CaptureGroups", captureGroups),
                        new DebugUtil.Value("LookAheadAssertions", lookAheadAssertions),
                        new DebugUtil.Value("ComplexLookAheadAssertions", complexLookAheadAssertions),
                        new DebugUtil.Value("LookBehindAssertions", lookBehindAssertions),
                        new DebugUtil.Value("ComplexLookBehindAssertions", complexLookBehindAssertions),
                        new DebugUtil.Value("NegativeLookAheadAssertions", negativeLookAheadAssertions),
                        new DebugUtil.Value("Loops", loops),
                        new DebugUtil.Value("LargeCountedRepetitions", largeCountedRepetitions));
    }
}
