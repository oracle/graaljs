/*
 * Copyright (c) 2026, 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * For-in declaration initializers are rejected when Annex B is disabled, unlike ordinary loop heads.
 *
 * @option annex-b=false
 */

load('assert.js');

for (const prefix of ['', '"use strict"; ']) {
    for (const statement of [
        'for (var x = 42 in {}) {}',
        '{ for (var x = (0, 42) in {}) {} }',
        'function f() { for (var x = function() {} in {}) {} }'
    ]) {
        assertThrows(() => Function(prefix + statement), SyntaxError);
        assertThrows(() => eval(prefix + statement), SyntaxError);
    }

    for (const [loop, expected] of [
        ['for (var x in {a: 42}) { result.push(x); }', ['a']],
        ['for (let x in {a: 42}) { result.push(x); }', ['a']],
        ['for (const x in {a: 42}) { result.push(x); }', ['a']],
        ['for (var x of [42]) { result.push(x); }', [42]],
        ['for (var x = 0; x < 1; x++) { result.push(x); }', [0]]
    ]) {
        const source = prefix + 'const result = []; ' + loop;
        assertSameContent(expected, Function(source + ' return result;')());
        assertSameContent(expected, eval(source + ' result;'));
    }
}
