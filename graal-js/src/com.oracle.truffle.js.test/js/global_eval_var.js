/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Tests corner cases w.r.t. global var declarations created by eval code conflicting with lexical declarations.
 */

load('assert.js');

// ensure var declaration flag is also set on an already existing property.
globalThis.x = 42;

eval("var x;");
assertTrue(Object.getOwnPropertyDescriptor(globalThis, "x").configurable);

// let x conflicts with var x
assertThrows(() => load('global_eval_var_let.js')); // "let x"

// ensure redefining a global property does not lose the var declaration flag.
Object.defineProperty(globalThis, 'x', { value: 42 });

assertThrows(() => load('global_eval_var_let.js')); // "let x"

delete globalThis.x;

load('global_eval_var_let.js'); // "let x"
