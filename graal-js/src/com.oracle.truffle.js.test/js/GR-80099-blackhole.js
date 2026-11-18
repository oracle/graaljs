/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * @option debug-builtin
 */

load("assert.js");

const object = {
    valueOf() { throw new Error("blackhole must not coerce its argument"); },
    toString() { throw new Error("blackhole must not stringify its argument"); }
};
const values = [0, -1, 2147483647, 1.5, NaN, Infinity, 1n, "value", true,
                undefined, null, Symbol("value"), object];
assertSame(1, Debug.blackhole.length);
assertSame(undefined, Debug.blackhole());
for (let i = 0; i < 1000; i++) {
    for (let j = 0; j < values.length; j++) {
        assertSame(undefined, Debug.blackhole(values[j]));
    }
}
let evaluated = 0;
Debug.blackhole(++evaluated);
assertSame(1, evaluated);

const consume = Debug.blackhole;
for (let i = 0; i < 1000; i++) {
    assertSame(undefined, consume(consume));
    assertSame(undefined, consume(i));
}
assertSame(undefined, consume(++evaluated, ++evaluated));
assertSame(3, evaluated);
assertSame(undefined, consume.call(null, object));
assertSame(undefined, consume.apply(null, [42]));
