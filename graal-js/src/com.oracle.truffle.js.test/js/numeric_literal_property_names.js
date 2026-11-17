/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

load('assert.js');

var { 0xFFFFFFFFFFFFF8: v } = { 0xFFFFFFFFFFFFF8: 'x' };
assertSame('x', v);
assertSameContent(['72057594037927930'], Object.keys({ 0xFFFFFFFFFFFFF8: 'x' }));

// Numeric property names must use the same rounding and decimal formatting as Number values.
for (const integer of [
    0n, 2147483648n, 4294967295n, 4294967296n,
    9007199254740991n, 9007199254740992n, 9007199254740993n,
    72057594037927928n, 9223372036854775807n, 9223372036854775808n
]) {
    const number = Number(integer);
    const key = String(number);
    for (const [radix, prefix] of [[2, '0b'], [8, '0o'], [10, ''], [16, '0x']]) {
        const literal = prefix + integer.toString(radix);
        const object = Function(`return ({ ${literal}: 'x' });`)();
        assertSameContent([key], Object.keys(object));
        assertSame('x', object[number]);

        const binding = Function('object', `var { ${literal}: value, ...rest } = object; return [value, Object.keys(rest).length];`);
        assertSameContent(['x', 0], binding(object));
        const assignment = Function('object', `var value; ({ ${literal}: value } = object); return value;`);
        assertSame('x', assignment(object));

        const method = Function(`return ({ ${literal}() { return 'x'; } });`)();
        assertSame('x', method[number]());
        assertSame(key, method[number].name);

        const accessor = Function(`return ({ get ${literal}() { return 'x'; }, set ${literal}(value) { this.value = value; } });`)();
        assertSame('x', accessor[number]);
        accessor[number] = 'y';
        assertSame('y', accessor.value);
        assertSame('get ' + key, Object.getOwnPropertyDescriptor(accessor, key).get.name);
        assertSame('set ' + key, Object.getOwnPropertyDescriptor(accessor, key).set.name);

        const C = Function(`return class { ${literal} = 'x'; static ${literal}() { return 'y'; } };`)();
        assertSame('x', new C()[number]);
        assertSame('y', C[number]());
        assertSame(key, C[number].name);

        // BigInt and quoted property names retain the exact integer spelling.
        const bigintObject = Function(`return ({ ${literal}n: 'x' });`)();
        assertSameContent([String(integer)], Object.keys(bigintObject));
        const quotedObject = Function(`return ({ '${integer}': 'x' });`)();
        assertSame('x', quotedObject[integer]);
    }
}
