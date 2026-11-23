/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Deleting a global lexical binding returns false, including before initialization.
 */

load('assert.js');

const compilers = [
    expression => Function(`return ${expression};`),
    expression => () => eval(expression),
    expression => () => (0, eval)(expression)
];
const names = ['globalLet', 'globalConst', 'GlobalClass'];
const deletes = [];
for (const compile of compilers) {
    for (const name of names) {
        // The lexical binding shadows a global property with the same name.
        globalThis[name] = 42;
        const remove = compile(`delete ${name}`);
        assertFalse(remove());
        assertSame(42, globalThis[name]);
        deletes.push(remove);

        // Reads still require an initialized binding.
        assertThrows(compile(name), ReferenceError);
        assertThrows(compile(`typeof ${name}`), ReferenceError);
    }
}

let globalLet = 1;
const globalConst = 2;
class GlobalClass {}

// Reuse the delete nodes after the lexical bindings have been initialized.
for (const remove of deletes) {
    assertFalse(remove());
}
assertSame(1, globalLet);
assertSame(2, globalConst);
assertSame('function', typeof GlobalClass);
for (const name of names) {
    assertSame(42, globalThis[name]);
    assertTrue(delete globalThis[name]);
}

// Missing lexical bindings continue to fall back to the global object.
var globalVar = 42;
for (const compile of compilers) {
    globalThis.dynamicDeleteProperty = 42;
    assertTrue(compile('delete dynamicDeleteProperty')());
    assertFalse(Object.hasOwn(globalThis, 'dynamicDeleteProperty'));
    assertFalse(compile('delete globalVar')());
    assertTrue(compile('delete missingGlobalToDelete')());
}
