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
package com.oracle.truffle.js.nodes.access;

import com.oracle.truffle.api.dsl.Executed;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.cast.JSToBooleanNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;

/**
 * (partially) implements ES6 7.4.5 IteratorStep(iterator).
 *
 * Note that this node returns the value instead of the result, thus is non-standard! For the
 * standard-compliant version, see {@link IteratorStepNode}.
 */
public abstract class IteratorStepSpecialNode extends JavaScriptNode {
    @Child @Executed JavaScriptNode iteratorNode;
    @Child private PropertyGetNode getNextNode;
    @Child private PropertyGetNode getValueNode;
    @Child private PropertyGetNode getDoneNode;
    @Child private JSFunctionCallNode methodCallNode;
    @Child private IsObjectNode isObjectNode;
    @Child private JavaScriptNode doneNode;
    @Child private JSToBooleanNode toBooleanNode;
    private final boolean setDoneOnError;

    protected IteratorStepSpecialNode(JSContext context, JavaScriptNode iteratorNode, JavaScriptNode doneNode, boolean setDoneOnError) {
        this.iteratorNode = iteratorNode;
        this.getNextNode = PropertyGetNode.create(JSRuntime.NEXT, false, context);
        this.getValueNode = PropertyGetNode.create(JSRuntime.VALUE, false, context);
        this.getDoneNode = PropertyGetNode.create(JSRuntime.DONE, false, context);
        this.methodCallNode = JSFunctionCallNode.createCall();
        this.isObjectNode = IsObjectNode.create();
        this.toBooleanNode = JSToBooleanNode.create();
        this.doneNode = doneNode;
        this.setDoneOnError = setDoneOnError;
    }

    public static IteratorStepSpecialNode create(JSContext context, JavaScriptNode iterator, JavaScriptNode doneNode, boolean setDoneOnError) {
        return IteratorStepSpecialNodeGen.create(context, iterator, doneNode, setDoneOnError);
    }

    @Specialization
    protected Object doIteratorStep(VirtualFrame frame, DynamicObject iterator) {
        Object next;
        Object result;
        try {
            next = getNextNode.getValue(iterator);
            result = methodCallNode.executeCall(JSArguments.createZeroArg(iterator, next));
            if (!isObjectNode.executeBoolean(result)) {
                throw Errors.createTypeErrorIterResultNotAnObject(result, this);
            }
        } catch (Exception ex) {
            if (setDoneOnError) {
                doneNode.execute(frame);
            }
            throw ex;
        }

        Object value = getValueNode.getValue(result);
        Object done = toBooleanNode.executeBoolean(getDoneNode.getValue(result));
        return done == Boolean.FALSE ? value : doneNode.execute(frame);
    }

    public abstract Object execute(VirtualFrame frame, DynamicObject iterator);

    @Override
    protected JavaScriptNode copyUninitialized() {
        return create(getNextNode.getContext(), cloneUninitialized(iteratorNode), cloneUninitialized(doneNode), setDoneOnError);
    }
}
