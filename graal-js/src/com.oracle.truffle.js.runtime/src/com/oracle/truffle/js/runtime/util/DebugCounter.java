/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.runtime.util;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicLong;

import com.oracle.truffle.js.runtime.JSTruffleOptions;

public abstract class DebugCounter {
    private DebugCounter() {
    }

    public abstract long get();

    public abstract void inc();

    public static DebugCounter create(String name) {
        return JSTruffleOptions.DebugCounters ? DebugCounterImpl.createImpl(name) : Dummy.INSTANCE;
    }

    public static void dumpCounters() {
        if (JSTruffleOptions.DebugCounters) {
            DebugCounterImpl.dumpCounters(System.out);
        }
    }

    private static final class DebugCounterImpl extends DebugCounter {
        private static final ArrayList<DebugCounter> allCounters = new ArrayList<>();
        static {
            assert JSTruffleOptions.DebugCounters;
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                dumpCounters(System.out);
            }));
        }

        private final String name;
        private final AtomicLong value;

        private DebugCounterImpl(String name) {
            this.name = name;
            this.value = new AtomicLong();
            allCounters.add(this);
        }

        private static DebugCounter createImpl(String name) {
            return new DebugCounterImpl(name);
        }

        @Override
        public long get() {
            return value.get();
        }

        @Override
        public void inc() {
            value.incrementAndGet();
        }

        @Override
        public String toString() {
            return name + ": " + get();
        }

        private static void dumpCounters(PrintStream out) {
            for (DebugCounter counter : allCounters) {
                out.println(counter);
            }
        }
    }

    private static final class Dummy extends DebugCounter {
        static final DebugCounter INSTANCE = new Dummy();

        private Dummy() {
        }

        @Override
        public long get() {
            return 0;
        }

        @Override
        public void inc() {
        }
    }
}
