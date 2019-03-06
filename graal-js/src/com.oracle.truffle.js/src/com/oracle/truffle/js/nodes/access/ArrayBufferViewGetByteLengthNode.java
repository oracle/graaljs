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
