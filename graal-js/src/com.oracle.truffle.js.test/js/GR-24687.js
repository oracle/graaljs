/*
 * Copyright (c) 2020, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

load('assert.js');

// valueOf() should be invoked twice (from ToUint32() and ToNumber())
var valueOfCalls = 0;
Object.defineProperty([0, 1, 2], 'length', {
    value: {
        valueOf: function () {
            valueOfCalls++;
            return 3;
        }
    }
});
assertSame(2, valueOfCalls);

// length should remain writable when its re-definition is refused
var array = [42, 211];
assertThrows(function () {
    Object.defineProperty(array, 'length', {
        writable: false,
        configurable: true
    });
}, TypeError);
array.length = 5;
assertSame(5, array.length);

// length should be constant after re-definition with "writable: false"
var array = [0, 1, 2];
Object.defineProperty(array, 'length', {
    value: 3,
    writable: false
});
assertThrows(function () {
    array.splice(1, 2, 4);
}, TypeError);
assertSame(3, array.length);
assertSame(array[0], 0);
assertSame(array[1], 4);
assertSame(array[2], undefined);

true;
