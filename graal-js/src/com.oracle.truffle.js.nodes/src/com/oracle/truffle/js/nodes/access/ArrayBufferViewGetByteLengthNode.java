/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.access;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.array.TypedArray;
import com.oracle.truffle.js.runtime.builtins.JSArrayBufferView;

/**
 * Optimization over JSArrayBufferView.getByteLength to have a valueProfile on the TypedArray,
 * potentially avoiding a virtual call.
 */
public abstract class ArrayBufferViewGetByteLengthNode extends JavaScriptBaseNode {

    private final JSContext context;

    protected ArrayBufferViewGetByteLengthNode(JSContext context) {
        this.context = context;
    }

    public abstract int executeInt(DynamicObject obj);

    public static ArrayBufferViewGetByteLengthNode create(JSContext context) {
        return ArrayBufferViewGetByteLengthNodeGen.create(context);
    }

    @Specialization(guards = {"isJSArrayBufferView(obj)", "hasDetachedBuffer(obj)"})
    protected int getByteLengthDetached(@SuppressWarnings("unused") DynamicObject obj) {
        return 0;
    }

    @Specialization(guards = {"isJSArrayBufferView(obj)", "!hasDetachedBuffer(obj)", "cachedArray == getArrayType(obj)"})
    protected int getByteLength(DynamicObject obj,
                    @Cached("getArrayType(obj)") TypedArray cachedArray) {
        boolean condition = JSArrayBufferView.isJSArrayBufferView(obj);
        return cachedArray.lengthInt(obj, condition) * cachedArray.bytesPerElement();
    }

    @Specialization(guards = {"isJSArrayBufferView(obj)", "!hasDetachedBuffer(obj)"}, replaces = "getByteLength")
    protected int getByteLengthOverLimit(DynamicObject obj) {
        boolean condition = JSArrayBufferView.isJSArrayBufferView(obj);
        TypedArray typedArray = getArrayType(obj);
        return typedArray.lengthInt(obj, condition) * typedArray.bytesPerElement();
    }

    @Specialization(guards = "!isJSArrayBufferView(obj)")
    protected int getByteLengthNoObj(@SuppressWarnings("unused") DynamicObject obj) {
        throw Errors.createTypeErrorArrayBufferViewExpected();
    }

    protected static TypedArray getArrayType(DynamicObject obj) {
        return JSArrayBufferView.typedArrayGetArrayType(obj);
    }

    protected boolean hasDetachedBuffer(DynamicObject object) {
        return JSArrayBufferView.hasDetachedBuffer(object, context);
    }

}
