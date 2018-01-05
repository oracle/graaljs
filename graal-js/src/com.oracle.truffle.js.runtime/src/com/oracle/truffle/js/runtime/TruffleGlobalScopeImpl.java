/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.runtime;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.interop.*;

public final class TruffleGlobalScopeImpl {
    private final TruffleLanguage.Env env;
    private final FrameDescriptor descriptor = new FrameDescriptor();
    private final MaterializedFrame globalFrame = Truffle.getRuntime().createMaterializedFrame(new Object[0], descriptor);

    public TruffleGlobalScopeImpl(TruffleLanguage.Env env) {
        this.env = env;
    }

    @TruffleBoundary
    public void exportTruffleObject(Object identifier, TruffleObject object) {
        FrameSlot slot = descriptor.findOrAddFrameSlot(identifier);
        globalFrame.setObject(slot, object);
        env.exportSymbol((String) identifier, object);
    }

    @TruffleBoundary
    public boolean contains(Object identifier) {
        return descriptor.findFrameSlot(identifier) != null;
    }

    @TruffleBoundary
    public Object getTruffleObject(Object identifier) {
        return getTruffleObject(identifier, true);
    }

    @TruffleBoundary
    public Object getTruffleObject(Object identifier, boolean seekGlobal) {
        FrameSlot slot = descriptor.findFrameSlot(identifier);
        if (slot == null && seekGlobal) {
            if (identifier instanceof String) {
                return env.importSymbol((String) identifier);
            }
            if (slot == null) {
                throw Errors.createTypeError(identifier + " not found");
            }
        }
        if (slot != null) {
            try {
                return globalFrame.getObject(slot);
            } catch (FrameSlotTypeException e) {
                throw new IllegalStateException(e);
            }
        } else {
            return null;
        }
    }

}
