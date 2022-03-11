/*
 * Copyright (c) 2022, 2022, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Tests potential memory leaks caused by final object property get node.
 */

load('assert.js');

let collected = [];
let expected = ["a", "b"];

let registry = new FinalizationRegistry((heldValue) => {
    collected.push(heldValue);
});

let a = {};
let b = {};
let o = {
    a: a,
    b: b,
};

registry.register(a, "a");
registry.register(b, "b");

assertSame(a, o.a);
assertSame(b, o.b);

a = null;
b = null;
o = null;

for (let i = 0; collected.length !== expected.length && i < 10; i++) {
    java.lang.System.gc();
    registry.cleanupSome();
}

if (collected.length < expected.length) {
    throw new Error(`${collected.length} out of ${expected.length} registered objects have been garbage-collected (expected: ${expected}, actual: ${collected}).`);
}
