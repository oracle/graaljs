/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The Universal Permissive License (UPL), Version 1.0
 *
 * Subject to the condition set forth below, permission is hereby granted to any
 * person obtaining a copy of this software, associated documentation and/or
 * data (collectively the "Software"), free of charge and under any and all
 * copyright rights in the Software, and any and all patent rights owned or
 * freely licensable by each licensor hereunder covering either (i) the
 * unmodified Software as contributed to or provided by such licensor, or (ii)
 * the Larger Works (as defined below), to deal in both
 *
 * (a) the Software, and
 *
 * (b) any piece of software and/or hardware listed in the lrgrwrks.txt file if
 * one is included with the Software each a "Larger Work" to which the Software
 * is contributed by such licensors),
 *
 * without restriction, including without limitation the rights to copy, create
 * derivative works of, display, perform, and distribute the Software and make,
 * use, sell, offer for sale, import, export, have made, and have sold the
 * Software and the Larger Work(s), and to sublicense the foregoing rights on
 * either these or other terms.
 *
 * This license is subject to the following condition:
 *
 * The above copyright notice and either this complete permission notice or at a
 * minimum a reference to the UPL must be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
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
