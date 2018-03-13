/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.test.instrumentation;

import com.oracle.truffle.api.interop.CanResolve;
import com.oracle.truffle.api.interop.MessageResolution;
import com.oracle.truffle.api.interop.Resolve;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.nodes.Node;

@MessageResolution(receiverType = ForeignTestObject.class)
public class ForeignTestObjectMessageResolution {

    @CanResolve
    public abstract static class CanHandleTestMap extends Node {
        public boolean test(TruffleObject o) {
            return o instanceof ForeignTestObject;
        }
    }

    @Resolve(message = "INVOKE")
    abstract static class RunnableInvokeNode extends Node {

        @SuppressWarnings("unused")
        public Object access(ForeignTestObject invoker, String identifier, Object[] arguments) {
            return 42;
        }
    }

}
