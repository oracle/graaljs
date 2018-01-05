/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
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
