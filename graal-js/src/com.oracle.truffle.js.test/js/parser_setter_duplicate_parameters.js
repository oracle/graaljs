/*
 * Copyright (c) 2026, 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Setter parameters must reject duplicate bindings within destructuring patterns, including nested
 * patterns and rest bindings, for object, class, static, and private setters.
 */

load('assert.js');

for (const parameter of ['[a, a]', '{a, a}', '[a, {x: a}]', '[a, ...a]', '{a, b: a}', '{a = 1, b: a = 2}']) {
    for (const source of [
        '({set value(' + parameter + ') {}})',
        'class C { set value(' + parameter + ') {} }',
        'class C { static set value(' + parameter + ') {} }',
        'class C { set #value(' + parameter + ') {} }',
        'class C { static set #value(' + parameter + ') {} }'
    ]) {
        assertThrows(() => Function(source), SyntaxError);
    }
}

const object = {
    set value([a, b] = [1, 2]) {
        this.sum = a + b;
    }
};
object.value = [20, 22];
assertSame(42, object.sum);
object.value = undefined;
assertSame(3, object.sum);

assertThrows(() => Function('({set value(...args) {}})'), SyntaxError);
