/*
 * Copyright (c) 2022, 2022, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Verify correct handling of with statement with nested direct eval.
 */

load("assert.js");

var res;
var expected = 42;
var chk = function chk() { assertSame(expected, res); res = undefined; };

var b = [42];
with({}) { for(let w of eval("var a = b; a")) res = w; } chk();
with({}) { for(let w of eval("b;")) res = w; } chk();
b = 42;
with({}) { let w = eval("var a = b"); res = a; } chk();
with([1]) { let w = eval("var a = b"); res = a; } chk();
var t={}; with(t) { let w = eval("var b = a"); res = b; } chk();
with("abc") { let w = eval("var b = a"); res = b; } chk();
with("") { let w = eval("b"); res = w; } chk();

expected = 43;

var wee = {b: [43]};
with(wee) { with({}) { for(let w of eval("var a = b; a")) { res = w; } } } chk();
with(wee) { with({}) { for(let w of eval("b;")) { res = w; } } } chk();
wee.b = 43;
with(wee) { with({}) { let w = eval("var a = b"); res = a; } } chk();
expected = 42;
with([1]) { with({}) { let w = eval("var a = b"); res = a; } } chk();
var t={}; with(t) { with({}) { let w = eval("var b = a"); res = b; } } chk();
with("abc") { with({}) { let w = eval("var b = a"); res = b; } } chk();
with("") { with({}) { let w = eval("b"); res = w; } } chk();

expected = 43;

var wee = {b: [43]};
with(wee) { with({}) { for(let w of eval("'use strict'; var a = b; a")) { res = w; } } } chk();
with(wee) { with({}) { for(let w of eval("'use strict'; b;")) { res = w; } } } chk();
wee.b = 43;
with(wee) { with({}) { let w = eval("'use strict'; b"); res = w; } } chk();
