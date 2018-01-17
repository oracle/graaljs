/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.runtime.truffleinterop;

import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.runtime.objects.JSObject;

public final class InteropBoundFunction implements TruffleObject {
    private final DynamicObject function;
    private final Object receiver;

    public InteropBoundFunction(DynamicObject function, Object receiver) {
        this.function = function;
        this.receiver = receiver;
    }

    public DynamicObject getFunction() {
        return function;
    }

    public Object getReceiver() {
        return receiver;
    }

    public static boolean isInstance(TruffleObject object) {
        return object instanceof InteropBoundFunction;
    }

    @Override
    public ForeignAccess getForeignAccess() {
        return JSObject.getJSContext(function).getInteropRuntime().getInteropObjectForeignAccess();
    }
}
