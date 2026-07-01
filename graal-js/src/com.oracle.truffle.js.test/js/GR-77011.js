/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Test that competing SharedArrayBuffer.prototype.grow calls do not lose a successful grow.
 *
 * @option ecmascript-version=2024
 * @option worker
 */

load("assert.js");

function createGrowWorker(newByteLength) {
    return new Worker(`
        onmessage = function(event) {
            const sab = event.data;
            const synchronization = new Int32Array(sab);
            Atomics.add(synchronization, 0, 1);
            Atomics.wait(synchronization, 1, 0);
            try {
                sab.grow(${newByteLength});
            } catch (e) {
                // A concurrent larger grow may make the smaller request invalid.
            }
            postMessage("done");
        };
    `, {type: "string"});
}

const growTo24 = createGrowWorker(24);
const growTo20 = createGrowWorker(20);
try {
    for (let i = 0; i < 1000; i++) {
        const sab = new SharedArrayBuffer(16, {maxByteLength: 24});
        const synchronization = new Int32Array(sab);
        growTo24.postMessage(sab);
        growTo20.postMessage(sab);
        while (Atomics.load(synchronization, 0) !== 2) {
        }
        Atomics.store(synchronization, 1, 1);
        Atomics.notify(synchronization, 1, 2);
        growTo24.getMessage();
        growTo20.getMessage();
        assertSame(24, sab.byteLength);
    }
} finally {
    growTo24.terminate();
    growTo20.terminate();
}
