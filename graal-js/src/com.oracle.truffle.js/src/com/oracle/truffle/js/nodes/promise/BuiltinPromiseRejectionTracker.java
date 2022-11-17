/*
 * Copyright (c) 2021, 2022, Oracle and/or its affiliates. All rights reserved.
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

import java.io.PrintWriter;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.WeakHashMap;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.InvalidArrayIndexException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSContextOptions;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.PromiseRejectionTracker;
import com.oracle.truffle.js.runtime.builtins.JSError;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.Undefined;

public class BuiltinPromiseRejectionTracker implements PromiseRejectionTracker {

    private final JSContext context;
    private final JSContextOptions.UnhandledRejectionsTrackingMode mode;

    public BuiltinPromiseRejectionTracker(JSContext context, JSContextOptions.UnhandledRejectionsTrackingMode mode) {
        assert mode != JSContextOptions.UnhandledRejectionsTrackingMode.NONE;
        this.context = context;
        this.mode = mode;
    }

    private final Set<JSDynamicObject> pendingUnhandledRejections = new LinkedHashSet<>();
    private final Deque<PromiseChainInfoRecord> asyncHandledRejections = new LinkedList<>();
    private final Map<JSDynamicObject, PromiseChainInfoRecord> maybeUnhandledPromises = new WeakHashMap<>();

    @Override
    public void promiseRejected(JSDynamicObject promise, Object reason) {
        CompilerAsserts.neverPartOfCompilation();
        maybeUnhandledPromises.put(promise, new PromiseChainInfoRecord(reason, false));
        pendingUnhandledRejections.add(promise);
        context.getLanguage().getPromiseJobsQueueEmptyAssumption().invalidate("Potential unhandled rejection");
    }

    @Override
    public void promiseRejectionHandled(JSDynamicObject promise) {
        CompilerAsserts.neverPartOfCompilation();
        PromiseChainInfoRecord promiseInfo = maybeUnhandledPromises.get(promise);
        if (promiseInfo != null) {
            maybeUnhandledPromises.remove(promise);
            pendingUnhandledRejections.remove(promise);
            if (promiseInfo.warned) {
                asyncHandledRejections.add(promiseInfo);
            }
        }
    }

    @Override
    public void promiseRejectedAfterResolved(JSDynamicObject promise, Object value) {
    }

    @Override
    public void promiseResolvedAfterResolved(JSDynamicObject promise, Object value) {
    }

    @Override
    public void promiseReactionJobsProcessed() {
        CompilerAsserts.neverPartOfCompilation();
        JSRealm realm = JSRealm.get(null);
        assert realm.getContext() == context;

        while (!asyncHandledRejections.isEmpty()) {
            PromiseChainInfoRecord info = asyncHandledRejections.removeFirst();
            if (mode == JSContextOptions.UnhandledRejectionsTrackingMode.WARN) {
                PrintWriter out = realm.getErrorWriter();
                out.println("[GraalVM JavaScript Warning] Promise rejection was handled asynchronously: " + formatError(info.reason));
                out.flush();
            }
        }

        // Take one at a time as the rejection handler could queue up more rejections.
        while (!pendingUnhandledRejections.isEmpty()) {
            JSDynamicObject unhandledPromise = pendingUnhandledRejections.iterator().next();
            pendingUnhandledRejections.remove(unhandledPromise);

            PromiseChainInfoRecord info = maybeUnhandledPromises.get(unhandledPromise);
            if (info == null) {
                continue;
            }
            info.warned = true;
            if (mode == JSContextOptions.UnhandledRejectionsTrackingMode.HANDLER) {
                Object handler = realm.getUnhandledPromiseRejectionHandler();
                if (handler != null) {
                    JSRuntime.call(handler, Undefined.instance, new Object[]{info.reason, unhandledPromise});
                }
            } else if (mode == JSContextOptions.UnhandledRejectionsTrackingMode.WARN) {
                PrintWriter out = realm.getErrorWriter();
                out.println("[GraalVM JavaScript Warning] Unhandled promise rejection: " + formatError(info.reason));
                out.flush();
            } else {
                assert mode == JSContextOptions.UnhandledRejectionsTrackingMode.THROW;
                // Rethrow original exception, if possible.
                InteropLibrary interop = InteropLibrary.getUncached(info.reason);
                if (interop.isException(info.reason)) {
                    try {
                        interop.throwException(info.reason);
                    } catch (UnsupportedMessageException e) {
                    }
                }
                throw Errors.createError("Unhandled promise rejection: " + formatError(info.reason));
            }
        }
    }

    private static String formatError(Object error) {
        CompilerAsserts.neverPartOfCompilation();
        InteropLibrary interopExc = InteropLibrary.getUncached(error);
        InteropLibrary interopStr = InteropLibrary.getUncached();
        if (interopExc.isException(error)) {
            try {
                String message = null;
                if (interopExc.hasExceptionMessage(error)) {
                    message = interopStr.asString(interopExc.getExceptionMessage(error));
                }
                StringBuilder sb = new StringBuilder();
                sb.append(Objects.requireNonNullElse(message, "Error"));

                if (interopExc.hasExceptionStackTrace(error)) {
                    Object stackTrace = interopExc.getExceptionStackTrace(error);
                    InteropLibrary interopST = InteropLibrary.getUncached(stackTrace);
                    long length = interopST.getArraySize(stackTrace);
                    for (long i = 0; i < length; i++) {
                        Object stackTraceElement = interopST.readArrayElement(stackTrace, i);
                        InteropLibrary interopSTE = InteropLibrary.getUncached(stackTraceElement);

                        String name = null;
                        SourceSection sourceLocation = null;
                        if (interopSTE.hasExecutableName(stackTraceElement)) {
                            name = interopStr.asString(interopSTE.getExecutableName(stackTraceElement));
                        }
                        if (interopSTE.hasSourceLocation(stackTraceElement)) {
                            sourceLocation = interopSTE.getSourceLocation(stackTraceElement);
                        }

                        if (name == null && sourceLocation == null) {
                            continue;
                        }

                        sb.append('\n');
                        sb.append("    at ");
                        sb.append(Objects.requireNonNullElse(name, JSError.ANONYMOUS_FUNCTION_NAME));
                        if (sourceLocation != null) {
                            sb.append(" (").append(formatSourceLocation(sourceLocation)).append(")");
                        }
                    }
                }
                return sb.toString();
            } catch (UnsupportedMessageException | InvalidArrayIndexException e) {
                assert false : e;
            }
        }

        return JSRuntime.safeToString(error).toString();
    }

    private static String formatSourceLocation(SourceSection sourceSection) {
        if (sourceSection == null) {
            return "Unknown";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(sourceSection.getSource().getName());

        sb.append(":");
        sb.append(sourceSection.getStartLine());
        if (sourceSection.getStartLine() < sourceSection.getEndLine()) {
            sb.append("-").append(sourceSection.getEndLine());
        }
        sb.append(":");
        sb.append(sourceSection.getCharIndex());
        if (sourceSection.getCharLength() > 1) {
            sb.append("-").append(sourceSection.getCharEndIndex() - 1);
        }
        return sb.toString();
    }

    private static class PromiseChainInfoRecord {

        private final Object reason;
        private boolean warned;

        PromiseChainInfoRecord(Object reason, boolean warned) {
            this.reason = reason;
            this.warned = warned;
        }
    }
}
