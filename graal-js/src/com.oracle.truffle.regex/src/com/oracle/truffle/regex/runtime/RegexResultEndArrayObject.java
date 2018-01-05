/*
 * Copyright (c) 2017, 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.regex.runtime;

import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.regex.RegexLanguageObject;
import com.oracle.truffle.regex.result.RegexResult;

public final class RegexResultEndArrayObject implements TruffleObject, RegexLanguageObject {

    private final RegexResult result;

    public RegexResultEndArrayObject(RegexResult result) {
        this.result = result;
    }

    public RegexResult getResult() {
        return result;
    }

    public static boolean isInstance(TruffleObject object) {
        return object instanceof RegexResultEndArrayObject;
    }

    @Override
    public ForeignAccess getForeignAccess() {
        return RegexResultEndArrayObjectMessageResolutionForeign.ACCESS;
    }
}
