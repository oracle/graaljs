/*
 * Copyright (c) 2015, 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.runtime.util;

import java.util.concurrent.atomic.AtomicLong;

import com.oracle.truffle.js.runtime.JSTruffleOptions;

public final class TimeProfiler {
    private static final String CLASS_NAME = "[" + TimeProfiler.class.getSimpleName() + "] ";
    private final AtomicLong counter;

    public TimeProfiler() {
        counter = new AtomicLong();

        if (JSTruffleOptions.PrintCumulativeTime) {
            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() {
                    System.out.println(CLASS_NAME + "cumulative: " + TimeUtil.format(counter.get()));
                }
            });
        }
    }

    public void printElapsed(long startTime, String event) {
        long elapsed = System.nanoTime() - startTime;
        counter.addAndGet(elapsed);
        System.out.println(CLASS_NAME + event + " took: " + TimeUtil.format(elapsed));
    }
}
