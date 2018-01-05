/*
 * Copyright (c) 2017, 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.regex.util;

import com.oracle.truffle.api.CompilerDirectives;

public class NumberConversion {

    public static int intValue(Number number) {
        if (number instanceof Integer) {
            return ((Integer) number).intValue();
        } else if (number instanceof Long) {
            return ((Long) number).intValue();
        } else {
            return invokeIntValue(number);
        }
    }

    @CompilerDirectives.TruffleBoundary
    private static int invokeIntValue(Number number) {
        return number.intValue();
    }
}
