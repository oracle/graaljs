/*
 * Copyright (c) 2026, 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Arithmetic and bitwise compound assignments must not infer names for anonymous function or class
 * operands. Simple assignments, logical assignments, and declaration initializers still infer names.
 */

load('assert.js');

const definitions = ['function() {}', '() => {}', 'async function() {}', 'function*() {}', 'async function*() {}', 'class {}'];
const names = [];
const previous = Object.getOwnPropertyDescriptor(Function.prototype, Symbol.toPrimitive);
Object.defineProperty(Function.prototype, Symbol.toPrimitive, {
    configurable: true,
    value() {
        names.push(this.name);
        return 1;
    }
});
try {
    for (const operator of ['+=', '-=', '*=', '/=', '%=', '**=', '<<=', '>>=', '>>>=', '&=', '|=', '^=']) {
        for (const definition of definitions) {
            names.length = 0;
            Function('let target = 2; target ' + operator + ' ' + definition + ';')();
            assertSameContent([''], names);

            names.length = 0;
            Function('const target = {value: 2}; target.value ' + operator + ' ' + definition + ';')();
            assertSameContent([''], names);
        }
    }
} finally {
    if (previous === undefined) {
        delete Function.prototype[Symbol.toPrimitive];
    } else {
        Object.defineProperty(Function.prototype, Symbol.toPrimitive, previous);
    }
}

for (const definition of definitions) {
    for (const assignment of ['let target; target = ', 'let target = ', 'let target = true; target &&= ', 'let target; target ||= ', 'let target; target ??= ']) {
        assertSame('target', Function(assignment + definition + '; return target.name;')());
    }
    assertSame('target', Function('let {target = ' + definition + '} = {}; return target.name;')());
    assertSame('', Function('let target; (target) = ' + definition + '; return target.name;')());
}

let result = '';
result += class {
    static [Symbol.toPrimitive]() {
        return this.name;
    }
};
assertSame('', result);
