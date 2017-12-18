/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.regex.joni;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.regex.RegexCompiledRegex;
import com.oracle.truffle.regex.RegexSource;
import com.oracle.truffle.regex.nashorn.regexp.joni.Regex;

public final class JoniRegexCompiledRegex extends RegexCompiledRegex {
    public final Regex implementation;

    public JoniRegexCompiledRegex(RegexSource source, CallTarget callTarget, Regex implementation) {
        super(source, callTarget);
        this.implementation = implementation;
    }
}
