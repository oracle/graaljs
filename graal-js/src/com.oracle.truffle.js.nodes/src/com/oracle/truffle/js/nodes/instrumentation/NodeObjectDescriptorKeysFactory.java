/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.instrumentation;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.interop.MessageResolution;
import com.oracle.truffle.api.interop.Resolve;
import com.oracle.truffle.api.nodes.Node;

@MessageResolution(receiverType = NodeObjectDescriptorKeys.class)
public class NodeObjectDescriptorKeysFactory {

    @Resolve(message = "READ")
    abstract static class Read extends Node {
        @TruffleBoundary
        public Object access(NodeObjectDescriptorKeys target, Number index) {
            return target.getKeyAt(index.intValue());
        }
    }

    @Resolve(message = "HAS_SIZE")
    abstract static class HasSize extends Node {

        public boolean access(@SuppressWarnings("unused") NodeObjectDescriptorKeys target) {
            return true;
        }
    }

    @Resolve(message = "GET_SIZE")
    abstract static class GetSize extends Node {

        public Object access(NodeObjectDescriptorKeys target) {
            return target.size();
        }
    }
}
