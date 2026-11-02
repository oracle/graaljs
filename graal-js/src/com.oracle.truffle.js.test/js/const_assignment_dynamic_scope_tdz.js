/*
 * Copyright (c) 2026, 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Dynamic-scope fallback writes must check the const binding's TDZ after evaluating the RHS.
 */

load('assert.js');

for (const wrap of [
    expression => `eval('var unrelated = 0'); return ${expression};`,
    expression => `with (scope) { return ${expression}; }`,
    expression => `eval('var unrelated = 0'); with (scope) { return ${expression}; }`
]) {
    for (const expression of ['x = rhs()', '[x] = [rhs()]', '({x} = {x: rhs()})']) {
        for (const blockScope of [false, true]) {
            const body = `function f() { ${wrap(expression)} } return f();`;
            const scoped = source => blockScope ? `{ ${source} }` : source;
            const beforeDeclaration = Function('rhs', 'scope', scoped(`${body} const x = 0;`));
            const afterDeclaration = Function('rhs', 'scope', scoped(`const x = 0; ${body}`));
            for (const [run, error] of [[beforeDeclaration, ReferenceError], [afterDeclaration, TypeError]]) {
                let calls = 0;
                assertThrows(() => run(() => { calls++; return 42; }, {}), error);
                assertSame(1, calls);
                assertThrows(() => run(() => { throw new RangeError('rhs'); }, {}), RangeError, 'rhs');
            }
        }
    }

    for (const expression of ['x += rhs()', 'x &&= rhs()', 'x ||= rhs()', 'x ??= rhs()', 'x++', '++x', 'x--', '--x']) {
        const beforeDeclaration = Function('rhs', 'scope', `function f() { ${wrap(expression)} } return f(); const x = 0;`);
        let calls = 0;
        assertThrows(() => beforeDeclaration(() => { calls++; return 42; }, {}), ReferenceError);
        assertSame(0, calls);
    }

    for (const expression of ['x += rhs()', 'x++', '++x', 'x--', '--x']) {
        const afterDeclaration = Function('rhs', 'scope', `const x = 1; function f() { ${wrap(expression)} } return f();`);
        assertThrows(() => afterDeclaration(() => 42, {}), TypeError);
    }
}

// A shadowing dynamic binding must bypass the fallback binding's TDZ check entirely.
for (const wrap of [
    expression => `eval('var x = 0'); return ${expression};`,
    expression => `with (scope) { return ${expression}; }`,
    expression => `eval('var unrelated = 0'); with (scope) { return ${expression}; }`
]) {
    for (const expression of ['x = rhs()', 'x += rhs()', '[x] = [rhs()]', '({x} = {x: rhs()})']) {
        const run = Function('rhs', 'scope', `function f() { ${wrap(expression)} } f(); const x = 1; return x;`);
        let calls = 0;
        assertSame(1, run(() => { calls++; return 42; }, {x: 0}));
        assertSame(1, calls);
    }
}
