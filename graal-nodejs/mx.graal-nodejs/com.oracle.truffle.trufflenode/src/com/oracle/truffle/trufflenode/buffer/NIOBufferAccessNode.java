/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The Universal Permissive License (UPL), Version 1.0
 *
 * Subject to the condition set forth below, permission is hereby granted to any
 * person obtaining a copy of this software, associated documentation and/or
 * data (collectively the "Software"), free of charge and under any and all
 * copyright rights in the Software, and any and all patent rights owned or
 * freely licensable by each licensor hereunder covering either (i) the
 * unmodified Software as contributed to or provided by such licensor, or (ii)
 * the Larger Works (as defined below), to deal in both
 *
 * (a) the Software, and
 *
 * (b) any piece of software and/or hardware listed in the lrgrwrks.txt file if
 * one is included with the Software each a "Larger Work" to which the Software
 * is contributed by such licensors),
 *
 * without restriction, including without limitation the rights to copy, create
 * derivative works of, display, perform, and distribute the Software and make,
 * use, sell, offer for sale, import, export, have made, and have sold the
 * Software and the Larger Work(s), and to sublicense the foregoing rights on
 * either these or other terms.
 *
 * This license is subject to the following condition:
 *
 * The above copyright notice and either this complete permission notice or at a
 * minimum a reference to the UPL must be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
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
import com.oracle.truffle.js.runtime.builtins.JSSharedArrayBuffer;

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
        assert JSArrayBuffer.isJSDirectArrayBuffer(arrayBuffer) || JSSharedArrayBuffer.isJSSharedArrayBuffer(arrayBuffer) : "Target buffer must be a DirectArrayBuffer";
        return arrayBuffer;
    }

    protected static ByteBuffer getDirectByteBuffer(DynamicObject arrayBuffer) {
        if (JSArrayBuffer.isJSDirectArrayBuffer(arrayBuffer)) {
            return JSArrayBuffer.getDirectByteBuffer(arrayBuffer);
        } else {
            assert JSSharedArrayBuffer.isJSSharedArrayBuffer(arrayBuffer);
            return JSSharedArrayBuffer.getDirectByteBuffer(arrayBuffer);
        }
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
