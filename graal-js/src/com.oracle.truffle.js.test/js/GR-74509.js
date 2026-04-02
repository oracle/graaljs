/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

load("assert.js");

let maxCalled = 0;
assertSame(4.5, Math.max(1, 2, 3, {
    valueOf() {
        maxCalled++;
        return 4.5;
    }
}));
assertSame(1, maxCalled);

let minCalled = 0;
assertSame(-4.5, Math.min(1, 2, 3, {
    valueOf() {
        minCalled++;
        return -4.5;
    }
}));
assertSame(1, minCalled);
