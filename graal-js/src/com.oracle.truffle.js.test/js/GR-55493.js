/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Ensure that the parsing of some special parameter lists does not result in
 * an internal error.
 */

load("assert.js");

var fn;

fn = new Function("{ arguments }", "return arguments");
assertSame('foo', fn({ arguments: 'foo' }));

fn = new Function("x = eval(6*7)", "return x");
assertSame(42, fn());

assertThrows(() => eval('new Function("x = this.#y", "")'), SyntaxError);

assertThrows(() => eval('new Function("x = super.y", "")'), SyntaxError);
