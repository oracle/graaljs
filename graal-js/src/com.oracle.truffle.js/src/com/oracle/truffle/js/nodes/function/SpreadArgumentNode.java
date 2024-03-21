/*
 * Copyright (c) 2018, 2024, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Set;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.profiles.InlinedBranchProfile;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.access.GetIteratorUnaryNode;
import com.oracle.truffle.js.nodes.access.IteratorGetNextValueNode;
import com.oracle.truffle.js.nodes.access.JSConstantNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.objects.IteratorRecord;
import com.oracle.truffle.js.runtime.util.SimpleArrayList;

public abstract class SpreadArgumentNode extends JavaScriptNode {
    @Child private GetIteratorUnaryNode getIteratorNode;
    @Child private IteratorGetNextValueNode iteratorStepNode;
    private final JSContext context;

    protected SpreadArgumentNode(JSContext context, GetIteratorUnaryNode getIteratorNode) {
        this.context = context;
        this.getIteratorNode = getIteratorNode;
        this.iteratorStepNode = IteratorGetNextValueNode.create(context, null, JSConstantNode.create(null), false);
    }

    @Override
    public boolean isInstrumentable() {
        return false;
    }

    public static SpreadArgumentNode create(JSContext context, GetIteratorUnaryNode getIteratorNode) {
        return SpreadArgumentNodeGen.create(context, getIteratorNode);
    }

    @Override
    public final Object[] execute(VirtualFrame frame) {
        SimpleArrayList<Object> argList = new SimpleArrayList<>();
        executeToList(frame, argList);
        return argList.toArray();
    }

    @Override
    public final boolean executeBoolean(VirtualFrame frame) throws UnexpectedResultException {
        return super.executeBoolean(frame);
    }

    @Override
    public final double executeDouble(VirtualFrame frame) throws UnexpectedResultException {
        return super.executeDouble(frame);
    }

    @Override
    public final int executeInt(VirtualFrame frame) throws UnexpectedResultException {
        return super.executeInt(frame);
    }

    @Override
    public final void executeVoid(VirtualFrame frame) {
        super.executeVoid(frame);
    }

    public abstract void executeToList(VirtualFrame frame, SimpleArrayList<Object> argList);

    @Specialization
    protected final void doSpread(VirtualFrame frame, SimpleArrayList<Object> argList,
                    @Cached InlinedBranchProfile growBranch,
                    @Cached InlinedBranchProfile errorBranch) {
        IteratorRecord iteratorRecord = getIteratorNode.execute(frame);
        for (;;) {
            Object nextArg = iteratorStepNode.execute(frame, iteratorRecord);
            if (nextArg == null) {
                break;
            }
            if (argList.size() >= context.getFunctionArgumentsLimit()) {
                errorBranch.enter(this);
                throw Errors.createRangeError("spreaded function argument count exceeds limit");
            }
            argList.add(nextArg, this, growBranch);
        }
    }

    @Override
    protected JavaScriptNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
        return create(context, cloneUninitialized(getIteratorNode, materializedTags));
    }
}
