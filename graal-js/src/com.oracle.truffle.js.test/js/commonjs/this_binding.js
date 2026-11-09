/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Tests that strict and non-strict CommonJS modules receive the original exports object as this,
 * including after replacing module.exports.
 *
 * @option commonjs-require=true
 */

load('../assert.js');

for (const name of ['./fixtures/this_binding.js', './fixtures/this_binding_strict.js']) {
    const result = require(name);
    assertSame(result.originalExports, result.originalExports.originalThis);
    assertSame(42, result.originalExports.value);
    assertSame(result.originalExports, result.receiverAfterReassignment);
    assertSame(result, require(name));
}
