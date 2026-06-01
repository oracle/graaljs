/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

load("assert.js");

(function deleteAfterSpliceInsertResetsZeroBasedOffsets() {
    testDeleteAfterSpliceInsertResetsZeroBasedOffsets([128, 129, 130], 131);
    testDeleteAfterSpliceInsertResetsZeroBasedOffsets([128.5, 129.5, 130.5], 131.5);
})();

function testDeleteAfterSpliceInsertResetsZeroBasedOffsets(array, value) {
    array.shift();
    array.shift();
    array.length = 0;
    array.splice(0, 0, value);

    assertSameContent([value], array);

    assertSame(true, delete array[0]);
    assertSame(1, array.length);
}

// simplified version of the original fuzzer test-case
(function copyWithinWithArgumentConversionMutatingReceiver() {
    const array = [-11, 127, -15364, -61117];
    array.unshift(array);

    const descriptor = Object.getOwnPropertyDescriptor(Float64Array, Symbol.toPrimitive);
    function toPrimitiveGetter() {
        const value = array.shift();
        if (Array.isArray(value)) {
            value.copyWithin(Float64Array, Float64Array);
        }
        array.splice("string", 1024, 1024);
    }

    try {
        Object.defineProperty(Float64Array, Symbol.toPrimitive, { configurable: true, get: toPrimitiveGetter });
        toPrimitiveGetter();
    } finally {
        if (descriptor === undefined) {
            delete Float64Array[Symbol.toPrimitive];
        } else {
            Object.defineProperty(Float64Array, Symbol.toPrimitive, descriptor);
        }
    }

    assertSameContent([1024], array);
})();
