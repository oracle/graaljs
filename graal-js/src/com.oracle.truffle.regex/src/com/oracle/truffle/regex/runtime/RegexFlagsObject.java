/*
 * Copyright (c) 2017, 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.regex.runtime;

import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.regex.RegexFlags;
import com.oracle.truffle.regex.RegexLanguageObject;

public final class RegexFlagsObject implements TruffleObject, RegexLanguageObject {

    private final RegexFlags flags;

    public RegexFlagsObject(RegexFlags flags) {
        this.flags = flags;
    }

    public RegexFlags getFlags() {
        return flags;
    }

    public static boolean isInstance(TruffleObject object) {
        return object instanceof RegexFlagsObject;
    }

    @Override
    public ForeignAccess getForeignAccess() {
        return RegexFlagsObjectMessageResolutionForeign.ACCESS;
    }
}
