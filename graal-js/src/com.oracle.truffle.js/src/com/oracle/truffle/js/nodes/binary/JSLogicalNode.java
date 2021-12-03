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
package com.oracle.truffle.js.nodes.binary;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.control.ResumableNode;
import com.oracle.truffle.js.nodes.control.YieldException;

public abstract class JSLogicalNode extends JSBinaryNode implements ResumableNode {

    private static final int RESUME_RIGHT = 1;
    private static final int RESUME_UNEXECUTED = 0;

    protected final ConditionProfile canShortCircuit = ConditionProfile.createBinaryProfile();

    protected JSLogicalNode(JavaScriptNode left, JavaScriptNode right) {
        super(left, right);
    }

    protected abstract boolean useLeftValue(Object leftValue);

    @Override
    public final Object execute(VirtualFrame frame) {
        Object leftValue = leftNode.execute(frame);
        if (canShortCircuit.profile(useLeftValue(leftValue))) {
            return leftValue;
        } else {
            return rightNode.execute(frame);
        }
    }

    @Override
    public Object resume(VirtualFrame frame) {
        int state = getStateAsIntAndReset(frame);
        if (state == RESUME_UNEXECUTED) {
            Object leftValue = leftNode.execute(frame);
            if (canShortCircuit.profile(useLeftValue(leftValue))) {
                return leftValue;
            } else {
                try {
                    return getRight().execute(frame);
                } catch (YieldException e) {
                    setState(frame, RESUME_RIGHT);
                    throw e;
                }
            }
        } else {
            assert state == RESUME_RIGHT;
            try {
                return rightNode.execute(frame);
            } catch (YieldException e) {
                setState(frame, RESUME_RIGHT);
                throw e;
            }
        }
    }

    @Override
    public boolean isResultAlwaysOfType(Class<?> clazz) {
        return getLeft().isResultAlwaysOfType(clazz) && getRight().isResultAlwaysOfType(clazz);
    }
}
