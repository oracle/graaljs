/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

load("assert.js");

const veryLargeIndex = 9.197828594769734e+307;

// match
for (const v1 of [/(?:)/guy, /(?:)/gy, /(?:)/g]) {
    v1.exec = function exec_(...args) {
        const res = RegExp.prototype.exec.apply(this, args);
        v1.lastIndex = veryLargeIndex;
        return res;
    }
    const m1 = "11".match(v1);

    assertSame(1, m1.length);
    assertSame("", m1[0]);
}

// matchAll
for (const v2 of [/(?:)/guy, /(?:)/gy, /(?:)/g]) {
    v2.constructor = function RegExp_(...args) {
        const re = Reflect.construct(RegExp, args, new.target);
        re.exec = function exec_(...args) {
            const res = RegExp.prototype.exec.apply(this, args);
            re.lastIndex = veryLargeIndex;
            return res;
        }
        return re;
    }
    v2.constructor[Symbol.species] = v2.constructor;
    const m2 = [..."11".matchAll(v2)];

    assertSame(1, m2.length);
    assertTrue(Array.isArray(m2[0]));
    assertSame(1, m2[0].length);
    assertSame("", (m2[0][0]));
}
