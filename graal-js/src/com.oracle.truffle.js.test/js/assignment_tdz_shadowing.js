/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Writes to shadowing dynamic bindings must not check the fallback lexical binding's TDZ.
 */

load('assert.js');

// A with binding shadows a lexical binding in the current frame.
const scope = {x: 0};
{
    with (scope) {
        x = 42;
    }
    let x;
}
assertSame(42, scope.x);

// Cover captured bindings and combinations of dynamic scopes.
for (const wrap of [
    expression => `with (scope) { ${expression}; return x; }`,
    expression => `with ({}) { with (scope) { ${expression}; return x; } }`,
    expression => `eval('var x = scope.x'); ${expression}; return x;`,
    expression => `eval('var unrelated = 0'); with (scope) { ${expression}; return x; }`
]) {
    for (const declaration of ['let x;', 'const x = 0;', 'class x {}']) {
        for (const [expression, initial, expected, callsExpected] of [
            ['x = rhs()', 0, 42, 1],
            ['[x] = [rhs()]', 0, 42, 1],
            ['({x} = {x: rhs()})', 0, 42, 1],
            ['x += rhs()', 1, 43, 1],
            ['x &&= rhs()', 1, 42, 1],
            ['x ||= rhs()', 0, 42, 1],
            ['x ??= rhs()', null, 42, 1],
            ['x++', 1, 2, 0],
            ['++x', 1, 2, 0],
            ['x--', 1, 0, 0],
            ['--x', 1, 0, 0]
        ]) {
            const run = Function('rhs', 'scope', `function f() { ${wrap(expression)} } return f(); ${declaration}`);
            let calls = 0;
            assertSame(expected, run(() => { calls++; return 42; }, {x: initial}));
            assertSame(callsExpected, calls);
            if (callsExpected !== 0) {
                assertThrows(() => run(() => { throw new RangeError('rhs'); }, {x: initial}), RangeError, 'rhs');
            }
        }
    }
}

// The resolved with binding remains the target even if the RHS deletes its property.
const deletingScope = {x: 0};
{
    with (deletingScope) {
        x = (delete deletingScope.x, 42);
    }
    let x;
}
assertSame(42, deletingScope.x);

// A missing or unscopable dynamic binding must still check the fallback binding's TDZ.
for (const wrap of [
    expression => `with (scope) { return ${expression}; }`,
    expression => `eval('var unrelated = 0'); return ${expression};`,
    expression => `eval('var unrelated = 0'); with (scope) { return ${expression}; }`
]) {
    for (const dynamicScope of [{}, {x: 0, [Symbol.unscopables]: {x: true}}]) {
        const assign = Function('rhs', 'scope', `function f() { ${wrap('x = rhs()')} } return f(); let x;`);
        let calls = 0;
        assertThrows(() => assign(() => { calls++; return 42; }, dynamicScope), ReferenceError);
        assertSame(1, calls);
        assertThrows(() => assign(() => { throw new RangeError('rhs'); }, dynamicScope), RangeError, 'rhs');

        const add = Function('rhs', 'scope', `function f() { ${wrap('x += rhs()')} } return f(); let x;`);
        assertThrows(() => add(() => { calls++; return 42; }, dynamicScope), ReferenceError);
        assertSame(1, calls);
    }
}
