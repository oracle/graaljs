/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * @option ecmascript-version=2024
 */

load("assert.js");

(function sliceRelativeIndicesUseCurrentLength() {
    var buffer = new SharedArrayBuffer(4, { maxByteLength: 8 });
    new Uint8Array(buffer).set([1, 2, 3, 4]);

    var result = buffer.slice(-2);

    assertSame(2, result.byteLength);
    assertSameContent([3, 4], new Uint8Array(result));
})();

(function sliceRejectsSpeciesResultWithInsufficientCurrentLength() {
    var buffer = new SharedArrayBuffer(4, { maxByteLength: 8 });
    buffer.constructor = {};
    buffer.constructor[Symbol.species] = function(length) {
        assertSame(2, length);
        return new SharedArrayBuffer(0, { maxByteLength: length });
    };

    assertThrows(() => buffer.slice(0, 2), TypeError);
})();
