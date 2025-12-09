/*
 * Copyright (c) 2018, 2025, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.runtime.objects.Undefined;

public abstract class AbstractGeneratorBlockNode extends AbstractBlockNode {
    protected final int stateSlot;

    protected AbstractGeneratorBlockNode(JavaScriptNode[] statements, int stateSlot) {
        super(statements);
        this.stateSlot = stateSlot;
    }

    protected final int getStateAndReset(VirtualFrame frame) {
        int index = 0;
        if (frame.isInt(stateSlot)) {
            index = frame.getInt(stateSlot);
            assert index < statements.length;
        } else {
            assert frame.isObject(stateSlot) && frame.getObject(stateSlot) == Undefined.instance;
        }
        setState(frame, 0);
        return index;
    }

    protected final void setState(VirtualFrame frame, int index) {
        frame.setInt(stateSlot, index);
    }

    @ExplodeLoop
    @Override
    public void executeVoid(VirtualFrame frame) {
        int startIndex = getStateAndReset(frame);
        JavaScriptNode[] stmts = statements;
        for (int i = 0; i < stmts.length; ++i) {
            executeVoid(frame, stmts[i], i, startIndex);
        }
    }

    @ExplodeLoop
    @Override
    public Object execute(VirtualFrame frame) {
        int startIndex = getStateAndReset(frame);
        JavaScriptNode[] stmts = statements;
        int last = stmts.length - 1;
        for (int i = 0; i < last; ++i) {
            executeVoid(frame, stmts[i], i, startIndex);
        }
        try {
            return stmts[last].execute(frame);
        } catch (YieldException e) {
            setState(frame, last);
            throw e;
        }
    }

    public void executeVoid(VirtualFrame frame, JavaScriptNode node, int index, int startIndex) {
        if (index < startIndex) {
            return;
        }
        try {
            node.executeVoid(frame);
        } catch (YieldException e) {
            setState(frame, index);
            throw e;
        }
    }

}
