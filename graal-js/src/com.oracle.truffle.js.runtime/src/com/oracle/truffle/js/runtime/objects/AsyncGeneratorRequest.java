/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.runtime.objects;

import com.oracle.truffle.api.object.DynamicObject;

public final class AsyncGeneratorRequest {
    private final int completionType;
    private final Object completionValue;
    private final DynamicObject promiseCapability;

    private AsyncGeneratorRequest(int completionType, Object completionValue, DynamicObject promiseCapability) {
        this.completionType = completionType;
        this.completionValue = completionValue;
        this.promiseCapability = promiseCapability;
    }

    public Completion getCompletion() {
        return new Completion(completionType, completionValue);
    }

    public Object getCompletionValue() {
        return completionValue;
    }

    public DynamicObject getPromiseCapability() {
        return promiseCapability;
    }

    public boolean isNormal() {
        return completionType == Completion.NORMAL;
    }

    public boolean isAbruptCompletion() {
        return completionType != Completion.NORMAL;
    }

    public boolean isReturn() {
        return completionType == Completion.RETURN;
    }

    public boolean isThrow() {
        return completionType == Completion.THROW;
    }

    public static AsyncGeneratorRequest create(Completion completion, DynamicObject promiseCapability) {
        return new AsyncGeneratorRequest(completion.type, completion.value, promiseCapability);
    }
}
