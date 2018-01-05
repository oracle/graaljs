/*
 * Copyright (c) 2015, 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.runtime.util;

import java.util.Locale;

public final class TimeUtil {
    private TimeUtil() {
    }

    public static String format(long time) {
        if (time < 1_000_000L) {
            return String.format(Locale.ROOT, "%.2f\u00b5s", time / 1e3);
        } else if (time < 1_000_000_000L) {
            return String.format(Locale.ROOT, "%.2fms", time / 1e6);
        } else {
            return String.format(Locale.ROOT, "%.2fs", time / 1e9);
        }
    }
}
