/*
 * Copyright (c) 2026, 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Global lexical assignment evaluates its RHS before checking the target binding's TDZ.
 */

load('assert.js');

function checkRhs(run, error) {
    let calls = 0;
    assertThrows(() => run(() => { calls++; return 42; }), error);
    assertSame(1, calls);
    assertThrows(() => run(() => { throw new RangeError('rhs'); }), RangeError, 'rhs');
}

// Statically resolved global lexical bindings.
const log = [];
function rhs() { log.push('rhs evaluated'); return 1; }
try { globalLet = rhs(); } catch (e) { log.push(e.name); }
assertSameContent(['rhs evaluated', 'ReferenceError'], log);

checkRhs(rhs => globalLet = rhs(), ReferenceError);
checkRhs(rhs => globalConst = rhs(), ReferenceError);
let globalLet;
const globalConst = 0;
assertSame(42, globalLet = 42);
assertSame(42, globalLet);
checkRhs(rhs => globalConst = rhs(), TypeError);
