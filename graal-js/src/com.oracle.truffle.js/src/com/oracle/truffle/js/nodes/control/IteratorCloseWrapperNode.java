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

import java.util.Set;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.exception.AbstractTruffleException;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.nodes.ControlFlowException;
import com.oracle.truffle.api.profiles.InlinedBranchProfile;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.access.IteratorCloseNode;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.objects.IteratorRecord;

/**
 * Wrapper around a for-in/of loop or ArrayAssignmentPattern that performs IteratorClose on abrupt
 * completion, and optionally on normal completion, too, if the iterator is not done.
 *
 * @see #arrayDestructuring
 * @see AsyncIteratorCloseWrapperNode
 */
public abstract class IteratorCloseWrapperNode extends JavaScriptNode {
    @Child private JavaScriptNode blockNode;
    @Child private JavaScriptNode iteratorNode;
    @Child private IteratorCloseNode iteratorCloseNode;
    private final JSContext context;
    /**
     * If true, follow the semantics of DestructuringAssignmentEvaluation : ArrayAssignmentPattern,
     * performing IteratorClose on any (abrupt and normal) completion if not iterator.[[Done]].
     * <p>
     * If false, follow the semantics of ForIn/OfBodyEvaluation, performing IteratorClose on abrupt
     * completion (but never on normal completion), regardless of iterator.[[Done]].
     */
    private final boolean arrayDestructuring;

    protected IteratorCloseWrapperNode(JSContext context, JavaScriptNode block, JavaScriptNode iterator, boolean arrayDestructuring) {
        this.context = context;
        this.blockNode = block;
        this.iteratorNode = iterator;
        this.arrayDestructuring = arrayDestructuring;
    }

    public static JavaScriptNode create(JSContext context, JavaScriptNode block, JavaScriptNode iterator, boolean arrayDestructuring) {
        return IteratorCloseWrapperNodeGen.create(context, block, iterator, arrayDestructuring);
    }

    @Specialization
    protected final Object doDefault(VirtualFrame frame,
                    @Cached InlinedBranchProfile throwBranch,
                    @Cached InlinedBranchProfile exitBranch,
                    @Cached InlinedBranchProfile notDoneBranch) {
        Object result;
        try {
            result = blockNode.execute(frame);
        } catch (YieldException e) {
            throw e;
        } catch (ControlFlowException e) {
            exitBranch.enter(this);
            IteratorRecord iteratorRecord = getIteratorRecord(frame);
            if (!arrayDestructuring || !iteratorRecord.isDone()) {
                iteratorClose().executeVoid(iteratorRecord.getIterator());
            }
            throw e;
        } catch (AbstractTruffleException e) {
            throwBranch.enter(this);
            IteratorRecord iteratorRecord = getIteratorRecord(frame);
            if (!arrayDestructuring || !iteratorRecord.isDone()) {
                iteratorClose().executeAbrupt(iteratorRecord.getIterator());
            }
            throw e;
        }

        IteratorRecord iteratorRecord = getIteratorRecord(frame);
        if (arrayDestructuring && !iteratorRecord.isDone()) {
            notDoneBranch.enter(this);
            iteratorClose().executeVoid(iteratorRecord.getIterator());
        }
        return result;
    }

    private IteratorRecord getIteratorRecord(VirtualFrame frame) {
        return (IteratorRecord) iteratorNode.execute(frame);
    }

    private IteratorCloseNode iteratorClose() {
        if (iteratorCloseNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            iteratorCloseNode = insert(IteratorCloseNode.create(context));
        }
        return iteratorCloseNode;
    }

    @Override
    protected JavaScriptNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
        return IteratorCloseWrapperNodeGen.create(context, cloneUninitialized(blockNode, materializedTags), cloneUninitialized(iteratorNode, materializedTags), arrayDestructuring);
    }
}
