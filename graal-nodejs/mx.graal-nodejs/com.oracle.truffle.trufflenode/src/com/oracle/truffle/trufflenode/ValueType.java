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
package com.oracle.truffle.trufflenode;

public interface ValueType {
    int UNDEFINED_VALUE = 1;
    int NULL_VALUE = 2;
    int BOOLEAN_VALUE_TRUE = 3;
    int BOOLEAN_VALUE_FALSE = 4;
    int STRING_VALUE = 5;
    int NUMBER_VALUE = 6;
    int EXTERNAL_OBJECT = 7;
    int FUNCTION_OBJECT = 8;
    int ARRAY_OBJECT = 9;
    int DATE_OBJECT = 10;
    int REGEXP_OBJECT = 11;
    int ORDINARY_OBJECT = 12;
    int LAZY_STRING_VALUE = 13;
    int ARRAY_BUFFER_VIEW_OBJECT = 14;
    int DIRECT_ARRAY_BUFFER_OBJECT = 15;
    int INTEROP_ARRAY_BUFFER_OBJECT = 45;
    int SYMBOL_VALUE = 16;
    int DIRECT_UINT8ARRAY_OBJECT = 17;
    int DIRECT_UINT8CLAMPEDARRAY_OBJECT = 18;
    int DIRECT_INT8ARRAY_OBJECT = 20;
    int DIRECT_UINT16ARRAY_OBJECT = 21;
    int DIRECT_INT16ARRAY_OBJECT = 22;
    int DIRECT_UINT32ARRAY_OBJECT = 19;
    int DIRECT_INT32ARRAY_OBJECT = 23;
    int DIRECT_FLOAT32ARRAY_OBJECT = 24;
    int DIRECT_FLOAT64ARRAY_OBJECT = 25;
    int MAP_OBJECT = 26;
    int SET_OBJECT = 27;
    int PROMISE_OBJECT = 28;
    int PROXY_OBJECT = 29;
    int DATA_VIEW_OBJECT = 30;
    int BIG_INT_VALUE = 31;
    int DIRECT_BIGINT64ARRAY_OBJECT = 32;
    int DIRECT_BIGUINT64ARRAY_OBJECT = 33;
    int INTEROP_UINT8ARRAY_OBJECT = 34;
    int INTEROP_UINT8CLAMPEDARRAY_OBJECT = 35;
    int INTEROP_INT8ARRAY_OBJECT = 36;
    int INTEROP_UINT16ARRAY_OBJECT = 37;
    int INTEROP_INT16ARRAY_OBJECT = 38;
    int INTEROP_UINT32ARRAY_OBJECT = 39;
    int INTEROP_INT32ARRAY_OBJECT = 40;
    int INTEROP_FLOAT32ARRAY_OBJECT = 41;
    int INTEROP_FLOAT64ARRAY_OBJECT = 42;
    int INTEROP_BIGINT64ARRAY_OBJECT = 43;
    int INTEROP_BIGUINT64ARRAY_OBJECT = 44;
    int MODULE_REQUEST = 45;

    int UNKNOWN_TYPE = -1;
}
