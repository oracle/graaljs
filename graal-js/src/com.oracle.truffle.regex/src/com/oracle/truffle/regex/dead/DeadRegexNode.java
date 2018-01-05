/*
 * Copyright (c) 2016, 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.regex.dead;

import com.oracle.truffle.regex.CompiledRegex;
import com.oracle.truffle.regex.RegexNode;
import com.oracle.truffle.regex.result.RegexResult;

/**
 * This RegexNode is used for regular expressions that can never match, like /a^a/, /a\ba/, /(?=a)b/
 * etc.
 */
public final class DeadRegexNode extends RegexNode {

    private final String pattern;

    public DeadRegexNode(String pattern) {
        this.pattern = pattern;
    }

    @Override
    protected RegexResult execute(CompiledRegex regex, Object input, int fromIndex) {
        return RegexResult.NO_MATCH;
    }

    @Override
    protected String getEngineLabel() {
        return "dead";
    }

    @Override
    protected String getPatternSource() {
        return pattern;
    }
}
