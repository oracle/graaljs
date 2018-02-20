/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.runtime.truffleinterop;

import com.oracle.truffle.api.interop.CanResolve;
import com.oracle.truffle.api.interop.MessageResolution;
import com.oracle.truffle.api.interop.Resolve;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.js.runtime.objects.JSLazyString;

@MessageResolution(receiverType = JSLazyString.class)
public class JSLazyStringForeignAccessFactory {

    @Resolve(message = "READ")
    abstract static class ReadNode extends Node {
        public Object access(JSLazyString target, int key) {
            return target.charAt(key);
        }
    }

    @Resolve(message = "GET_SIZE")
    abstract static class GetSizeNode extends Node {
        public int access(JSLazyString target) {
            return target.length();
        }
    }

    @Resolve(message = "UNBOX")
    abstract static class UnboxNode extends Node {
        public Object access(JSLazyString target) {
            return target.toString();
        }
    }

    @Resolve(message = "IS_BOXED")
    abstract static class IsBoxedNode extends Node {
        @SuppressWarnings("unused")
        public Object access(JSLazyString target) {
            return true;
        }
    }

    @Resolve(message = "HAS_SIZE")
    abstract static class HasSizeNode extends Node {
        @SuppressWarnings("unused")
        public Object access(JSLazyString target) {
            return true;
        }
    }

    @CanResolve
    public abstract static class CanResolveNode extends Node {

        protected boolean test(TruffleObject receiver) {
            return receiver instanceof JSLazyString;
        }
    }
}
