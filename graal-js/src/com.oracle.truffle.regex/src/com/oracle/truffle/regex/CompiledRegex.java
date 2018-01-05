/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.regex;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.regex.result.RegexResult;

public class CompiledRegex {

    private final RegexSource source;
    private final CallTarget callTarget;

    public CompiledRegex(RegexSource source, CallTarget callTarget) {
        this.source = source;
        this.callTarget = callTarget;
    }

    public RegexSource getSource() {
        return source;
    }

    /**
     * A call target to the underlying {@link RegexNode} which will return a {@link RegexResult}.
     * The signature of this operation corresponds to:
     * <code>{@link RegexResult} find(String input, int fromIndex);</code>
     */
    public CallTarget getCallTarget() {
        return callTarget;
    }
}
