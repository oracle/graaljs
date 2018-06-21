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
package com.oracle.truffle.js.nodes.function;

import java.util.Arrays;
import java.util.List;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.access.GetIteratorNode;
import com.oracle.truffle.js.nodes.access.IteratorStepSpecialNode;
import com.oracle.truffle.js.nodes.access.JSConstantNode;
import com.oracle.truffle.js.runtime.Boundaries;
import com.oracle.truffle.js.runtime.JSContext;

public final class SpreadArgumentNode extends JavaScriptNode {
    @Child private GetIteratorNode getIteratorNode;
    @Child private IteratorStepSpecialNode iteratorStepNode;

    private SpreadArgumentNode(JSContext context, JavaScriptNode arg) {
        this.getIteratorNode = GetIteratorNode.create(context, arg);
        this.iteratorStepNode = IteratorStepSpecialNode.create(context, null, JSConstantNode.create(null), false);
    }

    @Override
    public boolean isInstrumentable() {
        return false;
    }

    public static SpreadArgumentNode create(JSContext context, JavaScriptNode arg) {
        return new SpreadArgumentNode(context, arg);
    }

    public Object[] executeFillObjectArray(VirtualFrame frame, Object[] arguments, int delta) {
        DynamicObject iterator = getIteratorNode.execute(frame);
        Object[] args = arguments;
        int i = 0;
        for (;;) {
            Object nextArg = iteratorStepNode.execute(frame, iterator);
            if (nextArg == null) {
                break;
            }
            if (delta + i >= args.length) {
                args = Arrays.copyOf(args, args.length + (args.length + 1) / 2);
            }
            args[delta + i++] = nextArg;
        }
        return delta + i == args.length ? args : Arrays.copyOf(args, delta + i);
    }

    @Override
    public Object[] executeObjectArray(VirtualFrame frame) {
        DynamicObject iterator = getIteratorNode.execute(frame);
        Object[] args = new Object[0];
        for (int i = 0;; i++) {
            Object nextArg = iteratorStepNode.execute(frame, iterator);
            if (nextArg == null) {
                break;
            }
            if (i >= args.length) {
                args = Arrays.copyOf(args, args.length + 1);
            }
            args[i] = nextArg;
        }
        return args;
    }

    @Override
    public Object[] execute(VirtualFrame frame) {
        return executeObjectArray(frame);
    }

    public void executeToList(VirtualFrame frame, List<Object> argList) {
        DynamicObject iterator = getIteratorNode.execute(frame);
        for (;;) {
            Object nextArg = iteratorStepNode.execute(frame, iterator);
            if (nextArg == null) {
                break;
            }
            Boundaries.listAdd(argList, nextArg);
        }
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        SpreadArgumentNode copy = (SpreadArgumentNode) copy();
        copy.getIteratorNode = cloneUninitialized(getIteratorNode);
        copy.iteratorStepNode = cloneUninitialized(iteratorStepNode);
        return copy;
    }
}
