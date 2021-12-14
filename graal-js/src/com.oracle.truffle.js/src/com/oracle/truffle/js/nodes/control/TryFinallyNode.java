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
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.objects.Undefined;

/**
 * 12.14 The try Statement.
 */
@NodeInfo(shortName = "try-finally")
public class TryFinallyNode extends StatementNode implements ResumableNode.WithObjectState {

    @Child private JavaScriptNode tryBlock;
    @Child private JavaScriptNode finallyBlock;
    @Child private InteropLibrary exceptions;

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
        Object result = null;
        Throwable throwable;
        try {
            result = tryBlock.execute(frame);
            throwable = null;
        } catch (ControlFlowException cfe) {
            throwable = cfe;
        } catch (Throwable ex) {
            if (TryCatchNode.shouldCatch(ex, exceptions())) {
                throwable = ex;
            } else {
                // skip finally block
                throw ex;
            }
        }

        finallyBlock.executeVoid(frame);

        if (throwable != null) {
            throw JSRuntime.rethrow(throwable);
        }
        assert result != null;
        return result;
    }

    @Override
    public void executeVoid(VirtualFrame frame) {
        Throwable throwable;
        try {
            tryBlock.executeVoid(frame);
            throwable = null;
        } catch (ControlFlowException cfe) {
            throwable = cfe;
        } catch (Throwable ex) {
            if (TryCatchNode.shouldCatch(ex, exceptions())) {
                throwable = ex;
            } else {
                // skip finally block
                throw ex;
            }
        }

        finallyBlock.executeVoid(frame);

        if (throwable != null) {
            throw JSRuntime.rethrow(throwable);
        }
    }

    @Override
    public Object resume(VirtualFrame frame, int stateSlot) {
        Object result = EMPTY;
        Throwable throwable = null;
        Object state = getStateAndReset(frame, stateSlot);
        if (state == Undefined.instance) {
            try {
                result = tryBlock.execute(frame);
            } catch (YieldException e) {
                // not executing finally block
                throw e;
            } catch (ControlFlowException cfe) {
                throwable = cfe;
            } catch (Throwable ex) {
                if (TryCatchNode.shouldCatch(ex, exceptions())) {
                    throwable = ex;
                } else {
                    // skip finally block
                    throw ex;
                }
            }
        } else {
            // resuming into finally block
            if (state instanceof Throwable) {
                throwable = (Throwable) state;
            }
        }

        try {
            finallyBlock.execute(frame);
        } catch (YieldException e) {
            setState(frame, stateSlot, throwable);
            throw e;
        }

        if (throwable != null) {
            throw JSRuntime.rethrow(throwable);
        }
        // Since we're in a generator function, we may ignore the result and return undefined;
        // otherwise we'd have to remember the result when yielding from the finally block.
        return result;
    }

    private InteropLibrary exceptions() {
        InteropLibrary e = exceptions;
        if (e == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            exceptions = e = insert(InteropLibrary.getFactory().createDispatched(JSConfig.InteropLibraryLimit));
        }
        return e;
    }
}
