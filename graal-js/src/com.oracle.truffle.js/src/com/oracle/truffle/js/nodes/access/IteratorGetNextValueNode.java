/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.dsl.Executed;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.cast.JSToBooleanNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.objects.IteratorRecord;

import java.util.Set;

/**
 * Combines IteratorStep and IteratorValue in one node.
 *
 * Equivalent to the following steps:
 *
 * <ol>
 * <li>Let next be IteratorStep(iteratorRecord).
 * <li>(opt) If next is an abrupt completion, set iteratorRecord.[[Done]] to true.
 * <li>ReturnIfAbrupt(next).
 * <li>If next is false,
 * <ol>
 * <li>(opt) Set iteratorRecord.[[Done]] to true.
 * <li>Return doneResult.
 * </ol>
 * <li>Else,
 * <ol>
 * <li>Let value be IteratorValue(next).
 * <li>(opt) If value is an abrupt completion, set iteratorRecord.[[Done]] to true.
 * <li>Return value.
 * </ol>
 * </ol>
 */
public abstract class IteratorGetNextValueNode extends JavaScriptNode {
    @Child @Executed JavaScriptNode iteratorNode;
    @Child private PropertyGetNode getValueNode;
    @Child private PropertyGetNode getDoneNode;
    @Child private JSFunctionCallNode methodCallNode;
    @Child private IsJSObjectNode isObjectNode;
    @Child private JavaScriptNode doneResultNode;
    @Child private JSToBooleanNode toBooleanNode;
    private final boolean setDone;

    protected IteratorGetNextValueNode(JSContext context, JavaScriptNode iteratorNode, JavaScriptNode doneNode, boolean setDone) {
        this.iteratorNode = iteratorNode;
        this.getValueNode = PropertyGetNode.create(JSRuntime.VALUE, false, context);
        this.getDoneNode = PropertyGetNode.create(JSRuntime.DONE, false, context);
        this.methodCallNode = JSFunctionCallNode.createCall();
        this.isObjectNode = IsJSObjectNode.create();
        this.toBooleanNode = JSToBooleanNode.create();
        this.doneResultNode = doneNode;
        this.setDone = setDone;
    }

    public static IteratorGetNextValueNode create(JSContext context, JavaScriptNode iterator, JavaScriptNode doneNode, boolean setDone) {
        return IteratorGetNextValueNodeGen.create(context, iterator, doneNode, setDone);
    }

    private Object iteratorNext(IteratorRecord iteratorRecord) {
        Object next = iteratorRecord.getNextMethod();
        DynamicObject iterator = iteratorRecord.getIterator();
        Object result = methodCallNode.executeCall(JSArguments.createZeroArg(iterator, next));
        if (!isObjectNode.executeBoolean(result)) {
            throw Errors.createTypeErrorIterResultNotAnObject(result, this);
        }
        return result;
    }

    @Specialization
    protected Object iteratorStepAndGetValue(VirtualFrame frame, IteratorRecord iteratorRecord) {
        try {
            Object result = iteratorNext(iteratorRecord);
            boolean done = toBooleanNode.executeBoolean(getDoneNode.getValue(result));
            if (!done) {
                return getValueNode.getValue(result);
            } else {
                if (setDone) {
                    iteratorRecord.setDone(true);
                }
                return doneResultNode.execute(frame);
            }
        } catch (Exception ex) {
            if (setDone) {
                iteratorRecord.setDone(true);
            }
            throw ex;
        }
    }

    public abstract Object execute(VirtualFrame frame, IteratorRecord iteratorRecord);

    @Override
    protected JavaScriptNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
        return create(getValueNode.getContext(), cloneUninitialized(iteratorNode, materializedTags), cloneUninitialized(doneResultNode, materializedTags), setDone);
    }
}
