/*
 * Copyright (c) 2026, 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Legacy leading-zero decimal literals remain forbidden in strict mode when a fraction or exponent
 * follows, but their values and property names must continue to work in sloppy mode.
 */

load('assert.js');

for (const prefix of ['08', '09', '0008', '0009']) {
    for (const suffix of ['.', '.0', '.1', '.1e1', 'e1', 'e+1', 'e-1']) {
        const literal = prefix + suffix;
        const expected = Number(literal);
        assertSame(expected, Function('return ' + literal + ';')());
        assertSame(-expected, Function('return -' + literal + ';')());
        const object = Function('return ({' + literal + ': 42});')();
        assertSame(42, object[String(expected)]);

        assertThrows(() => Function('"use strict"; return ' + literal + ';'), SyntaxError);
        assertThrows(() => Function('"use strict"; return ({' + literal + ': 42});'), SyntaxError);
        assertThrows(() => Function('return class { method() { return ' + literal + '; } };'), SyntaxError);
        assertThrows(() => Function('return class { [' + literal + ']() {} };'), SyntaxError);
    }
}

assertSame('8', Function('return 08..toString();')());
assertThrows(() => Function('"use strict"; return 08..toString();'), SyntaxError);

for (const literal of ['0.1', '0e1', '0.0e-1', '8.1', '9e1', '0x08', '0o10', '0b1000']) {
    assertSame(Number(literal), Function('"use strict"; return ' + literal + ';')());
}
