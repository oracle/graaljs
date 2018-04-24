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
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.runtime.Errors;

@NodeInfo(cost = NodeCost.NONE)
public final class ExprBlockNode extends AbstractBlockNode implements SequenceNode, ResumableNode {
    ExprBlockNode(JavaScriptNode[] statements) {
        super(statements);
        assert statements.length >= 1 : "block must contain at least 1 statement";
    }

    public static JavaScriptNode createExprBlock(JavaScriptNode[] statements) {
        return filterStatements(statements, true);
    }

    @Override
    public boolean hasTag(Class<? extends Tag> tag) {
        if (tag == StandardTags.ExpressionTag.class) {
            return true;
        }
        return super.hasTag(tag);
    }

    @ExplodeLoop
    @Override
    public Object execute(VirtualFrame frame) {
        JavaScriptNode[] stmts = statements;
        int last = stmts.length - 1;
        for (int i = 0; i < last; ++i) {
            stmts[i].executeVoid(frame);
        }
        return stmts[last].execute(frame);
    }

    @ExplodeLoop
    @Override
    public boolean executeBoolean(VirtualFrame frame) throws UnexpectedResultException {
        JavaScriptNode[] stmts = statements;
        int last = stmts.length - 1;
        for (int i = 0; i < last; ++i) {
            stmts[i].executeVoid(frame);
        }
        return stmts[last].executeBoolean(frame);
    }

    @ExplodeLoop
    @Override
    public int executeInt(VirtualFrame frame) throws UnexpectedResultException {
        JavaScriptNode[] stmts = statements;
        int last = stmts.length - 1;
        for (int i = 0; i < last; ++i) {
            stmts[i].executeVoid(frame);
        }
        return stmts[last].executeInt(frame);
    }

    @ExplodeLoop
    @Override
    public double executeDouble(VirtualFrame frame) throws UnexpectedResultException {
        JavaScriptNode[] stmts = statements;
        int last = stmts.length - 1;
        for (int i = 0; i < last; ++i) {
            stmts[i].executeVoid(frame);
        }
        return stmts[last].executeDouble(frame);
    }

    @ExplodeLoop
    @Override
    public Object resume(VirtualFrame frame) {
        int index = getStateAsIntAndReset(frame);
        JavaScriptNode[] stmts = statements;
        assert index < stmts.length;
        int last = stmts.length - 1;
        for (int i = 0; i < stmts.length; ++i) {
            if (i < index) {
                continue;
            }
            try {
                if (i != last) {
                    stmts[i].executeVoid(frame);
                } else {
                    return stmts[last].execute(frame);
                }
            } catch (YieldException e) {
                setState(frame, i);
                throw e;
            }
        }
        throw Errors.shouldNotReachHere();
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        return new ExprBlockNode(cloneUninitialized(statements));
    }

    @Override
    public boolean isResultAlwaysOfType(Class<?> clazz) {
        return statements[statements.length - 1].isResultAlwaysOfType(clazz);
    }
}
