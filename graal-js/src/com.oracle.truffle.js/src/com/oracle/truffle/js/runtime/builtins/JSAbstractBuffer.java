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
package com.oracle.truffle.js.runtime.builtins;

import java.nio.ByteBuffer;

import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.api.object.Property;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.JSShape;

public abstract class JSAbstractBuffer extends JSBuiltinObject {

    protected static final Shape.Allocator allocator;
    protected static final String BYTE_LENGTH = "byteLength";
    protected static final HiddenKey BYTE_ARRAY_ID = new HiddenKey("byteArray");
    protected static final Property BYTE_ARRAY_PROPERTY;
    protected static final Property BYTE_BUFFER_PROPERTY;

    static {
        allocator = JSShape.makeAllocator(JSObject.LAYOUT);
        BYTE_ARRAY_PROPERTY = JSObjectUtil.makeHiddenProperty(BYTE_ARRAY_ID, allocator.copy().locationForType(byte[].class));
        BYTE_BUFFER_PROPERTY = JSObjectUtil.makeHiddenProperty(BYTE_ARRAY_ID, allocator.locationForType(ByteBuffer.class));
    }

    protected JSAbstractBuffer() {
    }

    public static byte[] getByteArray(DynamicObject thisObj) {
        return getByteArray(thisObj, JSAbstractBuffer.isJSAbstractHeapBuffer(thisObj));
    }

    public static byte[] getByteArray(DynamicObject thisObj, boolean condition) {
        assert isJSAbstractHeapBuffer(thisObj);
        return (byte[]) BYTE_ARRAY_PROPERTY.get(thisObj, condition);
    }

    public static int getByteLength(DynamicObject thisObj) {
        assert isJSAbstractHeapBuffer(thisObj);
        return getByteArray(thisObj).length;
    }

    public static boolean isJSAbstractHeapBuffer(Object obj) {
        return JSObject.isDynamicObject(obj) && isJSAbstractHeapBuffer((DynamicObject) obj);
    }

    public static boolean isJSAbstractHeapBuffer(DynamicObject obj) {
        return JSArrayBuffer.isJSHeapArrayBuffer(obj);
    }

    public static boolean isJSAbstractBuffer(Object obj) {
        return JSObject.isDynamicObject(obj) && isJSAbstractBuffer((DynamicObject) obj);
    }

    public static boolean isJSAbstractBuffer(DynamicObject obj) {
        return JSArrayBuffer.isJSHeapArrayBuffer(obj) || JSArrayBuffer.isJSDirectArrayBuffer(obj) || JSSharedArrayBuffer.isJSSharedArrayBuffer(obj);
    }
}
