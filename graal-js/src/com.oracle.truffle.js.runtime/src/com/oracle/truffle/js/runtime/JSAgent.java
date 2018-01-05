/*
 * Copyright (c) 2017, 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.runtime;

import java.util.concurrent.atomic.AtomicInteger;

import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.runtime.JSAgentWaiterList.JSAgentWaiterListEntry;
import com.oracle.truffle.js.runtime.builtins.JSArrayBufferView;
import com.oracle.truffle.js.runtime.builtins.JSSharedArrayBuffer;

/**
 * Base class for ECMA2017 8.7 Agents.
 */
public abstract class JSAgent implements EcmaAgent {

    private static final AtomicInteger signifierGenerator = new AtomicInteger(0);

    /* ECMA2017 Agent Record */
    private final int signifier;
    private final boolean canBlock;

    private boolean inAtomicSection;
    private boolean inCriticalSection;

    public JSAgent() {
        this.signifier = signifierGenerator.incrementAndGet();
        this.canBlock = true;
        this.inCriticalSection = false;
        this.inAtomicSection = false;
    }

    public abstract void wakeAgent(int w);

    public int getSignifier() {
        return signifier;
    }

    public boolean canBlock() {
        return canBlock;
    }

    public boolean inCriticalSection() {
        return inCriticalSection;
    }

    public void criticalSectionEnter(JSAgentWaiterListEntry wl) {
        assert !inCriticalSection;
        wl.lock();
        inCriticalSection = true;
    }

    public void criticalSectionLeave(JSAgentWaiterListEntry wl) {
        assert inCriticalSection;
        inCriticalSection = false;
        wl.unlock();
    }

    public void atomicSectionEnter(DynamicObject target) {
        assert !inAtomicSection;
        assert JSArrayBufferView.isJSArrayBufferView(target);
        DynamicObject arrayBuffer = JSArrayBufferView.getArrayBuffer(target, JSArrayBufferView.isJSArrayBufferView(target));
        JSAgentWaiterList waiterList = JSSharedArrayBuffer.getWaiterList(arrayBuffer);
        waiterList.lock();
        inAtomicSection = true;
    }

    public void atomicSectionLeave(DynamicObject target) {
        assert inAtomicSection;
        assert JSArrayBufferView.isJSArrayBufferView(target);
        DynamicObject arrayBuffer = JSArrayBufferView.getArrayBuffer(target, JSArrayBufferView.isJSArrayBufferView(target));
        JSAgentWaiterList waiterList = JSSharedArrayBuffer.getWaiterList(arrayBuffer);
        inAtomicSection = false;
        waiterList.unlock();
    }

}
