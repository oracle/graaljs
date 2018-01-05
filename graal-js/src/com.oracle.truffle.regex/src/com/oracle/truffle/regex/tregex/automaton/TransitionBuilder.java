/*
 * Copyright (c) 2017, 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.regex.tregex.automaton;

import com.oracle.truffle.regex.tregex.matchers.MatcherBuilder;

public abstract class TransitionBuilder<SS> {

    private TransitionBuilder<SS> next;

    public TransitionBuilder<SS> getNext() {
        return next;
    }

    public void setNext(TransitionBuilder<SS> next) {
        this.next = next;
    }

    public abstract MatcherBuilder getMatcherBuilder();

    public abstract void setMatcherBuilder(MatcherBuilder matcherBuilder);

    public abstract SS getTargetState();

    public abstract TransitionBuilder<SS> createMerged(TransitionBuilder<SS> other, MatcherBuilder mergedMatcher);

    public abstract void mergeInPlace(TransitionBuilder<SS> other, MatcherBuilder mergedMatcher);
}
