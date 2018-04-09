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
package com.oracle.truffle.js.nodes.control;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.js.nodes.JSTypesGen;
import com.oracle.truffle.js.nodes.JavaScriptNode;

public class ReturnTargetNode extends JavaScriptNode {

    @Child protected JavaScriptNode body;

    protected ReturnTargetNode(JavaScriptNode child) {
        this.body = child;
    }

    public static ReturnTargetNode create(JavaScriptNode child) {
        return new ReturnTargetNode(child);
    }

    public static FrameReturnTargetNode createFrameReturnTarget(JavaScriptNode body, JavaScriptNode returnValue) {
        return new FrameReturnTargetNode(body, returnValue);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        try {
            return body.execute(frame);
        } catch (ReturnException ex) {
            return ex.getResult();
        }
    }

    @Override
    public int executeInt(VirtualFrame frame) throws UnexpectedResultException {
        try {
            return body.executeInt(frame);
        } catch (ReturnException ex) {
            return JSTypesGen.expectInteger(ex.getResult());
        }
    }

    @Override
    public double executeDouble(VirtualFrame frame) throws UnexpectedResultException {
        try {
            return body.executeDouble(frame);
        } catch (ReturnException ex) {
            return JSTypesGen.expectDouble(ex.getResult());
        }
    }

    @Override
    public boolean executeBoolean(VirtualFrame frame) throws UnexpectedResultException {
        try {
            return body.executeBoolean(frame);
        } catch (ReturnException ex) {
            return JSTypesGen.expectBoolean(ex.getResult());
        }
    }

    public final JavaScriptNode getBody() {
        return body;
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        return new ReturnTargetNode(cloneUninitialized(body));
    }

    public static class FrameReturnTargetNode extends ReturnTargetNode {

        @Child private JavaScriptNode returnValue;

        protected FrameReturnTargetNode(JavaScriptNode body, JavaScriptNode returnValue) {
            super(body);
            this.returnValue = returnValue;
        }

        @Override
        public Object execute(VirtualFrame frame) {
            try {
                return body.execute(frame);
            } catch (ReturnException ex) {
                return returnValue.execute(frame);
            }
        }

        @Override
        public int executeInt(VirtualFrame frame) throws UnexpectedResultException {
            try {
                return body.executeInt(frame);
            } catch (ReturnException ex) {
                return returnValue.executeInt(frame);
            }
        }

        @Override
        public double executeDouble(VirtualFrame frame) throws UnexpectedResultException {
            try {
                return body.executeDouble(frame);
            } catch (ReturnException ex) {
                return returnValue.executeDouble(frame);
            }
        }

        @Override
        public boolean executeBoolean(VirtualFrame frame) throws UnexpectedResultException {
            try {
                return body.executeBoolean(frame);
            } catch (ReturnException ex) {
                return returnValue.executeBoolean(frame);
            }
        }

        @Override
        protected JavaScriptNode copyUninitialized() {
            return new FrameReturnTargetNode(cloneUninitialized(body), cloneUninitialized(returnValue));
        }
    }
}
