/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Test of same data block checks in SAB.slice and TA.set.
 * 
 * @option worker
 */

load("assert.js");

(function sharedArrayBufferSliceRejectsSpeciesResultWithSameDataBlock() {
    var buffer = new SharedArrayBuffer(4);
    new Uint8Array(buffer).set([1, 2, 3, 4]);

    var worker = new Worker("onmessage = function(event) { postMessage(event.data); };", { type: "string" });
    try {
        worker.postMessage(buffer);
        var alias = worker.getMessage();

        assertFalse(alias === buffer);
        new Uint8Array(alias)[0] = 42;
        assertSame(42, new Uint8Array(buffer)[0]);

        buffer.constructor = {};
        buffer.constructor[Symbol.species] = function(length) {
            assertSame(2, length);
            return alias;
        };

        assertThrows(() => buffer.slice(0, 2), TypeError);
    } finally {
        worker.terminate();
    }
})();

(function typedArraySetClonesSourceWithSameSharedDataBlock() {
    var buffer = new SharedArrayBuffer(5);
    var bytes = new Uint8Array(buffer);
    bytes.set([1, 2, 3, 4]);

    var worker = new Worker("onmessage = function(event) { postMessage(event.data); };", { type: "string" });
    try {
        worker.postMessage(buffer);
        var alias = worker.getMessage();

        assertFalse(alias === buffer);
        new Uint8Array(alias)[4] = 42;
        assertSame(42, bytes[4]);

        // The source and target ranges overlap in the same shared data block.
        // The source must be snapshotted before writes to avoid propagating the first write.
        new Uint8Array(buffer, 1, 3).set(new Int8Array(alias, 0, 3));

        assertSameContent([1, 1, 2, 3, 42], bytes);
    } finally {
        worker.terminate();
    }
})();
