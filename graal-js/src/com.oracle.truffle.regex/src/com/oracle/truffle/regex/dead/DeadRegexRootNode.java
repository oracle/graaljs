/*
 * Copyright (c) 2016, 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.regex.dead;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.regex.CompiledRegex;
import com.oracle.truffle.regex.RegexLanguage;
import com.oracle.truffle.regex.RegexObject;
import com.oracle.truffle.regex.RegexRootNode;
import com.oracle.truffle.regex.RegexSource;
import com.oracle.truffle.regex.result.RegexResult;

/**
 * This RegexNode is used for regular expressions that can never match, like /a^a/, /a\ba/, /(?=a)b/
 * etc.
 */
public final class DeadRegexRootNode extends RegexRootNode implements CompiledRegex {

    private final CallTarget regexCallTarget;

    public DeadRegexRootNode(RegexLanguage language, RegexSource source) {
        super(language, source);
        regexCallTarget = Truffle.getRuntime().createCallTarget(this);
    }

    @Override
    protected RegexResult execute(VirtualFrame frame, RegexObject regex, Object input, int fromIndex) {
        return RegexResult.NO_MATCH;
    }

    @Override
    protected String getEngineLabel() {
        return "dead";
    }

    @Override
    public CallTarget getRegexCallTarget() {
        return regexCallTarget;
    }
}
