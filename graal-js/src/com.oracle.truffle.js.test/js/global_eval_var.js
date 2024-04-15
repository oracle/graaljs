/*
 * Copyright (c) 2023, 2024, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Tests corner cases w.r.t. global var declarations created by eval code conflicting with lexical declarations.
 */

load('assert.js');

globalThis.x = 42;

eval("var x;");
assertTrue(Object.getOwnPropertyDescriptor(globalThis, "x").configurable);

// global let x does not conflict with eval-declared global var x
load('global_eval_var_let.js'); // "let x"

// but eval-declared global var x conflicts with global let x
try {
    eval("var x;");
    fail("should have thrown SyntaxError");
} catch (e) {
    assertSame(SyntaxError, e.constructor);
}
