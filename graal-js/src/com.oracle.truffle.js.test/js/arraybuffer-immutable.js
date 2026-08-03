/*
 * Copyright (c) 2026, 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Immutable ArrayBuffer views reject indexed writes according to the strictness of the write.
 *
 * @option ecmascript-version=staging
 */

load("assert.js");

const view = new Uint8Array(new ArrayBuffer(1).transferToImmutable());

view[0] = 42;
assertSame(0, view[0]);

assertThrows(() => {
    'use strict';
    view[0] = 42;
}, TypeError);

// Indexed array writes dispatch to JSArrayBufferView.set(long, ...) when a view is a prototype.
const derivedArray = [];
Object.setPrototypeOf(derivedArray, view);

derivedArray[0] = 42;
assertFalse(Object.hasOwn(derivedArray, 0));

assertThrows(() => {
    'use strict';
    derivedArray[0] = 42;
}, TypeError);
