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
package com.oracle.truffle.js.nodes.promise;

import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.nodes.access.IteratorStepNode;
import com.oracle.truffle.js.nodes.access.IteratorValueNode;
import com.oracle.truffle.js.nodes.access.PropertyGetNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.builtins.JSPromise;
import com.oracle.truffle.js.runtime.objects.IteratorRecord;
import com.oracle.truffle.js.runtime.objects.PromiseCapabilityRecord;

public class PerformPromiseRaceNode extends PerformPromiseAllOrRaceNode {

    @Child private IteratorStepNode iteratorStep;
    @Child private IteratorValueNode iteratorValue;
    @Child private PropertyGetNode getResolve;
    @Child private JSFunctionCallNode callResolve;
    @Child private PropertyGetNode getThen;
    @Child private JSFunctionCallNode callThen;

    protected PerformPromiseRaceNode(JSContext context) {
        super(context);
        this.iteratorStep = IteratorStepNode.create(context);
        this.iteratorValue = IteratorValueNode.create(context);
        this.getResolve = PropertyGetNode.create(JSPromise.RESOLVE, false, context);
        this.callResolve = JSFunctionCallNode.createCall();
        this.getThen = PropertyGetNode.create(JSPromise.THEN, false, context);
        this.callThen = JSFunctionCallNode.createCall();
    }

    public static PerformPromiseRaceNode create(JSContext context) {
        return new PerformPromiseRaceNode(context);
    }

    @Override
    public DynamicObject execute(IteratorRecord iteratorRecord, DynamicObject constructor, PromiseCapabilityRecord resultCapability) {
        assert JSRuntime.isConstructor(constructor);
        for (;;) {
            Object next;
            try {
                next = iteratorStep.execute(iteratorRecord.getIterator());
            } catch (Throwable error) {
                iteratorRecord.setDone(true);
                throw error;
            }
            if (next == Boolean.FALSE) {
                iteratorRecord.setDone(true);
                return resultCapability.getPromise();
            }
            Object nextValue;
            try {
                nextValue = iteratorValue.execute((DynamicObject) next);
            } catch (Throwable error) {
                iteratorRecord.setDone(true);
                throw error;
            }
            Object nextPromise = callResolve.executeCall(JSArguments.createOneArg(constructor, getResolve.getValue(constructor), nextValue));
            callThen.executeCall(JSArguments.create(nextPromise, getThen.getValue(nextPromise), resultCapability.getResolve(), resultCapability.getReject()));
        }
    }
}
