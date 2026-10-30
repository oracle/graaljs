/*
 * Copyright (c) 2026, 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Any property access within an optional chain is an invalid assignment, update, destructuring, or
 * loop target. Parentheses may end the chain before a separate, assignable property access.
 */

load('assert.js');

for (const target of ['obj?.a.b', 'obj?.a[b]', 'obj?.().a', 'obj?.[a].b', 'obj.a?.b.c', '(obj?.a.b)']) {
    for (const source of [
        target + ' = 1;',
        target + ' += 1;',
        target + ' &&= 1;',
        target + ' ??= 1;',
        '++' + target + ';',
        '--' + target + ';',
        target + '++;',
        target + '--;',
        '({x: ' + target + '} = {});',
        '[' + target + '] = [];',
        '[...' + target + '] = [];',
        'for (' + target + ' in {}) {}',
        'for (' + target + ' of []) {}',
        'async function f() { for await (' + target + ' of []) {} }'
    ]) {
        assertThrows(() => Function(source), SyntaxError);
    }
}

for (const target of ['(obj?.a).b', '(obj?.a)[b]', '(obj?.()).a']) {
    Function(target + ' = 1;');
    Function(target + '++;');
    Function('({x: ' + target + '} = {});');
    Function('for (' + target + ' of []) {}');
}

const object = {a: {b: 0}};
(object?.a).b = 1;
(object?.a).b++;
assertSame(2, object.a.b);
for ((object?.a).b of [42]) {}
assertSame(42, object.a.b);
