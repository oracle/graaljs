/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.nodes.access;

import java.util.Set;

import com.oracle.truffle.api.dsl.Executed;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.builtins.JSArray;
import com.oracle.truffle.js.runtime.builtins.JSArrayObject;
import com.oracle.truffle.js.runtime.objects.IteratorRecord;

/**
 * Absorb iterator to new array.
 */
public abstract class IteratorToArrayNode extends JavaScriptNode {
    private final JSContext context;
    @Child @Executed JavaScriptNode iteratorNode;
    @Child private IteratorGetNextValueNode iteratorStepNode;
    @Child private WriteElementNode writeNode;

    protected IteratorToArrayNode(JSContext context, JavaScriptNode iteratorNode, IteratorGetNextValueNode iteratorStepNode) {
        this.context = context;
        this.iteratorNode = iteratorNode;
        this.iteratorStepNode = iteratorStepNode;

        writeNode = WriteElementNode.create(context, true, true);
    }

    public static IteratorToArrayNode create(JSContext context, JavaScriptNode iterator) {
        IteratorGetNextValueNode iteratorStep = IteratorGetNextValueNode.create(context, null, JSConstantNode.create(null), true);
        return IteratorToArrayNodeGen.create(context, iterator, iteratorStep);
    }

    @Specialization(guards = "!iteratorRecord.isDone()")
    protected Object doIterator(VirtualFrame frame, IteratorRecord iteratorRecord) {
        JSArrayObject items = JSArray.createEmptyChecked(context, getRealm(), 0);

        long index;
        for (index = 0;; index++) {
            Object value = iteratorStepNode.execute(frame, iteratorRecord);
            if (value == null) {
                break;
            }
            writeNode.executeWithTargetAndIndexAndValue(items, index, value);
        }

        return items;
    }

    @Specialization(guards = "iteratorRecord.isDone()")
    protected Object doDoneIterator(@SuppressWarnings("unused") IteratorRecord iteratorRecord) {
        return JSArray.createEmptyZeroLength(context, getRealm());
    }

    public abstract Object execute(VirtualFrame frame, IteratorRecord iteratorRecord);

    @Override
    protected JavaScriptNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
        return IteratorToArrayNodeGen.create(context, cloneUninitialized(iteratorNode, materializedTags), cloneUninitialized(iteratorStepNode, materializedTags));
    }
}
