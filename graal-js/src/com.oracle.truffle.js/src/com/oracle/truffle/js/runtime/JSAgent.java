/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The Universal Permissive License (UPL), Version 1.0
 *
 * Subject to the condition set forth below, permission is hereby granted to any
 * person obtaining a copy of this software, associated documentation and/or
 * data (collectively the "Software"), free of charge and under any and all
 * copyright rights in the Software, and any and all patent rights owned or
 * freely licensable by each licensor hereunder covering either (i) the
 * unmodified Software as contributed to or provided by such licensor, or (ii)
 * the Larger Works (as defined below), to deal in both
 *
 * (a) the Software, and
 *
 * (b) any piece of software and/or hardware listed in the lrgrwrks.txt file if
 * one is included with the Software each a "Larger Work" to which the Software
 * is contributed by such licensors),
 *
 * without restriction, including without limitation the rights to copy, create
 * derivative works of, display, perform, and distribute the Software and make,
 * use, sell, offer for sale, import, export, have made, and have sold the
 * Software and the Larger Work(s), and to sublicense the foregoing rights on
 * either these or other terms.
 *
 * This license is subject to the following condition:
 *
 * The above copyright notice and either this complete permission notice or at a
 * minimum a reference to the UPL must be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
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

    public JSAgent(boolean canBlock) {
        this.signifier = signifierGenerator.incrementAndGet();
        this.canBlock = canBlock;
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
