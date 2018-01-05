/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.regex;

public interface RegexEngine {

    /**
     * Uses the engine to try and compile the regular expression described in {@code source}.
     * 
     * @param source
     * @return the {@link CompiledRegex} or null if the engine could not compile the regular
     *         expression
     * @throws RegexSyntaxException if the engine discovers a syntax error in the regular expression
     */
    CompiledRegex compile(RegexSource source) throws RegexSyntaxException;

}
