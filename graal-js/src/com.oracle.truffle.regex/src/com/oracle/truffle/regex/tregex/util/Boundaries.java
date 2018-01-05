/*
 * Copyright (c) 2016, 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.regex.tregex.util;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;

public class Boundaries {

    @TruffleBoundary
    public static int stringIndexOf(String s, char c, int fromIndex) {
        return s.indexOf(c, fromIndex);
    }

    @TruffleBoundary
    public static int stringLastIndexOf(String s, char c, int fromIndex) {
        return s.lastIndexOf(c, fromIndex);
    }
}
