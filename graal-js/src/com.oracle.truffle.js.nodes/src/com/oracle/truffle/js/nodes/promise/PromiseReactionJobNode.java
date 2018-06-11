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
package com.oracle.truffle.js.nodes.promise;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.access.PropertyGetNode;
import com.oracle.truffle.js.nodes.access.PropertySetNode;
import com.oracle.truffle.js.nodes.control.TryCatchNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSFrameUtil;
import com.oracle.truffle.js.runtime.JavaScriptRootNode;
import com.oracle.truffle.js.runtime.PromiseHook;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSFunctionData;
import com.oracle.truffle.js.runtime.objects.PromiseCapabilityRecord;
import com.oracle.truffle.js.runtime.objects.PromiseReactionRecord;
import com.oracle.truffle.js.runtime.objects.Undefined;

public class PromiseReactionJobNode extends JavaScriptBaseNode {
    static final HiddenKey REACTION_KEY = new HiddenKey("Reaction");
    static final HiddenKey ARGUMENT_KEY = new HiddenKey("Argument");

    private final JSContext context;
    @Child private PropertySetNode setReaction;
    @Child private PropertySetNode setArgument;

    protected PromiseReactionJobNode(JSContext context) {
        this.context = context;
        this.setReaction = PropertySetNode.createSetHidden(REACTION_KEY, context);
        this.setArgument = PropertySetNode.createSetHidden(ARGUMENT_KEY, context);
    }

    public static PromiseReactionJobNode create(JSContext context) {
        return new PromiseReactionJobNode(context);
    }

    public DynamicObject execute(Object reaction, Object argument) {
        JSFunctionData functionData = context.getOrCreateBuiltinFunctionData(JSContext.BuiltinFunctionKey.PromiseReactionJob, (c) -> createPromiseReactionJobImpl(c));
        DynamicObject function = JSFunction.create(context.getRealm(), functionData);
        setReaction.setValue(function, reaction);
        setArgument.setValue(function, argument);
        return function;
    }

    private static JSFunctionData createPromiseReactionJobImpl(JSContext context) {
        class PromiseReactionJob extends JavaScriptRootNode {
            @Child private PropertyGetNode getReaction = PropertyGetNode.createGetHidden(REACTION_KEY, context);
            @Child private PropertyGetNode getArgument = PropertyGetNode.createGetHidden(ARGUMENT_KEY, context);
            @Child private JSFunctionCallNode callResolveNode;
            @Child private JSFunctionCallNode callRejectNode;
            @Child private JSFunctionCallNode callHandlerNode;
            @Child private TryCatchNode.GetErrorObjectNode getErrorObjectNode;
            private final ConditionProfile handlerProf = ConditionProfile.createBinaryProfile();

            @Override
            public Object execute(VirtualFrame frame) {
                DynamicObject functionObject = JSFrameUtil.getFunctionObject(frame);
                PromiseReactionRecord reaction = (PromiseReactionRecord) getReaction.getValue(functionObject);
                Object argument = getArgument.getValue(functionObject);

                PromiseCapabilityRecord promiseCapability = reaction.getCapability();
                DynamicObject handler = reaction.getHandler();

                context.notifyPromiseHook(PromiseHook.TYPE_BEFORE, promiseCapability.getPromise());

                Object resolve = promiseCapability.getResolve();
                Object reject = promiseCapability.getReject();
                Object status;
                if (handlerProf.profile(handler == Undefined.instance)) {
                    if (reaction.isFulfill()) {
                        status = callResolve().executeCall(JSArguments.createOneArg(Undefined.instance, resolve, argument));
                    } else {
                        assert reaction.isReject();
                        status = callReject().executeCall(JSArguments.createOneArg(Undefined.instance, reject, argument));
                    }
                } else {
                    Object handlerResult;
                    Object resolutionFn;
                    try {
                        handlerResult = callHandler().executeCall(JSArguments.createOneArg(Undefined.instance, handler, argument));
                        resolutionFn = resolve;
                    } catch (Throwable ex) {
                        if (shouldCatch(ex)) {
                            handlerResult = getErrorObjectNode.execute(ex);
                            resolutionFn = reject;
                        } else {
                            throw ex;
                        }
                    }
                    status = callResolve().executeCall(JSArguments.createOneArg(Undefined.instance, resolutionFn, handlerResult));
                }

                context.notifyPromiseHook(PromiseHook.TYPE_AFTER, promiseCapability.getPromise());
                return status;
            }

            private boolean shouldCatch(Throwable exception) {
                if (getErrorObjectNode == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    getErrorObjectNode = insert(TryCatchNode.GetErrorObjectNode.create(context));
                }
                return TryCatchNode.shouldCatch(exception);
            }

            private JSFunctionCallNode callResolve() {
                if (callResolveNode == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    callResolveNode = insert(JSFunctionCallNode.createCall());
                }
                return callResolveNode;
            }

            private JSFunctionCallNode callReject() {
                if (callRejectNode == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    callRejectNode = insert(JSFunctionCallNode.createCall());
                }
                return callRejectNode;
            }

            private JSFunctionCallNode callHandler() {
                if (callHandlerNode == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    callHandlerNode = insert(JSFunctionCallNode.createCall());
                }
                return callHandlerNode;
            }
        }
        CallTarget callTarget = Truffle.getRuntime().createCallTarget(new PromiseReactionJob());
        return JSFunctionData.createCallOnly(context, callTarget, 0, "");
    }
}
