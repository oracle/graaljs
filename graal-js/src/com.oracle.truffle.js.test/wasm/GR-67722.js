/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Regression test for structured cloning of WebAssembly.Module objects into worker realms.
 *
 * The cloned module must not share per-language Wasm interop call targets with the source realm.
 * With VM assertions enabled, sharing the source realm's internal WasmModule can fail with
 * "Invalid sharing of AST nodes detected" in <wasm>.wasm-function-interop.
 *
 * @option webassembly
 * @option worker
 * @option engine.DisableCodeSharing=true
 */

const ATTEMPTS = 16;
const WORKERS = 8;
const FUNCTIONS = 256;
const PARAMS = 8;

// Minimal unsigned LEB128 encoding helpers for building a Wasm binary directly in JS.
function u32(value) {
    const out = [];
    do {
        let byte = value & 0x7f;
        value >>>= 7;
        if (value !== 0) {
            byte |= 0x80;
        }
        out.push(byte);
    } while (value !== 0);
    return out;
}

function str(s) {
    const out = u32(s.length);
    for (let i = 0; i < s.length; i++) {
        out.push(s.charCodeAt(i));
    }
    return out;
}

function section(id, bytes) {
    return [id, ...u32(bytes.length), ...bytes];
}

function makeModule() {
    const bytes = [0x00, 0x61, 0x73, 0x6d, 0x01, 0x00, 0x00, 0x00];

    // Wasm caches the JS interop adapter per function type, so many distinct
    // function types give the test independent chances to hit cross-layer reuse.
    const types = u32(FUNCTIONS);
    for (let i = 0; i < FUNCTIONS; i++) {
        types.push(0x60, ...u32(PARAMS));
        for (let p = 0; p < PARAMS; p++) {
            types.push(((i >>> p) & 1) ? 0x7c : 0x7f);
        }
        types.push(0x01, 0x7f);
    }
    bytes.push(...section(1, types));

    const funcs = u32(FUNCTIONS);
    for (let i = 0; i < FUNCTIONS; i++) {
        funcs.push(...u32(i));
    }
    bytes.push(...section(3, funcs));

    const exports = u32(FUNCTIONS);
    for (let i = 0; i < FUNCTIONS; i++) {
        exports.push(...str("f" + i), 0x00, ...u32(i));
    }
    bytes.push(...section(7, exports));

    const code = u32(FUNCTIONS);
    for (let i = 0; i < FUNCTIONS; i++) {
        const body = [0x00, 0x41, 0x00, 0x0b];
        code.push(...u32(body.length), ...body);
    }
    bytes.push(...section(10, code));

    return new Uint8Array(bytes);
}

const module = new WebAssembly.Module(makeModule());
const workerCode = `
const FUNCTIONS = ${FUNCTIONS};
const PARAMS = ${PARAMS};
const args = Array(PARAMS).fill(1);

onmessage = function(e) {
    const ia = new Int32Array(e.data.sab);
    const instance = new WebAssembly.Instance(e.data.module);
    const exports = instance.exports;
    postMessage("ready");

    while (Atomics.load(ia, 0) === 0) {
        Atomics.wait(ia, 0, 0);
    }

    for (let i = 0; i < FUNCTIONS; i++) {
        const r = exports["f" + i](...args);
        if (r !== 0) {
            throw new Error("bad result " + i + ": " + r);
        }
    }
    postMessage("done");
};
`;

function runAttempt() {
    const sab = new SharedArrayBuffer(4);
    const workers = [];

    for (let i = 0; i < WORKERS; i++) {
        const worker = new Worker(workerCode, {type: "string"});
        workers.push(worker);
        worker.postMessage({module, sab});
    }
    for (const worker of workers) {
        const msg = worker.getMessage();
        if (msg !== "ready") {
            throw new Error("expected ready, got " + msg);
        }
    }
    const ia = new Int32Array(sab);
    Atomics.store(ia, 0, 1);
    Atomics.notify(ia, 0, WORKERS);

    for (const worker of workers) {
        const msg = worker.getMessage();
        if (msg !== "done") {
            throw new Error("worker terminated before done; see assertion above; got " + msg);
        }
        worker.terminate();
    }
}

for (let attempt = 0; attempt < ATTEMPTS; attempt++) {
    runAttempt();
}
