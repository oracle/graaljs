/*
 * Copyright (c) 2022, 2022, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Tests of AwaitExpression and YieldExpression (in FormalParameters mostly).
 */

load("assert.js");

assertThrows(() => eval("async function f(x = await 42) {}"), SyntaxError);
assertThrows(() => eval("async function f(x = (await 42)) {}"), SyntaxError);
assertThrows(() => eval("function* f(x = yield) {}"), SyntaxError);
assertThrows(() => eval("function* f(x = (yield)) {}"), SyntaxError);

// valid
(async function() { return (await 42); })
(function* () { return (yield); })

// original code from fuzzer
assertThrows(() => eval("{ if (!isAsmJSCompilationAvailable()) { void 0; void 0; } void 0; } h1.delete = (function() { try { delete o1['ceil']; } catch(e0) { } a2.forEach(async function*  b (c, y = (window = yield)) { /*RXUB*/var r = new RegExp('(?!(?!(?!^)|.|[]*|[^]\\3)|${3,8388612}){3,}', 'gim'); var s = ''; print(s.replace(r, (p={}, (p.z = Object.values('5'))())));  } ); throw e0; });"), SyntaxError);
assertThrows(() => eval("function shapeyConstructor(nmrrcz){return this; }/*tLoopC*/for (let x of async function* (x, w = (await Math)) { /* no regression tests found */ } ) { try{let nslmgy = shapeyConstructor(x); print('EETT'); g1.a1.__proto__ = this.v2;}catch(e){print('TTEE ' + e); } }"), SyntaxError);
