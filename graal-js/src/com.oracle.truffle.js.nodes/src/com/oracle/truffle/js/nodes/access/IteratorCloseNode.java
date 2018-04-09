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

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.objects.Undefined;

/**
 * ES6 7.4.6 IteratorClose(iterator, completion).
 *
 * The completion part must be handled in caller.
 */
public class IteratorCloseNode extends JavaScriptNode {
    @Child private GetMethodNode getReturnNode;
    @Child private JSFunctionCallNode methodCallNode;
    @Child private IsObjectNode isObjectNode;
    @Child private JavaScriptNode iteratorNode;

    protected IteratorCloseNode(JSContext context, JavaScriptNode iteratorNode) {
        this.getReturnNode = GetMethodNode.create(context, null, "return");
        this.methodCallNode = JSFunctionCallNode.createCall();
        this.isObjectNode = IsObjectNode.create();
        this.iteratorNode = iteratorNode;
    }

    public static IteratorCloseNode create(JSContext context) {
        return new IteratorCloseNode(context, null);
    }

    public static IteratorCloseNode create(JSContext context, JavaScriptNode iteratorNode) {
        return new IteratorCloseNode(context, iteratorNode);
    }

    public final void executeVoid(DynamicObject iterator) {
        Object returnMethod = getReturnNode.executeWithTarget(iterator);
        if (returnMethod != Undefined.instance) {
            Object innerResult = methodCallNode.executeCall(JSArguments.createZeroArg(iterator, returnMethod));
            if (!isObjectNode.executeBoolean(innerResult)) {
                throw Errors.createTypeErrorIterResultNotAnObject(innerResult, this);
            }
        }
    }

    public final Object execute(DynamicObject iterator, Object value) {
        executeVoid(iterator);
        return value;
    }

    public final void executeAbrupt(DynamicObject iterator) {
        Object returnMethod = getReturnNode.executeWithTarget(iterator);
        if (returnMethod != Undefined.instance) {
            try {
                methodCallNode.executeCall(JSArguments.createZeroArg(iterator, returnMethod));
            } catch (Exception e) {
                // re-throw outer exception, see 7.4.6 IteratorClose
            }
        }
    }

    public final <T extends Throwable> T executeRethrow(DynamicObject iterator, T exception) throws T {
        executeAbrupt(iterator);
        throw exception;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        executeVoid((DynamicObject) iteratorNode.execute(frame));
        return Undefined.instance;
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        return new IteratorCloseNode(getReturnNode.getContext(), cloneUninitialized(iteratorNode));
    }
}
