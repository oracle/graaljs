/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
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
    int ARRAY_BUFFER_OBJECT = 15;
    int SYMBOL_VALUE = 16;
    int UINT8ARRAY_OBJECT = 17;
    int UINT8CLAMPEDARRAY_OBJECT = 18;
    int INT8ARRAY_OBJECT = 20;
    int UINT16ARRAY_OBJECT = 21;
    int INT16ARRAY_OBJECT = 22;
    int UINT32ARRAY_OBJECT = 19;
    int INT32ARRAY_OBJECT = 23;
    int FLOAT32ARRAY_OBJECT = 24;
    int FLOAT64ARRAY_OBJECT = 25;
    int MAP_OBJECT = 26;
    int SET_OBJECT = 27;
    int PROMISE_OBJECT = 28;
    int PROXY_OBJECT = 29;
    int DATA_VIEW_OBJECT = 30;

    int UNKNOWN_TYPE = -1;
}
