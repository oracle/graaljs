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
package com.oracle.truffle.trufflenode.serialization;

import com.oracle.truffle.js.runtime.array.TypedArrayFactory;

/**
 * A tag that determines the type of the serialized {@code ArrayBufferView}.
 */
public enum ArrayBufferViewTag {
    INT8_ARRAY('b', TypedArrayFactory.Int8Array), // kInt8Array
    UINT8_ARRAY('B', TypedArrayFactory.Uint8Array), // kUint8Array
    UINT8_CLAMPED_ARRAY('C', TypedArrayFactory.Uint8ClampedArray), // kUint8ClampedArray
    INT16_ARRAY('w', TypedArrayFactory.Int16Array), // kInt16Array
    UINT16_ARRAY('W', TypedArrayFactory.Uint16Array), // kUint16Array
    INT32_ARRAY('d', TypedArrayFactory.Int32Array), // kInt32Array
    UINT32_ARRAY('D', TypedArrayFactory.Uint32Array), // kUint32Array
    FLOAT32_ARRAY('f', TypedArrayFactory.Float32Array), // kFloat32Array
    FLOAT64_ARRAY('F', TypedArrayFactory.Float64Array), // kFloat64Array
    DATA_VIEW('?', null); // kDataView

    private final byte tag;
    private final TypedArrayFactory factory;

    ArrayBufferViewTag(char tag, TypedArrayFactory factory) {
        this.tag = (byte) tag;
        this.factory = factory;
    }

    public byte getTag() {
        return tag;
    }

    public TypedArrayFactory getFactory() {
        return factory;
    }

    public static ArrayBufferViewTag fromTag(byte tag) {
        for (ArrayBufferViewTag t : values()) {
            if (t.tag == tag) {
                return t;
            }
        }
        return null;
    }

    public static ArrayBufferViewTag fromFactory(TypedArrayFactory factory) {
        for (ArrayBufferViewTag t : values()) {
            if (t.factory == factory) {
                return t;
            }
        }
        return null;
    }

}
