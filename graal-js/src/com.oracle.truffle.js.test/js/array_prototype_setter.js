/*
 * Copyright (c) 2024, 2024, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Regression test of the handling of a setter on the prototype chain of an array.
 */

load("assert.js");

Object.setPrototypeOf(Array.prototype, {set[1] (x) {}});

[42, Math.PI, 'foo', {}].forEach(value => {
    var array = [];
    assertSame(0, array.length);
    array[1] = value;
    assertSame(0, array.length); // No own property has been changed/set.
});
