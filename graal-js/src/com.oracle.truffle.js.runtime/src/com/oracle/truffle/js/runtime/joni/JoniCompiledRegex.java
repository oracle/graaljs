/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.runtime.joni;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.regex.CompiledRegex;
import com.oracle.truffle.regex.nashorn.regexp.joni.Regex;

public class JoniCompiledRegex implements CompiledRegex {

    private final Regex joniRegex;
    private final CallTarget regexCallTarget;

    public JoniCompiledRegex(Regex joniRegex, CallTarget regexCallTarget) {
        this.joniRegex = joniRegex;
        this.regexCallTarget = regexCallTarget;
    }

    public Regex getJoniRegex() {
        return joniRegex;
    }

    @Override
    public CallTarget getRegexCallTarget() {
        return regexCallTarget;
    }
}
