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

/**
 * A tag that determines the type of the serialized value.
 */
public enum SerializationTag {
    TRUE('T'), // kTrue
    FALSE('F'), // kFalse
    UNDEFINED('_'), // kUndefined
    NULL('0'), // kNull
    INT32('I'), // kInt32
    UINT32('U'), // kUint32
    DOUBLE('N'), // kDouble
    UTF8_STRING('S'), // kUtf8String
    ONE_BYTE_STRING('"'), // kOneByteString
    TWO_BYTE_STRING('c'), // kTwoByteString
    PADDING('\0'), // kPadding
    DATE('D'), // kDate
    TRUE_OBJECT('y'), // kTrueObject
    FALSE_OBJECT('x'), // kFalseObject
    NUMBER_OBJECT('n'), // kNumberObject
    STRING_OBJECT('s'), // kStringObject
    REGEXP('R'), // kRegExp
    ARRAY_BUFFER('B'), // kArrayBuffer
    SHARED_ARRAY_BUFFER('u'), // kSharedArrayBuffer
    ARRAY_BUFFER_TRANSFER('t'), // kArrayBufferTransfer
    ARRAY_BUFFER_VIEW('V'), // kArrayBufferView
    BEGIN_JS_MAP(';'), // kBeginJSMap
    END_JS_MAP(':'), // kEndJSMap
    BEGIN_JS_SET('\''), // kBeginJSSet
    END_JS_SET(','), // kEndJSSet
    BEGIN_JS_OBJECT('o'), // kBeginJSObject
    END_JS_OBJECT('{'), // kEndJSObject
    BEGIN_SPARSE_JS_ARRAY('a'), // kBeginSparseJSArray
    END_SPARSE_JS_ARRAY('@'), // kEndSparseJSArray
    BEGIN_DENSE_JS_ARRAY('A'), // kBeginDenseJSArray
    END_DENSE_JS_ARRAY('$'), // kEndDenseJSArray
    THE_HOLE('-'), // kTheHole
    OBJECT_REFERENCE('^'), // kObjectReference
    HOST_OBJECT('\\'); // kHostObject

    private final byte tag;

    SerializationTag(char tag) {
        this.tag = (byte) tag;
    }

    public byte getTag() {
        return tag;
    }

    public static SerializationTag fromTag(byte tag) {
        for (SerializationTag t : values()) {
            if (t.tag == tag) {
                return t;
            }
        }
        return null;
    }

}
