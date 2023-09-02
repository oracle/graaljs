/*
 * Copyright (c) 2023, 2023, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * @option ecmascript-version=staging
 */

load('../assert.js')

let extraClassInitCalled = 0;

@extraClassInit('b', 43)
@extraClassInit('a', 42)
class C {
    static staticMethod() {
        return C;
    }
}

function extraClassInit(name, value, ...rest) {
    assertSame(0, rest.length);
    return (classDef, context, ...rest) => {
        assertSame(0, rest.length);
        assertSame('class', context.kind);
        assertSame(classDef.name, context.name);
        assertSame('function', typeof context.addInitializer);
        assertSameContent(['kind', 'name', 'addInitializer'], Object.getOwnPropertyNames(context));

        assertTrue('staticMethod' in classDef);
        assertThrows(() => classDef.staticMethod(), ReferenceError); // classBinding has not been initialized yet!

        context.addInitializer(function (...rest) {
            assertSame(0, rest.length);
            assertSame(classDef, this);
            assertSame(classDef, classDef.staticMethod());
            this[name] = value;
            extraClassInitCalled++;
        });
    };
}

// make sure extra class initializers were called, and in the right order.
assertSame(2, extraClassInitCalled);
assertSame(42, C.a);
assertSame(43, C.b);
assertSameContent(['a', 'b'], Object.getOwnPropertyNames(C).slice(-2));
