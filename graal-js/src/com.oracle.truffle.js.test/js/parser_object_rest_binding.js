/*
 * Copyright (c) 2026, 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Object rest bindings require an identifier, unlike array rest bindings, which may contain nested
 * patterns. Object rest assignment targets may still be property accesses.
 */

load('assert.js');

for (const pattern of ['{...{a}}', '{...[a]}', '{x: {...{a}}}', '[{...[a]}]']) {
    for (const source of [
        'var ' + pattern + ' = {};',
        'let ' + pattern + ' = {};',
        'const ' + pattern + ' = {};',
        'function f(' + pattern + ') {}',
        '(' + pattern + ') => {};',
        'for (const ' + pattern + ' of []) {}',
        'try {} catch (' + pattern + ') {}'
    ]) {
        assertThrows(() => Function(source), SyntaxError);
    }
}

const {a, ...rest} = {a: 1, b: 42};
assertSame(1, a);
assertSame(42, rest.b);
assertSame(42, (({...value}) => value.b)({b: 42}));

const [...[first, second]] = [20, 22];
assertSame(42, first + second);
const [...{length}] = [1, 2];
assertSame(2, length);

const holder = {};
({...holder.value} = {a: 42});
assertSame(42, holder.value.a);
assertThrows(() => Function('({...{a}} = {});'), SyntaxError);
assertThrows(() => Function('({...[a]} = {});'), SyntaxError);
let assigned;
[...[assigned]] = [42];
assertSame(42, assigned);
