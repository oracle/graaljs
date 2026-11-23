/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Module assignments evaluate the RHS before checking TDZ and preserve import immutability.
 */

load('../assert.js');

import { value as imported } from './assignment_tdz_module.mjs';

function checkRhs(run, error) {
    let calls = 0;
    assertThrows(() => run(() => { calls++; return 42; }), error);
    assertSame(1, calls);
    assertThrows(() => run(() => { throw new RangeError('rhs'); }), RangeError, 'rhs');
}

const log = [];
function rhs() { log.push('rhs evaluated'); return 1; }
try { moduleLet = rhs(); } catch (e) { log.push(e.name); }
assertSameContent(['rhs evaluated', 'ReferenceError'], log);

checkRhs(rhs => moduleLet = rhs(), ReferenceError);
checkRhs(rhs => moduleConst = rhs(), ReferenceError);
checkRhs(rhs => moduleClass = rhs(), ReferenceError);
let moduleLet;
const moduleConst = 0;
class moduleClass {}
assertSame(42, moduleLet = 42);
assertSame(42, moduleClass = 42);
checkRhs(rhs => moduleConst = rhs(), TypeError);

// The local import binding is initialized even while its exported target is in the TDZ.
assertThrows(() => imported, ReferenceError);
checkRhs(rhs => imported = rhs(), TypeError);
let calls = 0;
assertThrows(() => imported += (++calls, 42), ReferenceError);
assertSame(0, calls);

export let value = 1;
checkRhs(rhs => imported = rhs(), TypeError);
assertSame(1, imported);
