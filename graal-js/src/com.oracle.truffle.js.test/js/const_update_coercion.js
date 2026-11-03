/*
 * Copyright (c) 2026, 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Updates of immutable bindings must perform ToNumeric before rejecting the write.
 */

load('assert.js');

for (const expression of ['x++', '++x', 'x--', '--x']) {
    for (const run of [
        Function('value', 'const x = value; return ' + expression + ';'),
        Function('value', '"use strict"; const x = value; return ' + expression + ';'),
        Function('value', 'const x = value; return (() => ' + expression + ')();'),
        Function('value', 'const x = value; return function() { return ' + expression + '; };')
    ]) {
        function update(value) {
            const result = run(value);
            return typeof result === 'function' ? result() : result;
        }

        for (const primitive of [42, 42n, Symbol('value')]) {
            const log = [];
            const value = {
                [Symbol.toPrimitive](hint) {
                    log.push(hint);
                    return primitive;
                }
            };
            assertThrows(() => update(value), TypeError);
            assertSameContent(['number'], log);
        }

        const log = [];
        const value = {
            valueOf() { log.push('valueOf'); return {}; },
            toString() { log.push('toString'); return '42'; }
        };
        assertThrows(() => update(value), TypeError);
        assertSameContent(['valueOf', 'toString'], log);

        for (const hook of ['valueOf', Symbol.toPrimitive]) {
            assertThrows(() => update({[hook]() { throw new RangeError('coercion'); }}), RangeError, 'coercion');
            const getter = Object.defineProperty({}, hook, {
                get() { throw new RangeError('coercion getter'); }
            });
            assertThrows(() => update(getter), RangeError, 'coercion getter');
        }
    }

    let coerced = false;
    const value = {valueOf() { coerced = true; return 42; }};
    const beforeDeclaration = Function('value', expression + '; const x = value;');
    assertThrows(() => beforeDeclaration(value), ReferenceError);
    assertFalse(coerced);
}
