/*
 * Copyright (c) 2026, 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

load("assert.js");

function codeUnit(value) {
    return String.fromCharCode(value).charCodeAt(0);
}

function uint32(value) {
    return value >>> 0;
}

function int32(value) {
    return value | 0;
}

function int32UsingDataView(value) {
    const dataView = new DataView(new ArrayBuffer(4));
    dataView.setInt32(0, value);
    return dataView.getInt32(0);
}

assertSame(0, codeUnit(1e20));
assertSame(65535, codeUnit(65535.9));
assertSame(1, codeUnit(-65535.9));
assertSame(1, codeUnit(65537.9));
assertSame(65535, codeUnit(-65537.9));
assertSame(4096, codeUnit(2 ** 63 + 2 ** 12));
assertSame(61440, codeUnit(-(2 ** 63 + 2 ** 12)));

const string = String.fromCharCode(65, 1e20, 2 ** 63 + 2 ** 12);
assertSame(65, string.charCodeAt(0));
assertSame(0, string.charCodeAt(1));
assertSame(4096, string.charCodeAt(2));

assertSame(255, uint32(2 ** 40 + 255.9));
assertSame(4294967041, uint32(-(2 ** 40 + 255.9)));
assertSame(4096, uint32(2 ** 63 + 2 ** 12));
assertSame(4294963200, uint32(-(2 ** 63 + 2 ** 12)));
assertSame(0, uint32(java.lang.Long.MAX_VALUE));
assertSame(0, uint32(java.lang.Long.MIN_VALUE));

assertSame(255, int32(2 ** 40 + 255.9));
assertSame(-255, int32(-(2 ** 40 + 255.9)));
assertSame(4096, int32(2 ** 63 + 2 ** 12));
assertSame(-4096, int32(-(2 ** 63 + 2 ** 12)));
assertSame(0, int32(java.lang.Long.MAX_VALUE));
assertSame(0, int32(java.lang.Long.MIN_VALUE));

assertSame(-4097, ~(2 ** 63 + 2 ** 12));
assertSame(0, int32UsingDataView(java.lang.Long.MAX_VALUE));
assertSame(0, int32UsingDataView(java.lang.Long.MIN_VALUE));
