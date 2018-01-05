/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.trufflenode.buffer;

import com.oracle.truffle.js.builtins.JSBuiltinsContainer;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;

public final class NIOBufferBuiltins extends JSBuiltinsContainer.SwitchEnum<NIOBufferBuiltins.Buffer> {
    protected NIOBufferBuiltins() {
        super("NIOBuffer.prototype", Buffer.class);
    }

    public enum Buffer implements BuiltinEnum<Buffer> {
        utf8Write(0),
        utf8Slice(0);

        private final int length;

        private Buffer(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, Buffer builtinEnum) {
        switch (builtinEnum) {
            case utf8Write:
                return NIOBufferUTF8WriteNodeGen.create(context, builtin, args().withThis().fixedArgs(3).createArgumentNodes(context));
            case utf8Slice:
                return NIOBufferUTF8SliceNodeGen.create(context, builtin, args().withThis().fixedArgs(2).createArgumentNodes(context));
        }
        return null;
    }

}
