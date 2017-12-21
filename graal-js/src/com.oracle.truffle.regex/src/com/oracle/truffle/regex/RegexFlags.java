/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.regex;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.regex.runtime.RegexFlagsMessageResolutionForeign;

public final class RegexFlags implements TruffleObject, RegexLanguageObject {

    private static final int NONE = 0;
    private static final int IGNORE_CASE = 1;
    private static final int MULTILINE = 1 << 1;
    private static final int STICKY = 1 << 2;
    private static final int GLOBAL = 1 << 3;
    private static final int UNICODE = 1 << 4;
    private static final int DOT_ALL = 1 << 5;

    public static final RegexFlags DEFAULT = new RegexFlags("", false, false, false, false, false, false);

    private final String source;
    private final int value;

    private RegexFlags(String source, boolean ignoreCase, boolean multiline, boolean global, boolean sticky, boolean unicode, boolean dotAll) {
        this.source = source;
        this.value = (ignoreCase ? IGNORE_CASE : NONE) | (multiline ? MULTILINE : NONE) | (sticky ? STICKY : NONE) | (global ? GLOBAL : NONE) | (unicode ? UNICODE : NONE) | (dotAll ? DOT_ALL : NONE);
    }

    @TruffleBoundary
    public static RegexFlags parseFlags(String source) throws RegexSyntaxException {
        if (source.isEmpty()) {
            return DEFAULT;
        }
        boolean ignoreCase = false;
        boolean multiline = false;
        boolean global = false;
        boolean sticky = false;
        boolean unicode = false;
        boolean dotAll = false;

        for (int i = 0; i < source.length(); i++) {
            char ch = source.charAt(i);
            boolean repeated;
            switch (ch) {
                case 'i':
                    repeated = ignoreCase;
                    ignoreCase = true;
                    break;
                case 'm':
                    repeated = multiline;
                    multiline = true;
                    break;
                case 'g':
                    repeated = global;
                    global = true;
                    break;
                case 'y':
                    repeated = sticky;
                    sticky = true;
                    break;
                case 'u':
                    repeated = unicode;
                    unicode = true;
                    break;
                case 's':
                    repeated = dotAll;
                    dotAll = true;
                    break;
                default:
                    throw new RegexSyntaxException(source, "unsupported regex flag: " + ch);
            }
            if (repeated) {
                throw new RegexSyntaxException(source, "repeated regex flag: " + ch);
            }
        }
        return new RegexFlags(source, ignoreCase, multiline, global, sticky, unicode, dotAll);
    }

    public String getSource() {
        return source;
    }

    public boolean isIgnoreCase() {
        return isSet(IGNORE_CASE);
    }

    public boolean isMultiline() {
        return isSet(MULTILINE);
    }

    public boolean isSticky() {
        return isSet(STICKY);
    }

    public boolean isGlobal() {
        return isSet(GLOBAL);
    }

    public boolean isUnicode() {
        return isSet(UNICODE);
    }

    public boolean isDotAll() {
        return isSet(DOT_ALL);
    }

    public boolean isNone() {
        return value == NONE;
    }

    private boolean isSet(int flag) {
        return (value & flag) != NONE;
    }

    @Override
    public String toString() {
        return source;
    }

    @Override
    public int hashCode() {
        return value;
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this || obj != null && obj instanceof RegexFlags && value == ((RegexFlags) obj).value;
    }

    public static boolean isInstance(TruffleObject object) {
        return object instanceof RegexFlags;
    }

    @Override
    public ForeignAccess getForeignAccess() {
        return RegexFlagsMessageResolutionForeign.ACCESS;
    }

}
