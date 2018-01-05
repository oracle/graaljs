/*
 * Copyright (c) 2017, 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.runtime;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Java-based implementation of ECMA2017 WaiterList (24.4.1.2).
 */
public class JSAgentWaiterList {

    private final Map<Integer, JSAgentWaiterListEntry> waiters;

    private final Lock globalMonitor;

    @TruffleBoundary
    public JSAgentWaiterList() {
        this.waiters = new ConcurrentHashMap<>();
        this.globalMonitor = new ReentrantLock();
    }

    public JSAgentWaiterListEntry getListForIndex(int indexPos) {
        JSAgentWaiterListEntry list = Boundaries.mapPutIfAbsent(waiters, indexPos, new JSAgentWaiterListEntry());
        if (list == null) {
            return Boundaries.mapGet(waiters, indexPos);
        } else {
            return list;
        }
    }

    public void lock() {
        globalMonitor.lock();
    }

    public void unlock() {
        globalMonitor.unlock();
    }

    public static final class JSAgentWaiterListEntry extends ConcurrentLinkedQueue<Integer> {

        private static final long serialVersionUID = 2655886588267252886L;

        private final Lock indexMonitor;

        @TruffleBoundary
        public JSAgentWaiterListEntry() {
            super();
            this.indexMonitor = new ReentrantLock();
        }

        public void lock() {
            indexMonitor.lock();
        }

        public void unlock() {
            indexMonitor.unlock();
        }

    }

}
