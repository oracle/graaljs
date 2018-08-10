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
package com.oracle.truffle.trufflenode.threading;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Map;
import java.util.HashMap;

import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.Undefined;

public class GraalJSInstanceRunner {

    static {
        if (Boolean.getBoolean("uv.loadsharedlibrary")) {
            System.loadLibrary("node");
            nodeGlobalInit();
        }
    }

    private final CountDownLatch startupBarrier;

    private long loop = -1;
    private JSContext context;

    public static final AtomicInteger newLoopId = new AtomicInteger(0);

    public GraalJSInstanceRunner(long loop) {
        this.startupBarrier = null;
        this.loop = loop;
    }

    public GraalJSInstanceRunner() {
        this.startupBarrier = null;
    }

    private GraalJSInstanceRunner(CountDownLatch cd) {
        this.startupBarrier = cd;
    }

    public void executeFile(String fileName) {
        runLoop(new String[]{fileName}, null);
    }

    public void executeFile(String fileName, Map<String, Object> env) {
        runLoop(new String[]{fileName}, env);
    }

    private static class NodeJSLoopActivationCallback {

        private final GraalJSInstanceRunner node;
        private final Map<String, Object> env;

        NodeJSLoopActivationCallback(GraalJSInstanceRunner node, Map<String, Object> env) {
            this.node = node;
            this.env = env;
        }

        @SuppressWarnings("unused")
        public void init(Object proc, long loopPtr) {
            DynamicObject process = (DynamicObject) proc;
            JSContext processContext = JSObject.getJSContext(process);
            DynamicObject newEnv = JSObject.create(processContext, processContext.getEmptyShape());
            // Copy the original proxy-based env into the new threads' process.env
            if (env != null) {
                for (String k : env.keySet()) {
                    JSObject.set(newEnv, k, env.get(k), false);
                }
            }
            JSObject.set(process, "env", newEnv, false);
            node.loop = loopPtr;
            node.context = processContext;
            // JSObject.getJSContext(process).setNodeServer(node);
            if (node.startupBarrier != null) {
                node.startupBarrier.countDown();
            }

        }

    }

    private void runLoop(String[] args, Map<String, Object> env) {
        nodeRunLoop(args, new NodeJSLoopActivationCallback(this, env));
    }

    public long getLoopPtr() {
        assert loop != 0;
        return loop;
    }

    public JSContext getJSContext() {
        assert context != null;
        return context;
    }

    public GraalJSAsyncHandle newAsyncHandle() {
        return new GraalJSAsyncHandle(this);
    }

    public static void startInCurrentThread(String file) {
        startInCurrentThread(file, Undefined.instance);
    }

    public static void startInCurrentThread(String file, DynamicObject env) {
        new GraalJSInstanceRunner().runLoop(new String[]{file}, getKeyValues(env));
    }

    public static void startInNewThread(String file) throws InterruptedException {
        startInNewThread(new String[]{file}, Undefined.instance);
    }

    public static void startInNewThread(String[] args, DynamicObject env) throws InterruptedException {
        CountDownLatch cd = new CountDownLatch(1);
        Map<String, Object> mapEnv = getKeyValues(env);
        GraalJSInstanceRunner instance = new GraalJSInstanceRunner(cd);
        Thread nodeThread = new Thread(new Runnable() {

            @Override
            public void run() {
                instance.runLoop(args, mapEnv);
            }
        });
        nodeThread.start();
        cd.await();
        // TODO: when/how to nodeThread.join();
    }

    public interface GraalJSAsyncHandleCallback {

        void send();
    }

    public static class GraalJSAsyncHandle {

        private final GraalJSInstanceRunner instance;
        private long handle;

        private GraalJSAsyncHandleCallback onSend;

        public GraalJSAsyncHandle(GraalJSInstanceRunner instance) {
            this.instance = instance;
        }

        public void send() {
            assert onSend != null;
            instance.nodeAsyncHandleSend(handle);
        }

        public void setLambda(GraalJSAsyncHandleCallback lambda) {
            if (this.onSend != null) {
                throw new RuntimeException("Handles callback can be set only once!");
            }
            this.onSend = lambda;
            this.handle = instance.nodeRegisterHandle(instance.getLoopPtr(), onSend);
        }

        public void close() {
            instance.nodeAsyncHandleDispose(handle);
        }
    }

    private static Map<String, Object> getKeyValues(DynamicObject from) {
        Map<String, Object> result = new HashMap<>();
        for (Object k : JSObject.ownPropertyKeys(from)) {
            if (JSRuntime.isString(k)) {
                String key = JSRuntime.toString(k);
                Object value = JSObject.get(from, key);
                if (!(value instanceof DynamicObject)) {
                    result.put(key, value);
                }
            }
        }
        return result;
    }

    public void submitNextTick(@SuppressWarnings("unused") Runnable runnable) {
        throw new UnsupportedOperationException("TODO");
    }

    /* Node.js runtime interaction */
    private static native void nodeGlobalInit();

    private native long nodeRunLoop(String[] args, NodeJSLoopActivationCallback lambda);

    private native long nodeRegisterHandle(long loopPtr, GraalJSAsyncHandleCallback lambda);

    private native void nodeAsyncHandleSend(long handlePtr);

    private native void nodeAsyncHandleDispose(long handlePtr);

}
