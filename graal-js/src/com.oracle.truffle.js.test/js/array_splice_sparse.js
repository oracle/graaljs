/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Regression test for Array.prototype.splice on sparse arrays.
 */

load("assert.js");

const assert = {
    sameValue(actual, expected) {
        assertSame(expected, actual);
    },
    compareArray(actual, expected) {
        assertSameContent(expected, actual);
    }
}

for (const array of ([
    Array(10_000_000), // <= 2147483646
    {__proto__: Array.prototype, length: 1_000_000},
])) {
    const largeLength = array.length;

    // Make sparse array.
    array.splice(largeLength - 1, 1, 'lastElement');
    array.splice(largeLength / 2, 1, 'middleElement');
    array.splice(0, 0, 'firstElement');
    assert.sameValue(array.length, largeLength + 1);

    array.splice(largeLength - 1, 2);
    assert.sameValue(array.length, largeLength - 1);

    array.splice(largeLength - 1, 1, 'newElement');
    assert.sameValue(array.length, largeLength);
    assert.sameValue(array[largeLength - 1], 'newElement');

    array.splice(largeLength - 40, 5);
    assert.sameValue(array.length, largeLength - 5);
    assert.sameValue(array[largeLength - 6], 'newElement');

    array.splice(largeLength - 5, 1, 'newElement1', 'newElement2', 'newElement3');
    assert.sameValue(array.length, largeLength - 2);
    assert.compareArray(array.slice(largeLength - 5, largeLength - 2), ['newElement1', 'newElement2', 'newElement3']);

    array.splice(largeLength - 7, 0, 'newElement0');
    assert.sameValue(array.length, largeLength - 1);
    assert.compareArray(array.slice(largeLength - 7, largeLength - 1), ['newElement0', , 'newElement', 'newElement1', 'newElement2', 'newElement3']);
}
