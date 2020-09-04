/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.access.IteratorStepNode;
import com.oracle.truffle.js.nodes.access.IteratorValueNode;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.objects.IteratorRecord;
import com.oracle.truffle.js.runtime.objects.PromiseCapabilityRecord;

public abstract class PerformPromiseCombinatorNode extends JavaScriptBaseNode {
    protected final JSContext context;
    @Child private IteratorStepNode iteratorStep;
    @Child private IteratorValueNode iteratorValue;

    protected PerformPromiseCombinatorNode(JSContext context) {
        this.context = context;
        this.iteratorStep = IteratorStepNode.create(context);
        this.iteratorValue = IteratorValueNode.create(context);
    }

    public abstract DynamicObject execute(IteratorRecord iteratorRecord, DynamicObject constructor, PromiseCapabilityRecord resultCapability, Object promiseResolve);

    /**
     * Let next be IteratorStep(iteratorRecord). If next is an abrupt completion, set
     * iteratorRecord.[[Done]] to true.
     */
    protected final Object iteratorStepOrSetDone(IteratorRecord iteratorRecord) {
        Object next;
        try {
            next = iteratorStep.execute(iteratorRecord);
        } catch (Throwable error) {
            iteratorRecord.setDone(true);
            throw error;
        }
        return next;
    }

    /**
     * Let nextValue be IteratorValue(next). If nextValue is an abrupt completion, set
     * iteratorRecord.[[Done]] to true.
     */
    protected final Object iteratorValueOrSetDone(IteratorRecord iteratorRecord, Object next) {
        Object nextValue;
        try {
            nextValue = iteratorValue.execute((DynamicObject) next);
        } catch (Throwable error) {
            iteratorRecord.setDone(true);
            throw error;
        }
        return nextValue;
    }

    protected static final class BoxedInt {
        int value;

        BoxedInt(int value) {
            this.value = value;
        }
    }
}
