/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Dynamically resolved global assignments evaluate the RHS before checking TDZ or immutability.
 */

load('assert.js');

let rhs;
function checkRhs(run, error) {
    let calls = 0;
    rhs = () => { calls++; return 42; };
    assertThrows(run, error);
    assertSame(1, calls);
    rhs = () => { throw new RangeError('rhs'); };
    assertThrows(run, RangeError, 'rhs');
}

const compilers = [
    expression => Function(`return ${expression};`),
    expression => Function(`'use strict'; return ${expression};`),
    expression => () => (0, eval)(expression),
    expression => () => (0, eval)(`'use strict'; ${expression};`)
];
const letWrites = [];
const constWrites = [];
for (const compile of compilers) {
    for (const [name, writes] of [['globalLet', letWrites], ['globalConst', constWrites]]) {
        for (const expression of [`${name} = rhs()`, `[${name}] = [rhs()]`, `({x: ${name}} = {x: rhs()})`]) {
            const run = compile(expression);
            writes.push(run);
            checkRhs(run, ReferenceError);
        }
        // Operations that read the target first must still throw before evaluating the RHS.
        for (const expression of [`${name} += rhs()`, `${name} &&= rhs()`, `${name} ||= rhs()`, `${name} ??= rhs()`, `${name}++`, `++${name}`]) {
            let calls = 0;
            rhs = () => { calls++; return 42; };
            assertThrows(compile(expression), ReferenceError);
            assertSame(0, calls);
        }
    }
}

let globalLet;
const globalConst = 1;
// Reuse the same writes after initialization to exercise cached binding checks.
for (const run of letWrites) {
    rhs = () => 42;
    run();
    assertSame(42, globalLet);
}
for (const run of constWrites) {
    checkRhs(run, TypeError);
}

for (const compile of compilers) {
    checkRhs(compile('globalConst += rhs()'), TypeError);
    checkRhs(compile('globalConst &&= rhs()'), TypeError);
    rhs = () => { throw new RangeError('unexpected RHS'); };
    assertSame(1, compile('globalConst ||= rhs()')());
    assertSame(1, compile('globalConst ??= rhs()')());

    // Compound assignments perform value conversion before reporting const reassignment.
    rhs = () => ({valueOf() { throw new RangeError('conversion'); }});
    assertThrows(compile('globalConst += rhs()'), RangeError, 'conversion');
}

const globalObjectConst = {valueOf() { throw new RangeError('conversion'); }};
for (const compile of compilers) {
    assertThrows(compile('globalObjectConst++'), RangeError, 'conversion');
    assertThrows(compile('++globalObjectConst'), RangeError, 'conversion');
}

// The global-object fallback continues to accept writes.
globalThis.dynamicGlobalProperty = 0;
for (const compile of compilers) {
    rhs = () => 42;
    assertSame(42, compile('dynamicGlobalProperty = rhs()')());
    assertSame(42, globalThis.dynamicGlobalProperty);
}
delete globalThis.dynamicGlobalProperty;
