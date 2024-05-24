/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * CreateDataPropertyOrThrow on typed array should throw TypeError for invalid index.
 *
 * @option debug-builtin
 */

load("assert.js");

const TypedArrayConstructors =
    Object.getOwnPropertyNames(globalThis)
    .filter(name => name.match(/.*\d+.*Array$/))
    .map(name => globalThis[name]);

for (const TypedArray of TypedArrayConstructors) {
    const isBigInt = TypedArray.name.startsWith("Big");
    let thiz;
    class C extends TypedArray {
        [0] = isBigInt ? 42n : 42;

        constructor(...args) {
            super(...args);
            thiz = this;
        }
    }

    // 0 is an invalid typed array index (out of bounds)
    assertThrows(() => new C(), TypeError);
    assertThrows(() => new C(0), TypeError);
    // 0 is a valid typed array index (in bounds)
    assertSame(42, Number(new C(1)[0]));

    C.from(isBigInt ? [2n, 3n] : [2, 3], x => {
        // if (!thiz.buffer.detached) thiz.buffer.transfer();
        Debug.typedArrayDetachBuffer(thiz.buffer);
        return x;
    });
}
