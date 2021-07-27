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
package com.oracle.truffle.js.nodes.promise;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.access.PropertyGetNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.UserScriptException;
import com.oracle.truffle.js.runtime.builtins.JSPromise;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;

@ImportStatic(JSPromise.class)
public abstract class UnwrapPromiseNode extends JavaScriptBaseNode {
    @Child private PropertyGetNode getPromiseResult;

    private static final UnwrapPromiseNode UNCACHED = UnwrapPromiseNodeGen.create(null);

    protected UnwrapPromiseNode(JSContext context) {
        if (context != null) {
            this.getPromiseResult = PropertyGetNode.createGetHidden(JSPromise.PROMISE_RESULT, context);
        }
    }

    public static UnwrapPromiseNode create() {
        return UnwrapPromiseNodeGen.create(JavaScriptLanguage.get(null).getJSContext());
    }

    public final Object execute(DynamicObject promise) {
        if (getPromiseResult == null) {
            return doUncached(promise);
        }
        int promiseState = JSPromise.getPromiseState(promise);
        Object promiseResult = getPromiseResult.getValue(promise);
        return execute(promise, promiseState, promiseResult);
    }

    @TruffleBoundary
    private Object doUncached(DynamicObject promise) {
        return execute(promise, JSPromise.getPromiseState(promise), JSDynamicObject.getOrNull(promise, JSPromise.PROMISE_RESULT));
    }

    protected abstract Object execute(DynamicObject promise, int promiseState, Object promiseResult);

    @SuppressWarnings("unused")
    @Specialization(guards = "promiseState == FULFILLED")
    protected static Object fulfilled(DynamicObject promise, int promiseState, Object promiseResult) {
        return promiseResult;
    }

    @SuppressWarnings("unused")
    @Specialization(guards = "promiseState == REJECTED")
    protected static Object rejected(DynamicObject promise, int promiseState, Object promiseResult) {
        throw UserScriptException.create(promiseResult);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = "promiseState == PENDING")
    protected static Object pending(DynamicObject promise, int promiseState, Object promiseResult) {
        throw Errors.createTypeError("Attempt to unwrap pending promise");
    }

    public static UnwrapPromiseNode getUncached() {
        return UNCACHED;
    }
}
