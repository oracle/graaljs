/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.access.JSReadFrameSlotNode;
import com.oracle.truffle.js.nodes.control.AsyncDisposeResourcesNode.AsyncDisposeState;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.objects.Completion;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.DisposeCapability;

public final class AsyncDisposeResourcesWrapperNode extends AbstractAwaitNode implements ResumableNode.WithObjectState {
    @Child private JavaScriptNode capabilityNode;
    @Child private JavaScriptNode errorNode;
    @Child private AsyncDisposeResourcesNode asyncDisposeResourcesNode;

    private AsyncDisposeResourcesWrapperNode(JSContext context, int stateSlot, JavaScriptNode capabilityNode, JavaScriptNode errorNode,
                    JSReadFrameSlotNode asyncContextNode, JSReadFrameSlotNode asyncResultNode) {
        super(context, stateSlot, null, asyncContextNode, asyncResultNode);
        this.capabilityNode = capabilityNode;
        this.errorNode = errorNode;
        this.asyncDisposeResourcesNode = AsyncDisposeResourcesNode.create(context);
    }

    public static JavaScriptNode create(JSContext context, int stateSlot, JavaScriptNode capabilityNode, JavaScriptNode errorNode,
                    JSReadFrameSlotNode asyncContextNode, JSReadFrameSlotNode asyncResultNode) {
        return new AsyncDisposeResourcesWrapperNode(context, stateSlot, capabilityNode, errorNode, asyncContextNode, asyncResultNode);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        Object savedState = getState(frame, stateSlot);
        AsyncDisposeState state;
        if (savedState == Undefined.instance) {
            DisposeCapability capability = (DisposeCapability) capabilityNode.execute(frame);
            Object errorObject = errorNode.execute(frame);
            state = AsyncDisposeResourcesNode.createState(capability, errorObject);
        } else {
            state = (AsyncDisposeState) savedState;
            Completion completion = resumeAwaitCompletion(frame);
            if (completion.isThrow()) {
                asyncDisposeResourcesNode.rejectAwait(state, completion.getValue());
            } else {
                assert completion.isNormal();
            }
        }

        Object promiseOrValue = asyncDisposeResourcesNode.disposeUntilAwait(state);
        if (promiseOrValue == null) {
            resetState(frame, stateSlot);
            asyncDisposeResourcesNode.complete(state);
            return Undefined.instance;
        }

        setState(frame, stateSlot, state);
        return suspendAwait(frame, promiseOrValue);
    }

    @Override
    protected JavaScriptNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
        return create(context, stateSlot,
                        cloneUninitialized(capabilityNode, materializedTags),
                        cloneUninitialized(errorNode, materializedTags),
                        cloneUninitialized(readAsyncContextNode, materializedTags),
                        cloneUninitialized(readAsyncResultNode, materializedTags));
    }
}
