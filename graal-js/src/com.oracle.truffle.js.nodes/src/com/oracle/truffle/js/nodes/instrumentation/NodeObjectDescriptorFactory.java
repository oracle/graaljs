/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.instrumentation;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.interop.CanResolve;
import com.oracle.truffle.api.interop.KeyInfo;
import com.oracle.truffle.api.interop.MessageResolution;
import com.oracle.truffle.api.interop.Resolve;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.nodes.Node;

@MessageResolution(receiverType = NodeObjectDescriptor.class)
public class NodeObjectDescriptorFactory {

    @Resolve(message = "READ")
    abstract static class Read extends Node {
        @TruffleBoundary
        public Object access(NodeObjectDescriptor target, String key) {
            return target.getProperty(key);
        }
    }

    @Resolve(message = "GET_SIZE")
    abstract static class GetSize extends Node {

        public Object access(NodeObjectDescriptor target) {
            return target.size();
        }
    }

    @Resolve(message = "HAS_SIZE")
    abstract static class HasSize extends Node {

        public Object access(@SuppressWarnings("unused") NodeObjectDescriptor target) {
            return false;
        }
    }

    @Resolve(message = "HAS_KEYS")
    abstract static class HasKeys extends Node {

        public Object access(@SuppressWarnings("unused") Object target) {
            return true;
        }
    }

    @Resolve(message = "KEYS")
    abstract static class Keys extends Node {
        @TruffleBoundary
        public Object access(NodeObjectDescriptor target) {
            return target.getPropertyNames();
        }
    }

    @Resolve(message = "KEY_INFO")
    abstract static class KeyInfoMR extends Node {

        public Object access(NodeObjectDescriptor target, Object key) {
            if (key instanceof String && target.hasProperty((String) key)) {
                return KeyInfo.newBuilder().setInternal(false).setInvocable(false).setWritable(false).setReadable(true).build();
            } else {
                return 0;
            }
        }
    }

    @CanResolve
    public abstract static class CanResolveNode extends Node {

        protected boolean test(TruffleObject receiver) {
            return receiver instanceof NodeObjectDescriptor;
        }
    }

}
