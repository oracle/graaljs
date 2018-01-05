/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.trufflenode.buffer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.nodes.access.ArrayBufferViewGetByteLengthNode;
import com.oracle.truffle.js.nodes.access.ArrayBufferViewGetByteLengthNodeGen;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.builtins.JSArrayBuffer;
import com.oracle.truffle.js.runtime.builtins.JSArrayBufferView;

public abstract class NIOBufferAccessNode extends JSBuiltinNode {

    protected static final Charset utf8 = Charset.forName("UTF-8");

    @Child protected ArrayBufferViewGetByteLengthNode getLenNode;

    public NIOBufferAccessNode(JSContext context, JSBuiltin builtin) {
        super(context, builtin);
        this.getLenNode = ArrayBufferViewGetByteLengthNodeGen.create(context);
    }

    protected static DynamicObject getArrayBuffer(DynamicObject target) {
        assert JSArrayBufferView.isJSArrayBufferView(target) : "Target object must be a JSArrayBufferView";
        DynamicObject arrayBuffer = JSArrayBufferView.getArrayBuffer(target);
        assert JSArrayBuffer.isJSDirectArrayBuffer(arrayBuffer) : "Target buffer must be a DirectArrayBuffer";
        return arrayBuffer;
    }

    protected int getOffset(DynamicObject target) {
        int byteOffset = JSArrayBufferView.getByteOffset(target, JSArrayBufferView.isJSArrayBufferView(target), getContext());
        return byteOffset;
    }

    protected int getLength(DynamicObject target) {
        return getLenNode.executeInt(target);
    }

    protected static ByteBuffer sliceBuffer(ByteBuffer rawBuffer, int byteOffset) {
        ByteBuffer data = ((ByteBuffer) rawBuffer.duplicate().position(byteOffset)).slice().order(ByteOrder.nativeOrder());
        return data;
    }

    @TruffleBoundary
    protected static void outOfBoundsFail() {
        throw Errors.createRangeError("out of range index");
    }

    protected static boolean accept(DynamicObject target) {
        return JSArrayBufferView.isJSArrayBufferView(target);
    }
}
