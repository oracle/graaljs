/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Local assignments evaluate the RHS before checking the target binding's temporal dead zone.
 */

load('assert.js');

function checkRhs(run, error) {
    let calls = 0;
    assertThrows(() => run(() => { calls++; return 42; }), error);
    assertSame(1, calls);
    assertThrows(() => run(() => { throw new RangeError('rhs'); }), RangeError, 'rhs');
}

// Exercise current-frame, block, and captured bindings.
for (const wrap of [
    (statement, declaration) => `${statement} ${declaration}`,
    (statement, declaration) => `'use strict'; ${statement} ${declaration}`,
    (statement, declaration) => `{ ${statement} ${declaration} }`,
    (statement, declaration) => `function f() { ${statement} } return f(); ${declaration}`,
    (statement, declaration) => `{ function f() { ${statement} } return f(); ${declaration} }`
]) {
    for (const declaration of ['let x;', 'const x = 0;']) {
        for (const expression of ['x = rhs()', '[x] = [rhs()]', '({x} = {x: rhs()})']) {
            const run = Function('rhs', wrap(`return ${expression};`, declaration));
            checkRhs(run, ReferenceError);
        }
        for (const expression of ['x += rhs()', 'x &&= rhs()', 'x ||= rhs()', 'x ??= rhs()', 'x++', '++x']) {
            const run = Function('rhs', wrap(`return ${expression};`, declaration));
            let calls = 0;
            assertThrows(() => run(() => { calls++; return 42; }), ReferenceError);
            assertSame(0, calls);
        }
    }
}

// The RHS can initialize the captured binding by resuming its declaring generator.
function* declaringGenerator() {
    yield rhs => x = rhs();
    let x;
    yield () => x;
}
const generator = declaringGenerator();
const assign = generator.next().value;
let read;
assertSame(42, assign(() => { read = generator.next().value; return 42; }));
assertSame(42, read());

// Specialize a captured assignment, then use it with an uninitialized binding.
for (const declare of [
    function* () { yield rhs => x = rhs(); let x = 42; yield x; },
    function* () { yield rhs => x = rhs(); let x = 1.5; yield x; },
    function* () { yield rhs => x = rhs(); let x = true; yield x; }
]) {
    const initialized = declare();
    const write = initialized.next().value;
    const initialValue = initialized.next().value;
    assertSame(initialValue, write(() => initialValue));

    const uninitialized = declare().next().value;
    // Both matching and unexpected RHS types must reach the TDZ check.
    for (const value of [initialValue, 'different type']) {
        let calls = 0;
        assertThrows(() => uninitialized(() => { calls++; return value; }), ReferenceError);
        assertSame(1, calls);
    }
    assertThrows(() => uninitialized(() => { throw new RangeError('rhs'); }), RangeError, 'rhs');

    let calls = 0;
    assertSame('different type', write(() => { calls++; return 'different type'; }));
    assertSame(1, calls);
}
