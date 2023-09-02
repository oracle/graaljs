/*
 * Copyright (c) 2023, 2023, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Tests the instantiation of WebAssembly module when Object.prototype is customized
 * as reported at https://github.com/oracle/graaljs/issues/749
 *
 * @option webassembly
 */

load('../js/assert.js');

Object.defineProperty(Object.prototype, 'extern_module', { value: null, writable: false });
//(module
//  (import "extern_module" "fn" (func $fn1 (param i32) (result)))
//  (func $main (param) (result)
//    (call $fn1 (i32.const 1))
//  )
//  (start $main)
//)
var bytes = new Uint8Array([0,97,115,109,1,0,0,0,1,8,2,96,1,127,0,96,0,0,2,20,1,13,101,120,116,101,114,110,95,109,111,100,117,108,101,2,102,110,0,0,3,2,1,1,8,1,1,10,8,1,6,0,65,1,16,0,11,0,26,4,110,97,109,101,1,12,2,0,3,102,110,49,1,4,109,97,105,110,2,5,2,0,0,1,0]);
var wasmModule = new WebAssembly.Module(bytes);
var fnCalled = false;
var jsApi = {
    fn(arg) {
        assertSame(1, arg);
        fnCalled = true;
    }
};
var imports = { extern_module: jsApi };
new WebAssembly.Instance(wasmModule, imports);
assertTrue(fnCalled);
