/*
 * Copyright (c) 2016, 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.regex.tregex.parser;

import com.oracle.truffle.regex.UnsupportedRegexException;

public class Counter {

    private int count = 0;

    public int getCount() {
        return count;
    }

    public void reset() {
        count = 0;
    }

    public int inc() {
        return inc(1);
    }

    public int inc(int i) {
        int ret = count;
        count += i;
        return ret;
    }

    public int dec() {
        return count--;
    }

    public static class ThresholdCounter extends Counter {

        private final int max;
        private final String errorMsg;

        public ThresholdCounter(int max, String errorMsg) {
            this.max = max;
            this.errorMsg = errorMsg;
        }

        @Override
        public int inc(int i) {
            final int ret = super.inc(i);
            if (getCount() > max) {
                throw new UnsupportedRegexException(errorMsg);
            }
            return ret;
        }
    }
}
