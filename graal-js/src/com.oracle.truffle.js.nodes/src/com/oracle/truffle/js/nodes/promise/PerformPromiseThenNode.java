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

import java.util.ArrayList;

import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.access.PropertyGetNode;
import com.oracle.truffle.js.nodes.access.PropertySetNode;
import com.oracle.truffle.js.nodes.unary.IsCallableNode;
import com.oracle.truffle.js.runtime.Boundaries;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.builtins.JSPromise;
import com.oracle.truffle.js.runtime.objects.PromiseCapabilityRecord;
import com.oracle.truffle.js.runtime.objects.PromiseReactionRecord;
import com.oracle.truffle.js.runtime.objects.Undefined;

public class PerformPromiseThenNode extends JavaScriptBaseNode {
    private final JSContext context;
    @Child private IsCallableNode isCallableFulfill = IsCallableNode.create();
    @Child private IsCallableNode isCallableReject = IsCallableNode.create();
    @Child private PropertyGetNode getPromiseState;
    @Child private PropertyGetNode getPromiseFulfillReactions;
    @Child private PropertyGetNode getPromiseRejectReactions;
    @Child private PropertyGetNode getPromiseResult;
    @Child private PropertyGetNode getPromiseIsHandled;
    @Child private PropertySetNode setPromiseIsHandled;
    @Child private PromiseReactionJobNode promiseReactionJob;
    private final ConditionProfile pendingProf = ConditionProfile.createBinaryProfile();
    private final ConditionProfile fulfilledProf = ConditionProfile.createBinaryProfile();

    protected PerformPromiseThenNode(JSContext context) {
        this.context = context;
        this.getPromiseState = PropertyGetNode.createGetHidden(JSPromise.PROMISE_STATE, context);
        this.getPromiseFulfillReactions = PropertyGetNode.createGetHidden(JSPromise.PROMISE_FULFILL_REACTIONS, context);
        this.getPromiseRejectReactions = PropertyGetNode.createGetHidden(JSPromise.PROMISE_REJECT_REACTIONS, context);
        this.getPromiseResult = PropertyGetNode.createGetHidden(JSPromise.PROMISE_RESULT, context);
        this.getPromiseIsHandled = PropertyGetNode.createGetHidden(JSPromise.PROMISE_IS_HANDLED, context);
        this.setPromiseIsHandled = PropertySetNode.createSetHidden(JSPromise.PROMISE_IS_HANDLED, context);
        this.promiseReactionJob = PromiseReactionJobNode.create(context);
    }

    public static PerformPromiseThenNode create(JSContext context) {
        return new PerformPromiseThenNode(context);
    }

    @SuppressWarnings("unchecked")
    public DynamicObject execute(DynamicObject promise, Object onFulfilled, Object onRejected, PromiseCapabilityRecord resultCapability) {
        assert JSPromise.isJSPromise(promise);
        DynamicObject onFulfilledHandler = isCallableFulfill.executeBoolean(onFulfilled) ? (DynamicObject) onFulfilled : Undefined.instance;
        DynamicObject onRejectedHandler = isCallableReject.executeBoolean(onRejected) ? (DynamicObject) onRejected : Undefined.instance;
        PromiseReactionRecord fulfillReaction = PromiseReactionRecord.create(resultCapability, onFulfilledHandler, true);
        PromiseReactionRecord rejectReaction = PromiseReactionRecord.create(resultCapability, onRejectedHandler, false);

        int promiseState = getPromiseState(promise);
        if (pendingProf.profile(promiseState == JSPromise.PENDING)) {
            Boundaries.listAdd((ArrayList<? super PromiseReactionRecord>) getPromiseFulfillReactions.getValue(promise), fulfillReaction);
            Boundaries.listAdd((ArrayList<? super PromiseReactionRecord>) getPromiseRejectReactions.getValue(promise), rejectReaction);
        } else if (fulfilledProf.profile(promiseState == JSPromise.FULFILLED)) {
            Object value = getPromiseResult.getValue(promise);
            DynamicObject job = promiseReactionJob.execute(fulfillReaction, value);
            context.promiseEnqueueJob(job);
        } else {
            assert promiseState == JSPromise.REJECTED;
            Object reason = getPromiseResult.getValue(promise);
            if (!getPromiseIsHandled(promise)) {
                context.notifyPromiseRejectionTracker(promise, JSPromise.REJECTION_TRACKER_OPERATION_HANDLE);
            }
            DynamicObject job = promiseReactionJob.execute(rejectReaction, reason);
            context.promiseEnqueueJob(job);
        }
        setPromiseIsHandled.setValueBoolean(promise, true);
        return resultCapability.getPromise();
    }

    private boolean getPromiseIsHandled(DynamicObject promise) {
        try {
            return getPromiseIsHandled.getValueBoolean(promise);
        } catch (UnexpectedResultException e) {
            throw Errors.shouldNotReachHere();
        }
    }

    private int getPromiseState(DynamicObject promise) {
        try {
            return getPromiseState.getValueInt(promise);
        } catch (UnexpectedResultException e) {
            throw Errors.shouldNotReachHere();
        }
    }
}
