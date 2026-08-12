/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

load('./assert.js');

function assertSyntaxError(code) {
    assertThrows(() => Function(code), SyntaxError);
    assertThrows(() => globalThis.eval(code), SyntaxError);
}

function assertValid(code) {
    Function(code)();
    globalThis.eval(code);
}

assertSyntaxError('for (x => 0 in 1;;) break;');
assertSyntaxError('for ((x) => 0 in 1;;) break;');
assertSyntaxError('for (async x => 0 in 1;;) break;');
assertSyntaxError('for (async (x) => 0 in 1;;) break;');

assertValid('for ((x => 0 in 1);;) break;');
assertValid('for (((x) => 0 in 1);;) break;');
assertValid('for ((async x => 0 in 1);;) break;');
assertValid('for ((async (x) => 0 in 1);;) break;');

assertValid('for (x => (0 in 1);;) break;');
assertValid('for ((x) => (0 in 1);;) break;');
assertValid('for (async x => (0 in 1);;) break;');
assertValid('for (async (x) => (0 in 1);;) break;');

assertValid('for (x => true ? 0 in 1 : false;;) break;');
assertValid('for ((x) => true ? 0 in 1 : false;;) break;');
assertValid('for (async x => true ? 0 in 1 : false;;) break;');
assertValid('for (async (x) => true ? 0 in 1 : false;;) break;');

assertValid('for (x => { return 0 in 1; };;) break;');
assertValid('for ((x) => { return 0 in 1; };;) break;');
assertValid('for (async x => { return 0 in 1; };;) break;');
assertValid('for (async (x) => { return 0 in 1; };;) break;');

assertValid('x => 0 in 1;');
assertValid('(x) => 0 in 1;');
assertValid('async x => 0 in 1;');
assertValid('async (x) => 0 in 1;');
