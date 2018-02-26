/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.regex.result;

import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.regex.RegexLanguageObject;
import com.oracle.truffle.regex.RegexObject;
import com.oracle.truffle.regex.runtime.RegexResultEndArrayObject;
import com.oracle.truffle.regex.runtime.RegexResultMessageResolutionForeign;
import com.oracle.truffle.regex.runtime.RegexResultStartArrayObject;

/**
 * {@link RegexResult} is a {@link TruffleObject} that represents the result of matching a regular
 * expression against a string. It can be obtained as the result of a {@link RegexObject}'s
 * {@code exec} method and has the following properties:
 * <ol>
 * <li>{@link Object} {@code input}: The input sequence this result was calculated from. If the
 * result is no match, this property is {@code null}.</li>
 * <li>{@code boolean isMatch}: {@code true} if a match was found, {@code false} otherwise.</li>
 * <li>{@code int groupCount}: number of capture groups present in the regular expression, including
 * group 0. If the result is no match, this property is {@code 0}.</li>
 * <li>{@link TruffleObject} {@code start}: array of positions where the beginning of the capture
 * group with the given number was found. If the result is no match, this property is an empty
 * array. Capture group number {@code 0} denotes the boundaries of the entire expression. If no
 * match was found for a particular capture group, the returned value at its respective index is
 * {@code -1}.</li>
 * <li>{@link TruffleObject} {@code end}: array of positions where the end of the capture group with
 * the given number was found. If the result is no match, this property is an empty array. Capture
 * group number {@code 0} denotes the boundaries of the entire expression. If no match was found for
 * a particular capture group, the returned value at its respective index is {@code -1}.</li>
 * </ol>
 * </li>
 */
public abstract class RegexResult implements RegexLanguageObject {

    public static final RegexResult NO_MATCH = new RegexResult(null, "NULL", 0) {
    };

    private final RegexObject regex;
    private final Object input;
    private final int groupCount;
    private final RegexResultStartArrayObject startArrayObject;
    private final RegexResultEndArrayObject endArrayObject;

    public RegexResult(RegexObject regex, Object input, int groupCount) {
        this.regex = regex;
        this.input = input;
        this.groupCount = groupCount;
        startArrayObject = new RegexResultStartArrayObject(this);
        endArrayObject = new RegexResultEndArrayObject(this);
    }

    public final RegexObject getCompiledRegex() {
        return regex;
    }

    public final Object getInput() {
        return input;
    }

    public final int getGroupCount() {
        return groupCount;
    }

    public final RegexResultStartArrayObject getStartArrayObject() {
        return startArrayObject;
    }

    public final RegexResultEndArrayObject getEndArrayObject() {
        return endArrayObject;
    }

    public static boolean isInstance(TruffleObject object) {
        return object instanceof RegexResult;
    }

    @Override
    public ForeignAccess getForeignAccess() {
        return RegexResultMessageResolutionForeign.ACCESS;
    }
}
