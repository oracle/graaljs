/*
 * Copyright (c) 2017, 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.regex.result;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.regex.RegexCompiledRegex;

public final class SingleIndexArrayResult extends RegexResult {

    @CompilationFinal(dimensions = 1) private final int[] indices;

    public SingleIndexArrayResult(RegexCompiledRegex regex, Object input, int[] indices) {
        super(regex, input, indices.length / 2);
        this.indices = indices;
    }

    public int[] getIndices() {
        return indices;
    }

}
