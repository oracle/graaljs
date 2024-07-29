/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Value conversion in TypedArray.prototype.set can cause source array strategy transition.
 */

load("assert.js");

const sa = [30, 31, {
        toString() {
            sa[1073741825] = 36; // make array sparse
            return "32";
        }
    }, 33, 34, 35];

const ta = new Int16Array(9);
ta.set(sa);

assertSameContent([30,31,32,33,34,35,0,0,0], ta);

// Original test case
const v0 = [-267454138,886544441,-9007199254740991,-4294967297,-12,9007199254740990];
function f1(a2) {
    v0[1073741825] = v0; // make array sparse
    return a2;
}
v0.toString = f1;
v0[2] = v0;
const v5 = new Int16Array(9);
v5["set"](v0);

assertSameContent([-1722,-26567,0,-1,-12,-2,0,0,0], v5);
