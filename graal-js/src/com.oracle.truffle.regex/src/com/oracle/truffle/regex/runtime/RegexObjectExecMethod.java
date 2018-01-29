/*
 * Copyright (c) 2017, 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.regex.runtime;

import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.regex.RegexLanguageObject;
import com.oracle.truffle.regex.RegexObject;

public final class RegexObjectExecMethod implements RegexLanguageObject {

    private final RegexObject regex;

    public RegexObjectExecMethod(RegexObject regex) {
        this.regex = regex;
    }

    public RegexObject getRegexObject() {
        return regex;
    }

    public static boolean isInstance(TruffleObject object) {
        return object instanceof RegexObjectExecMethod;
    }

    @Override
    public ForeignAccess getForeignAccess() {
        return RegexObjectExecMethodMessageResolutionForeign.ACCESS;
    }
}
