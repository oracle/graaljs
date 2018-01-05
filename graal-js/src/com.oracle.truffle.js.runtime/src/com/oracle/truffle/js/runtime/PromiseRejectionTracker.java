/*
 * Copyright (c) 2017, 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.runtime;

import com.oracle.truffle.api.object.DynamicObject;

/**
 * Promise rejection tracker is invoked when a promise is rejected without any handler or when a
 * handler is added to a rejected promise for the first time.
 */
public interface PromiseRejectionTracker {

    /**
     * Invoked when a promise is rejected without any handler.
     * 
     * @param promise rejected promise.
     */
    void promiseRejected(DynamicObject promise);

    /**
     * Invoked when a handler is added to a rejected promise for the first time.
     * 
     * @param promise rejected promise.
     */
    void promiseRejectionHandled(DynamicObject promise);

}
