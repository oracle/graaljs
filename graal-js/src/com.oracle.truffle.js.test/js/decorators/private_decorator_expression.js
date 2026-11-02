/*
 * Copyright (c) 2026, 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Decorator expressions may access private members, but must not start with a bare private name.
 *
 * @option ecmascript-version=staging
 */

load('../assert.js');

for (const decorator of ['@#dec', '@#dec()']) {
    for (const source of [
        `class C { #dec; method() { ${decorator} class D {} } }`,
        `class C { #dec; method() { return ${decorator} class {}; } }`,
        `class C { #dec; ${decorator} method() {} }`,
        `class C { #dec; ${decorator} field; }`
    ]) {
        assertThrows(() => Function(source), SyntaxError);
    }
}

for (const decorator of ['@C.#dec', '@(C.#dec)', '@(this.#dec)', '@C.#factory()']) {
    const C = Function(`return class C {
        static #dec(value) { return value; }
        static #factory() { return this.#dec; }
        static create() {
            ${decorator} class D {
                ${decorator} method() { return 42; }
            }
            return D;
        }
    };`)();
    const D = C.create();
    assertSame('D', D.name);
    assertSame(42, new D().method());
}
