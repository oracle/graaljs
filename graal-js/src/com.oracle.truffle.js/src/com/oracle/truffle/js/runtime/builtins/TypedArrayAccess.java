/*
 * Copyright (c) 2018, 2022, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.runtime.builtins;

import java.nio.ByteBuffer;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.array.TypedArray;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;

public class TypedArrayAccess {
    public static final TypedArrayAccess SINGLETON = new TypedArrayAccess();

    protected TypedArrayAccess() {
    }

    public int getLength(JSDynamicObject thisObj) {
        return ((JSArrayBufferViewBase) thisObj).length;
    }

    public void setLength(JSDynamicObject thisObj, int length) {
        ((JSArrayBufferViewBase) thisObj).length = length;
    }

    public int getOffset(JSDynamicObject thisObj) {
        return ((JSArrayBufferViewBase) thisObj).offset;
    }

    public void setOffset(JSDynamicObject thisObj, int offset) {
        ((JSArrayBufferViewBase) thisObj).offset = offset;
    }

    public byte[] getByteArray(JSDynamicObject thisObj) {
        byte[] byteArray = ((JSArrayBufferObject.Heap) getArrayBuffer(thisObj)).getByteArray();
        if (byteArray == null) {
            CompilerDirectives.transferToInterpreter();
            throw Errors.createTypeErrorDetachedBuffer();
        }
        return byteArray;
    }

    public ByteBuffer getByteBuffer(JSDynamicObject thisObj) {
        ByteBuffer byteBuffer = ((JSArrayBufferObject.DirectBase) getArrayBuffer(thisObj)).getByteBuffer();
        if (byteBuffer == null) {
            CompilerDirectives.transferToInterpreter();
            throw Errors.createTypeErrorDetachedBuffer();
        }
        return byteBuffer;
    }

    public JSArrayBufferObject getArrayBuffer(JSDynamicObject thisObj) {
        assert JSArrayBufferView.isJSArrayBufferView(thisObj);
        return ((JSArrayBufferViewBase) thisObj).getArrayBuffer();
    }

    public TypedArray getArrayType(Object thisObj) {
        return ((JSTypedArrayObject) thisObj).arrayType;
    }

    public void setArrayType(JSDynamicObject thisObj, TypedArray arrayType) {
        ((JSTypedArrayObject) thisObj).arrayType = arrayType;
    }

    public TruffleString getTypedArrayName(JSDynamicObject thisObj) {
        return getArrayType(thisObj).getFactory().getName();
    }
}
