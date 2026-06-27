/*
 * Copyright (c) 2026, 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * @option ecmascript-version=staging
 */

load("assert.js");

let called = false;
const arg = {
    valueOf() {
        called = true;
        throw new Error("should not be converted");
    }
};

assertSame(undefined, Atomics.pause(arg));
assertSame(undefined, Atomics.pause(Symbol()));
assertSame(undefined, Atomics.pause(1.5));
assertSame(undefined, Atomics.pause(-1));
assertSame(undefined, Atomics.pause(1n));
assertSame(undefined, Atomics.pause(null));
assertSame(undefined, Atomics.pause(undefined));
assertFalse(called);
