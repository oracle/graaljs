/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.runtime.joni;

/**
 * Static utility methods for analyzing regular expression patterns.
 */
public final class PatternAnalyzer {

    public static boolean containsGroup(String pattern) {
        boolean charClass = false;
        int i = 0;
        for (; i < pattern.length(); i++) {
            char ch = pattern.charAt(i);
            if (ch == '\\') {
                i++;
            } else if (charClass && ch == ']') {
                charClass = false;
            } else if (ch == '[') {
                charClass = true;
            } else if (!charClass && ch == '(') {
                if (!pattern.regionMatches(i + 1, "?", 0, 1)) {
                    return true;
                }
            }
        }
        return false;
    }

}
