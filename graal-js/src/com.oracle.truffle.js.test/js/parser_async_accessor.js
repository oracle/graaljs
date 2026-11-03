/*
 * Copyright (c) 2026, 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Accessors cannot have an async modifier, including computed, static, and private accessors.
 * Async methods named get or set and ordinary synchronous accessors must remain valid.
 */

load('assert.js');

for (const [kind, parameters] of [['get', ''], ['set', 'value']]) {
    for (const name of ['x', '"x"', '["x"]']) {
        const accessor = 'async ' + kind + ' ' + name + '(' + parameters + ') {}';
        for (const source of [
            '({' + accessor + '})',
            'class C {' + accessor + '}',
            'class C {static ' + accessor + '}'
        ]) {
            assertThrows(() => Function(source), SyntaxError);
        }
    }
    const privateAccessor = 'async ' + kind + ' #x(' + parameters + ') {}';
    assertThrows(() => Function('class C {' + privateAccessor + '}'), SyntaxError);
    assertThrows(() => Function('class C {static ' + privateAccessor + '}'), SyntaxError);
}

const object = {
    async get() { return 42; },
    async set(value) { return value; }
};
assertTrue(object.get() instanceof Promise);
assertTrue(object.set(42) instanceof Promise);
Function('class C { async get() {} async set(value) {} }');
Function('class C { static async get() {} static async set(value) {} }');
Function('class C { async #get() {} async #set(value) {} }');

const accessors = {
    get x() { return this.value; },
    set x(value) { this.value = value; }
};
accessors.x = 42;
assertSame(42, accessors.x);
const Class = Function('return class { async\nget x() { return 42; } }')();
assertSame(42, new Class().x);
