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
package com.oracle.truffle.js.nodes.promise;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.runtime.GraalJSException;
import com.oracle.truffle.js.runtime.JSAgent;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.builtins.JSError;
import com.oracle.truffle.js.runtime.builtins.JSErrorObject;
import com.oracle.truffle.js.runtime.builtins.JSPromise;
import com.oracle.truffle.js.runtime.builtins.JSPromiseObject;

public class RejectPromiseNode extends JavaScriptBaseNode {
    private final JSContext context;
    @Child private TriggerPromiseReactionsNode triggerPromiseReactions;
    private final ConditionProfile unhandledProf = ConditionProfile.create();

    protected RejectPromiseNode(JSContext context) {
        this.context = context;
        this.triggerPromiseReactions = TriggerPromiseReactionsNode.create(context);
    }

    public static RejectPromiseNode create(JSContext context) {
        return new RejectPromiseNode(context);
    }

    public Object execute(JSPromiseObject promise, Object reason) {
        assert JSPromise.isPending(promise);

        if (!JSConfig.EagerStackTrace && context.isOptionAsyncStackTraces() && JSError.isJSError(reason)) {
            // Materialize lazy stack trace before clearing promise reactions.
            materializeLazyStackTrace((JSErrorObject) reason);
        }

        var reactions = promise.getPromiseRejectReactions();
        promise.setPromiseResult(reason);
        promise.clearPromiseReactions();
        JSPromise.setPromiseState(promise, JSPromise.REJECTED);
        if (unhandledProf.profile(!promise.isHandled())) {
            context.notifyPromiseRejectionTracker(promise, JSPromise.REJECTION_TRACKER_OPERATION_REJECT, reason, JSAgent.get(this));
        }
        return triggerPromiseReactions.execute(reactions, reason);
    }

    @TruffleBoundary
    private static void materializeLazyStackTrace(JSErrorObject error) {
        assert JSError.isJSError(error);
        GraalJSException exception = JSError.getException(error);
        if (exception != null) {
            exception.getJSStackTrace();
        }
    }
}
