/*
 * Copyright (c) 2017, 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.runtime;

import com.oracle.truffle.api.interop.CanResolve;
import com.oracle.truffle.api.interop.MessageResolution;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.nodes.Node;

/**
 * Message resolution for Symbol TruffleObject.
 */
@MessageResolution(receiverType = Symbol.class)
public class SymbolMessageResolution {

    @CanResolve
    public abstract static class CanHandleSymbol extends Node {
        public boolean test(TruffleObject o) {
            return o instanceof Symbol;
        }
    }
}
