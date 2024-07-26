/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Test error messages for missing importObject keys or illegal values.
 *
 * @option webassembly
 */

load('../js/assert.js');

//(module
//   (type (func (result i32)))
//   (import "m" "f" (func (type 0)))
//)
const bytes = new Uint8Array([
    0x0,0x61,0x73,0x6d,0x1,0x0,0x0,0x0,
    0x1,0x5,0x1,0x60,0x0,0x1,0x7f,
    0x2,0x7,0x1,0x1,0x6d,0x1,0x66,0x0,0x0]);

const wasmModule = new WebAssembly.Module(bytes);
assertThrows(() => new WebAssembly.Instance(wasmModule, {}), TypeError, 'Imported module "m" is not an object');

assertThrows(() => new WebAssembly.Instance(wasmModule, {m: {}}), WebAssembly.LinkError, 'Import #0 "m" "f": Imported value is not callable');
assertThrows(() => new WebAssembly.Instance(wasmModule, {m: {f: 404}}), WebAssembly.LinkError, 'Import #0 "m" "f": Imported value is not callable');
