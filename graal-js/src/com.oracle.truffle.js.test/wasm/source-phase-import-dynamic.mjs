/*
 * Copyright (c) 2024, 2025, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Test source phase imports.
 *
 * @option webassembly=true
 * @option source-phase-imports=true
 * @option unhandled-rejections=throw
 */

const wasmModule = await import.source("./source-phase-import.wasm");

load('../js/assert.js');

assertTrue(wasmModule instanceof WebAssembly.Module);
assertSame("WebAssembly.Module", wasmModule[Symbol.toStringTag]);

const instance = new WebAssembly.Instance(wasmModule);
assertSame(481, instance.exports.mul(13, 37));
