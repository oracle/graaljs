/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Multithreaded Wasm tests using atomic instructions
 *
 * @option webassembly
 * @option test262-mode
 * @option wasm.Threads
 * @option wasm.UseUnsafeMemory
 */

load('../js/assert.js');
load('../../../../graal-nodejs/deps/v8/test/mjsunit/wasm/wasm-module-builder.js');

(function TestAtomicWaitNotify() {
    let memory = new WebAssembly.Memory({initial: 1, maximum: 1, shared: true});
    let builder = new WasmModuleBuilder();
    builder.addImportedMemory("env", "imported_mem", 1, 1, "shared");
    builder.addFunction("notify", kSig_i_ii)
        .addBody([
            kExprLocalGet, 0,
            kExprLocalGet, 1,
            kAtomicPrefix,
            kExprAtomicNotify, 2, 0])
        .exportFunc();
    builder.addFunction("wait32", makeSig([kWasmI32, kWasmI32, kWasmI64], [kWasmI32]))
        .addBody([
            kExprLocalGet, 0,
            kExprLocalGet, 1,
            kExprLocalGet, 2,
            kAtomicPrefix,
            kExprI32AtomicWait, 2, 0])
        .exportFunc();
    builder.addFunction("wait64", makeSig([kWasmI32, kWasmI64, kWasmI64], [kWasmI32]))
        .addBody([
            kExprLocalGet, 0,
            kExprLocalGet, 1,
            kExprLocalGet, 2,
            kAtomicPrefix,
            kExprI64AtomicWait, 3, 0])
        .exportFunc();
    let moduleBytes = builder.toBuffer();
    let module = new WebAssembly.Module(moduleBytes);
    let instance = new WebAssembly.Instance(module, {env: {imported_mem: memory}});

    assertEqual(0, instance.exports.notify(0, 4));
    assertEqual(2, instance.exports.wait32(0, 0, BigInt(1e9)));
    assertEqual(2, instance.exports.wait64(0, BigInt(0), BigInt(1e9)));
})();

(function TestMemoryGrow() {
    let memory = new WebAssembly.Memory({initial: 1, maximum: 2, shared: true});
    let builder = new WasmModuleBuilder();
    builder.addImportedMemory("env", "imported_mem", 1, 2, "shared");
    builder.addFunction("grow", kSig_i_i)
        .addBody([
        kExprLocalGet, 0,
        kExprMemoryGrow, kMemoryZero])
        .exportFunc();
    let moduleBytes = builder.toBuffer();
    let module = new WebAssembly.Module(moduleBytes);
    let instance = new WebAssembly.Instance(module, {env: {imported_mem: memory}});

    assertEqual(1, instance.exports.grow(1));
    assertThrows(() => memory.grow(1), RangeError);
})();

(function TestByteLength() {
    // make sure shared memories are backed by a direct ByteBuffer
    let memory = new WebAssembly.Memory({initial: 1, maximum: 1, shared: true});
    assertEqual(kPageSize, memory.buffer.byteLength);
})();