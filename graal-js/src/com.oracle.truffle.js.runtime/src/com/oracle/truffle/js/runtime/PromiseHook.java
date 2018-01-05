/*
 * Copyright (c) 2017, 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
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
