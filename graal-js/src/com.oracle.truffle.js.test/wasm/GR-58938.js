/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Tests the update of a buffer when a WebAssembly.Memory is grown.
 *
 * @option webassembly
 * @option wasm.UseUnsafeMemory
 */

load('../js/assert.js');

var memory = new WebAssembly.Memory({ initial: 1, maximum: 2 });
assertSame(65536, memory.buffer.byteLength);
assertSame(1, memory.grow(1));
assertSame(131072, memory.buffer.byteLength);

var sharedMemory = new WebAssembly.Memory({ initial: 1, maximum: 2, shared: true });
assertSame(65536, sharedMemory.buffer.byteLength);
assertSame(1, sharedMemory.grow(1));
assertSame(131072, sharedMemory.buffer.byteLength);
