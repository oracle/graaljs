/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Tests that Iterator.prototype.join closes the iterator when the result exceeds the implementation string length limit.
 *
 * @option ecmascript-version=staging
 * @option string-length-limit=20
 */

load("../assert.js");

let closed = false;
const iterator = {
    next() {
        return {done: false, value: "1234567890"};
    },
    return() {
        closed = true;
        throw new Error("suppressed close error");
    }
};

assertThrows(() => Iterator.prototype.join.call(iterator), RangeError);
assertTrue(closed);
