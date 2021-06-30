/*
 * Copyright (c) 2021, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Error Cause proposal should work with WebAssembly error constructors, too.
 *
 * @option webassembly
 * @option error-cause
 */

load('../js/assert.js');

const WasmErrors = [
    WebAssembly.CompileError,
    WebAssembly.LinkError,
    WebAssembly.RuntimeError,
];

for (let WasmError of WasmErrors) {
    let e;
    e = new WasmError();
    assertSame("", e.message);
    assertSame(undefined, e.cause);
    e = new WasmError("withoutcause");
    assertSame("withoutcause", e.message);
    assertSame(undefined, e.cause);
    let cause = e;
    e = new WasmError("withcause", {cause: cause});
    assertSame("withcause", e.message);
    assertSame(cause, e.cause);
}
