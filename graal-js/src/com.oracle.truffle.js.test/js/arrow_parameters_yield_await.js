/*
 * Copyright (c) 2021, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * It is an early syntax error if ArrowParameters contains YieldExpression or AwaitExpression.
 */

load('assert.js');

// yield & await are just identifiers
/* allowed */(function () { (yield) => {} })();
/* allowed */(function () { (p = yield) => {} })();
/* allowed */(function () { (await) => {} })();
/* allowed */(function () { (p = await) => {} })();

syntaxError("(function*() { (yield) => {} })();");
syntaxError("(function*() { (p = yield) => {} })();");
syntaxError("(function*() { (p = 21 + yield) => {} })();");
syntaxError("(function*() { (p = 21 + yield* sth) => {} })();");
// await is just an identifier
/* allowed */(function*() { (await) => {} })();
/* allowed */(function*() { (p = await) => {} })();

// yield is just an identifier
/* allowed */(async function () { (yield) => {} })();
/* allowed */(async function () { (p = yield) => {} })();
/* allowed */(async function () { (p = 21 + yield) => {} })();
/* allowed */(async function () { (p = 21 + yield * sth) => {} })();
syntaxError("(async function () { (await) => {} })();");
syntaxError("(async function () { (p = await 21) => {} })();");
syntaxError("(async function () { (p = 21 + await 21) => {} })();");
syntaxError("(async function () { (p = (await 21, await 21)) => {} })();");

syntaxError("(async function*() { (yield) => {} })();");
syntaxError("(async function*() { (p = yield) => {} })();");
syntaxError("(async function*() { (p = 21 + yield) => {} })();");
syntaxError("(async function*() { (p = 21 + yield* sth) => {} })();");
syntaxError("(async function*() { (await) => {} })();");
syntaxError("(async function*() { (p = await 21) => {} })();");
syntaxError("(async function*() { (p = 21 + await 21) => {} })();");
syntaxError("(async function*() { (p = (await 21, await 21)) => {} })();");

syntaxError("async(p = await 21) => {}");
syntaxError("async(p = 21 + await 21) => {}");
syntaxError("async(p = (await 21, await 21)) => {}");
// await is not a valid identifier in async arrow function head
syntaxError("async(await) => {}");
syntaxError("async(\\u0061wait) => {}");
syntaxError("async(p = (await) => {}) => {}");
syntaxError("async(p = (\\u0061wait) => {}) => {}");
syntaxError("async(p = async (await) => {}) => {}");
// await is a valid identifier in non-async arrow function body
/* allowed */async(p = () => await) => {}
/* allowed */async(p = () => \u0061wait) => {}
// and of course, await can be used as an identifier when it is just a call expression
/* allowed */true || async(await);
/* allowed */true || async(\u0061wait);


// yield and await are of course allowed again if they are used in the body of nested function.

/* allowed */(      function () { (p = async() => (await 21, await 21)) => {} })();
/* allowed */(      function*() { (p = async() => (await 21, await 21)) => {} })();
/* allowed */(async function () { (p = async() => (await 21, await 21)) => {} })();
/* allowed */(async function*() { (p = async() => (await 21, await 21)) => {} })();

/* allowed */(      function () { (p = async function() { (await 21, await 21) }) => {} })();
/* allowed */(      function*() { (p = async function() { (await 21, await 21) }) => {} })();
/* allowed */(async function () { (p = async function() { (await 21, await 21) }) => {} })();
/* allowed */(async function*() { (p = async function() { (await 21, await 21) }) => {} })();

/* allowed */(      function () { (p = async function*() { (await 21, yield 21) }) => {} })();
/* allowed */(      function*() { (p = async function*() { (await 21, yield 21) }) => {} })();
/* allowed */(async function () { (p = async function*() { (await 21, yield 21) }) => {} })();
/* allowed */(async function*() { (p = async function*() { (await 21, yield 21) }) => {} })();

/* allowed */(      function () { (p = function*() { (yield 21, yield 21) }) => {} })();
/* allowed */(      function*() { (p = function*() { (yield 21, yield 21) }) => {} })();
/* allowed */(async function () { (p = function*() { (yield 21, yield 21) }) => {} })();
/* allowed */(async function*() { (p = function*() { (yield 21, yield 21) }) => {} })();

function syntaxError(code) {
    assertThrows(() => new Function(code), SyntaxError);
}
