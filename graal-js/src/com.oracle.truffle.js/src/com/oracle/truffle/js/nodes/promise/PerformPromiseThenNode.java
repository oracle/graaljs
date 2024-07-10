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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.NeverDefault;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.InlinedBranchProfile;
import com.oracle.truffle.api.profiles.InlinedConditionProfile;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.unary.IsCallableNode;
import com.oracle.truffle.js.runtime.JSAgent;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JobCallback;
import com.oracle.truffle.js.runtime.builtins.JSFunctionObject;
import com.oracle.truffle.js.runtime.builtins.JSPromise;
import com.oracle.truffle.js.runtime.builtins.JSPromiseObject;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.PromiseCapabilityRecord;
import com.oracle.truffle.js.runtime.objects.PromiseReactionRecord;
import com.oracle.truffle.js.runtime.objects.Undefined;

public abstract class PerformPromiseThenNode extends JavaScriptBaseNode {
    private final JSContext context;
    @Child private IsCallableNode isCallableFulfillNode = IsCallableNode.create();
    @Child private IsCallableNode isCallableRejectNode = IsCallableNode.create();
    @Child private PromiseReactionJobNode promiseReactionJobNode;

    protected PerformPromiseThenNode(JSContext context) {
        this.context = context;
    }

    @NeverDefault
    public static PerformPromiseThenNode create(JSContext context) {
        return PerformPromiseThenNodeGen.create(context);
    }

    public final JSDynamicObject execute(JSPromiseObject promise, Object onFulfilled, Object onRejected) {
        return execute(promise, onFulfilled, onRejected, null);
    }

    public abstract JSDynamicObject execute(JSPromiseObject promise, Object onFulfilled, Object onRejected, PromiseCapabilityRecord resultCapability);

    @Specialization
    protected JSDynamicObject promiseThen(JSPromiseObject promise, Object onFulfilled, Object onRejected, PromiseCapabilityRecord resultCapability,
                    @Cached InlinedConditionProfile pendingProf,
                    @Cached InlinedConditionProfile fulfilledProf,
                    @Cached InlinedConditionProfile unhandledProf,
                    @Cached InlinedBranchProfile growProfile) {
        JSRealm realm = getRealm();
        JSAgent agent = realm.getAgent();
        JobCallback onFulfilledHandler = isCallableFulfillNode.executeBoolean(onFulfilled) ? agent.hostMakeJobCallback(onFulfilled) : null;
        JobCallback onRejectedHandler = isCallableRejectNode.executeBoolean(onRejected) ? agent.hostMakeJobCallback(onRejected) : null;
        assert resultCapability != null || (onFulfilledHandler != null && onRejectedHandler != null);
        PromiseReactionRecord fulfillReaction = PromiseReactionRecord.create(resultCapability, onFulfilledHandler, true);
        PromiseReactionRecord rejectReaction = PromiseReactionRecord.create(resultCapability, onRejectedHandler, false);

        int promiseState = JSPromise.getPromiseState(promise);
        if (pendingProf.profile(this, promiseState == JSPromise.PENDING)) {
            promise.getPromiseFulfillReactions().add(fulfillReaction, this, growProfile);
            promise.getPromiseRejectReactions().add(rejectReaction, this, growProfile);
        } else if (fulfilledProf.profile(this, promiseState == JSPromise.FULFILLED)) {
            Object value = promise.getPromiseResult();
            assert value != null;
            JSFunctionObject job = getPromiseReactionJob(fulfillReaction, value);
            context.enqueuePromiseJob(realm, job);
        } else {
            assert promiseState == JSPromise.REJECTED;
            Object reason = promise.getPromiseResult();
            assert reason != null;
            if (unhandledProf.profile(this, !promise.isHandled())) {
                context.notifyPromiseRejectionTracker(promise, JSPromise.REJECTION_TRACKER_OPERATION_HANDLE, Undefined.instance, agent);
            }
            JSFunctionObject job = getPromiseReactionJob(rejectReaction, reason);
            context.enqueuePromiseJob(realm, job);
        }
        promise.setIsHandled(true);
        if (resultCapability == null) {
            return Undefined.instance;
        }
        return resultCapability.getPromise();
    }

    private JSFunctionObject getPromiseReactionJob(PromiseReactionRecord reaction, Object value) {
        if (promiseReactionJobNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            promiseReactionJobNode = insert(PromiseReactionJobNode.create(context));
        }
        return promiseReactionJobNode.execute(reaction, value);
    }
}
