/*
 * Copyright (c) 2024, 2024, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.runtime;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.TruffleContext;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.exception.AbstractTruffleException;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.ExceptionType;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.js.builtins.GlobalBuiltins;
import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.nodes.access.PropertyGetNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.nodes.unary.IsCallableNode;
import com.oracle.truffle.js.runtime.builtins.JSArrayBufferObject;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSFunctionData;
import com.oracle.truffle.js.runtime.builtins.JSFunctionObject;
import com.oracle.truffle.js.runtime.objects.JSAttributes;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.Undefined;
import org.graalvm.collections.EconomicSet;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;

public final class WorkerAgent extends JSAgent {
    private static final SerializedData WAKE_UP_MESSAGE = new SerializedData(Undefined.instance);

    private final TruffleContext workerContext;
    private final BlockingQueue<SerializedData> outMessages = new LinkedBlockingQueue<>();
    private final BlockingQueue<SerializedData> inMessages = new LinkedBlockingQueue<>();
    private volatile boolean finished;

    @TruffleBoundary
    public WorkerAgent() {
        super(true);
        JSRealm realm = JavaScriptLanguage.getCurrentJSRealm();
        TruffleLanguage.Env parentEnv = realm.getEnv();
        TruffleContext truffleContext = parentEnv.newInnerContextBuilder().inheritAllAccess(true).build();
        this.workerContext = truffleContext;
    }

    @Override
    public String toString() {
        return "WorkerAgent{signifier=" + getSignifier() + "}";
    }

    @TruffleBoundary
    public void start(String code) {
        workerContext.initializePublic(null, JavaScriptLanguage.ID);

        JSRealm rlm = JavaScriptLanguage.getCurrentJSRealm();
        rlm.getAgent().registerChildAgent(this);

        Thread thread = rlm.getEnv().newTruffleThreadBuilder(new Runnable() {
            @Override
            public void run() {
                try {
                    JSRealm realm = JavaScriptLanguage.getCurrentJSRealm();
                    realm.setAgent(WorkerAgent.this);

                    if (realm.getContextOptions().isTestV8Mode()) {
                        findAndEvalV8Mockup();
                    }

                    Object postMessage = realm.lookupFunction(GlobalBuiltins.GLOBAL_WORKER, Strings.POST_MESSAGE);
                    JSObjectUtil.putDataProperty(realm.getGlobalObject(), Strings.POST_MESSAGE, postMessage, JSAttributes.getDefaultNotEnumerable());

                    Source workerSource = Source.newBuilder(JavaScriptLanguage.ID, code, "worker").build();
                    CallTarget callTarget = realm.getEnv().parsePublic(workerSource);
                    callTarget.call();
                    processAllPromises(true);

                    Object messageHandler = JSObject.get(realm.getGlobalObject(), Strings.ONMESSAGE);
                    if (JSRuntime.isCallable(messageHandler)) {
                        JSFunctionData functionData = realm.getContext().getOrCreateBuiltinFunctionData(JSContext.BuiltinFunctionKey.WorkerProcessMessage, (c) -> createProcessMessage(c));
                        JSFunctionObject processMessage = JSFunction.create(realm, functionData);

                        while (true) {
                            SerializedData message = inMessages.take();
                            if (message != WAKE_UP_MESSAGE) {
                                Object deserialized = message.deserialize(realm);
                                JSFunction.call(processMessage, Undefined.instance, new Object[]{deserialized});
                            }
                            processAllPromises(true);
                        }
                    }
                } catch (InterruptedException e) {
                } catch (AbstractTruffleException e) {
                    try {
                        ExceptionType type = InteropLibrary.getUncached(e).getExceptionType(e);
                        if (type != ExceptionType.INTERRUPT && type != ExceptionType.EXIT) {
                            System.err.println("Uncaught error from " + Thread.currentThread() + ": " + e.getMessage());
                        }
                    } catch (UnsupportedMessageException umex) {
                    }
                } finally {
                    markFinished();
                }
            }

            private static void findAndEvalV8Mockup() throws InterruptedException {
                Context context = Context.getCurrent();
                for (org.graalvm.polyglot.Source source : context.getEngine().getCachedSources()) {
                    if (source.getName().startsWith("v8mockup")) {
                        try {
                            context.eval(source);
                        } catch (PolyglotException ex) {
                            if (ex.isCancelled() || ex.isInterrupted()) {
                                // worker terminated during evaluation of v8mockup
                                throw new InterruptedException();
                            } else {
                                throw Errors.shouldNotReachHere(ex);
                            }
                        }
                        break;
                    }
                }
            }
        }).context(workerContext).build();

        thread.setName("Worker-Thread-" + getSignifier());
        thread.start();
    }

    private static JSFunctionData createProcessMessage(JSContext context) {
        class ProcessMessageNode extends JavaScriptRootNode {
            @Child PropertyGetNode getOnMessage = PropertyGetNode.create(Strings.ONMESSAGE, context);
            @Child IsCallableNode isCallable = IsCallableNode.create();
            @Child JSFunctionCallNode call = JSFunctionCallNode.createCall();

            @Override
            public Object execute(VirtualFrame frame) {
                Object onMessage = getOnMessage.getValue(getRealm().getGlobalObject());
                if (isCallable.executeBoolean(onMessage)) {
                    Object message = JSFrameUtil.getArgumentsArray(frame)[0];
                    call.executeCall(JSArguments.create(Undefined.instance, onMessage, message));
                }
                return Undefined.instance;
            }
        }
        return JSFunctionData.createCallOnly(context, new ProcessMessageNode().getCallTarget(), 1, Strings.EMPTY_STRING);
    }

    @Override
    public void wake() {
        inMessages.add(WAKE_UP_MESSAGE);
    }

    @TruffleBoundary
    @Override
    public void terminate() {
        markFinished();
        workerContext.closeCancelled(null, "worker terminated");
    }

    private void markFinished() {
        finished = true;
        // un-block potential getMessage()
        outMessages.add(new SerializedData(Undefined.instance));
    }

    @TruffleBoundary
    public void postInMessage(Object message, EconomicSet<JSArrayBufferObject> transferSet) {
        inMessages.add(new SerializedData(message, transferSet));
    }

    @TruffleBoundary
    public void postOutMessage(Object message) {
        outMessages.add(new SerializedData(message));
    }

    @TruffleBoundary
    public Object getOutMessage(JSRealm realm) {
        Object message = Undefined.instance;
        if (!finished || !outMessages.isEmpty()) {
            try {
                message = outMessages.take().deserialize(realm);
            } catch (InterruptedException iex) {
            }
        }
        return message;
    }

}
