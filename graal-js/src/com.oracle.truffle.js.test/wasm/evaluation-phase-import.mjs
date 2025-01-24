/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Test "host instance linking" aka evaluation phase imports from WebAssembly modules.
 *
 * @option webassembly
 * @option unhandled-rejections=throw
 */

load("../js/assert.js");

import { mul } from "./source-phase-import.wasm";
export { mul }; // imported from wasm

assertSame(481, mul(13, 37));

import * as moduleNamespace from "./evaluation-phase-import.wasm";
import defaultExport, {fib, answer as fortyTwo, memory as memory0} from "./evaluation-phase-import.wasm";

function assertModuleNamespaceObject(obj) {
    assertSame("Module", obj[Symbol.toStringTag]);
}

assertModuleNamespaceObject(moduleNamespace);
assertSameContent(["answer", "default", "fib", "memory"], Object.keys(moduleNamespace));

const n = 14;
assertSame(377n, fib(n));
assertSame(377n, moduleNamespace.fib(n));
assertSame(1764, defaultExport());
assertSame(1764, moduleNamespace.default());

assertTrue(fortyTwo instanceof WebAssembly.Global);
assertSame(42, fortyTwo.value);
assertTrue(moduleNamespace.answer instanceof WebAssembly.Global);
assertSame(42, moduleNamespace.answer.value);

assertTrue(memory0 instanceof WebAssembly.Memory);
assertSame(memory0, moduleNamespace.memory);
assertSame(65536, memory0.buffer.byteLength);
assertSame("JS <3 Wasm", readString(memory0.buffer, 0));

import("./source-phase-import.wasm").then(
    exports => {
        assertModuleNamespaceObject(exports);
        assertSameContent(["mul"], Object.keys(exports));
        assertSame(481, exports.mul(13, 37));
        assertSame(mul, exports.mul);
    }
);

function readString(buffer, offset) {
    let bytes = new Uint8Array(buffer, offset);
    return String.fromCharCode(...bytes.subarray(0, bytes.indexOf(0)));
}
