/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * @option webassembly
 * @option ecmascript-version=staging
 */

load('../js/assert.js');

// (module
//   (import "env" "g" (global $env_g externref))
//   (export "g" (global $env_g))
// )
var source = Uint8Array.fromBase64("AGFzbQEAAAACCgEDZW52AWcDbwAHBQEBZwMAABIEbmFtZQIBAAcIAQAFZW52X2c=");
var module = new WebAssembly.Module(source);
var o = { hello: "world" };
var instance = new WebAssembly.Instance(module, {env: { g: o }});
assertSame(o, instance.exports.g.value);
