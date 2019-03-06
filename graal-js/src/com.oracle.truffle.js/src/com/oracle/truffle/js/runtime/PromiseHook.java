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
package com.oracle.truffle.js.runtime;

import com.oracle.truffle.api.object.DynamicObject;

/**
 * Provides information about the life-cycle of promises.
 */
public interface PromiseHook {
    /**
     * PromiseHook with type {@code TYPE_INIT} is called when a new promise is created.
     */
    int TYPE_INIT = 0;

    /**
     * PromiseHook with type {@code TYPE_RESOLVE} is called at the beginning of resolve or reject
     * function defined by {@code CreateResolvingFunctions}.
     */
    int TYPE_RESOLVE = 1;

    /**
     * PromiseHook with type {@code TYPE_BEFORE} is called at the beginning of the
     * {@code PromiseReactionJob}.
     */
    int TYPE_BEFORE = 2;

    /**
     * PromiseHook with type {@code TYPE_AFTER} is called right at the end of the
     * {@code PromiseReactionJob}.
     */
    int TYPE_AFTER = 3;

    /**
     * Invoked for each important change in the life-cycle of a promise.
     * 
     * @param changeType type of the change: {@link #TYPE_INIT}, {@link #TYPE_RESOLVE},
     *            {@link #TYPE_BEFORE} or {@link #TYPE_AFTER}.
     * @param promise promise being changed.
     * @param parentPromise parent promise when the promise is created ({@link #TYPE_INIT} change
     *            type) by {@code Promise.then/race/all} or {@code AsyncFunctionAwait}. The parent
     *            promise is {@code undefined} in all other cases.
     */
    void promiseChanged(int changeType, DynamicObject promise, DynamicObject parentPromise);

}
