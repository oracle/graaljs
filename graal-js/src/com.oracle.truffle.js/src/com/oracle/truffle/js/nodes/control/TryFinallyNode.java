/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.nodes.ControlFlowException;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.objects.Undefined;

import java.util.Set;

/**
 * 12.14 The try Statement.
 */
@NodeInfo(shortName = "try-finally")
public class TryFinallyNode extends StatementNode implements ResumableNode {

    @Child private JavaScriptNode tryBlock;
    @Child private JavaScriptNode finallyBlock;
    private final BranchProfile catchBranch = BranchProfile.create();
    private final ValueProfile typeProfile = ValueProfile.createClassProfile();

    TryFinallyNode(JavaScriptNode tryBlock, JavaScriptNode finallyBlock) {
        this.tryBlock = tryBlock;
        this.finallyBlock = finallyBlock;
    }

    public static JavaScriptNode create(JavaScriptNode tryBlock, JavaScriptNode finallyBlock) {
        return new TryFinallyNode(tryBlock, finallyBlock);
    }

    @Override
    protected JavaScriptNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
        return create(cloneUninitialized(tryBlock, materializedTags), cloneUninitialized(finallyBlock, materializedTags));
    }

    @Override
    public Object execute(VirtualFrame frame) {
        Object result;
        boolean abort = false;
        try {
            result = tryBlock.execute(frame);
        } catch (ControlFlowException cfe) {
            throw cfe;
        } catch (Throwable ex) {
            catchBranch.enter();
            if (!TryCatchNode.shouldCatch(ex, typeProfile)) {
                abort = true;
            }
            throw ex;
        } finally {
            if (!abort) {
                finallyBlock.execute(frame);
            }
        }
        return result;
    }

    @Override
    public Object resume(VirtualFrame frame) {
        Object state = getStateAndReset(frame);
        if (state == Undefined.instance) {
            Object result;
            boolean yieldedInTry = false;
            boolean abort = false;
            Throwable ex = null;
            try {
                try {
                    result = tryBlock.execute(frame);
                } catch (YieldException e) {
                    yieldedInTry = true;
                    throw e;
                } catch (ControlFlowException cfe) {
                    ex = cfe;
                    throw cfe;
                } catch (Throwable ex2) {
                    catchBranch.enter();
                    if (!TryCatchNode.shouldCatch(ex2, typeProfile)) {
                        abort = true;
                    }
                    ex = ex2;
                    throw ex2;
                }
            } finally {
                if (!abort && !yieldedInTry) {
                    try {
                        finallyBlock.execute(frame);
                    } catch (YieldException e) {
                        setState(frame, ex);
                        throw e;
                    }
                }
            }
            return result;
        } else {
            try {
                finallyBlock.execute(frame);
            } catch (YieldException e) {
                setState(frame, state);
                throw e;
            }
            if (state != null) {
                JSRuntime.rethrow((Throwable) state);
            }
            return EMPTY;
        }
    }
}
