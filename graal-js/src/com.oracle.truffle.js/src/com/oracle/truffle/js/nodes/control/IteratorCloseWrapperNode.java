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
package com.oracle.truffle.js.nodes.control;

import java.util.Set;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.nodes.ControlFlowException;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.access.IteratorCloseNode;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.objects.IteratorRecord;

public class IteratorCloseWrapperNode extends JavaScriptNode {
    @Child private JavaScriptNode blockNode;
    @Child private JavaScriptNode iteratorNode;
    @Child private IteratorCloseNode iteratorCloseNode;
    private final JSContext context;
    private final BranchProfile throwBranch = BranchProfile.create();
    private final BranchProfile exitBranch = BranchProfile.create();
    private final BranchProfile notDoneBranch = BranchProfile.create();
    @Child private InteropLibrary exceptions;

    protected IteratorCloseWrapperNode(JSContext context, JavaScriptNode block, JavaScriptNode iterator) {
        this.context = context;
        this.blockNode = block;
        this.iteratorNode = iterator;
    }

    public static JavaScriptNode create(JSContext context, JavaScriptNode block, JavaScriptNode iterator) {
        return new IteratorCloseWrapperNode(context, block, iterator);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        Object result;
        try {
            result = blockNode.execute(frame);
        } catch (YieldException e) {
            throw e;
        } catch (ControlFlowException e) {
            exitBranch.enter();
            IteratorRecord iteratorRecord = getIteratorRecord(frame);
            if (!iteratorRecord.isDone()) {
                iteratorClose().executeVoid(iteratorRecord.getIterator());
            }
            throw e;
        } catch (Throwable e) {
            if (TryCatchNode.shouldCatch(e, exceptions())) {
                throwBranch.enter();
                IteratorRecord iteratorRecord = getIteratorRecord(frame);
                if (!iteratorRecord.isDone()) {
                    iteratorClose().executeAbrupt(iteratorRecord.getIterator());
                }
            }
            throw e;
        }

        IteratorRecord iteratorRecord = getIteratorRecord(frame);
        if (!iteratorRecord.isDone()) {
            notDoneBranch.enter();
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

    private InteropLibrary exceptions() {
        InteropLibrary e = exceptions;
        if (e == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            exceptions = e = insert(InteropLibrary.getFactory().createDispatched(JSConfig.InteropLibraryLimit));
        }
        return e;
    }

    @Override
    protected JavaScriptNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
        return new IteratorCloseWrapperNode(context, cloneUninitialized(blockNode, materializedTags), cloneUninitialized(iteratorNode, materializedTags));
    }
}
