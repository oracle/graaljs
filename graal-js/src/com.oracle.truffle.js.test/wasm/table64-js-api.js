/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * WebAssembly table64 JavaScript API tests.
 *
 * @option webassembly
 * @option wasm.Memory64
 * @option wasm.UseUnsafeMemory
 */

load('../js/assert.js')

// i64-indexed tables use BigInt for sizes and indices.
{
    let table = new WebAssembly.Table({initial: 1n, maximum: 3n, element: 'externref', address: 'i64'});
    assertSame(1n, table.length);
    assertSame(undefined, table.get(0n));
    assertSame(1n, table.grow(1n));
    assertSame(2n, table.length);
    const value = {};
    table.set(1n, value);
    assertSame(value, table.get(1n));
    assertThrows(() => table.get(1), TypeError);
}

// Table objects exported from a table64 module retain their address type.
{
    const module = new WebAssembly.Module(new Uint8Array([
        0x00, 0x61, 0x73, 0x6d, 0x01, 0x00, 0x00, 0x00,
        0x04, 0x04, 0x01, 0x70, 0x04, 0x01,
        0x07, 0x05, 0x01, 0x01, 0x74, 0x01, 0x00,
    ]));
    const table = new WebAssembly.Instance(module).exports.t;
    assertSame(1n, table.length);
    assertSame(1n, table.grow(1n));
    assertSame(2n, table.length);
}
