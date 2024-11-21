/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

load("assert.js");

const veryLargeIndex = 9.197828594769734e+307;

for (const rx of [/,/uy, /,/y, /,/]) {
    rx.constructor = function RegExp_(...args) {
        const splitter = Reflect.construct(RegExp, args, new.target);
        splitter.exec = function exec_(...args) {
            const res = RegExp.prototype.exec.apply(this, args);
            splitter.lastIndex = veryLargeIndex;
            return res;
        }
        return splitter;
    }
    rx.constructor[Symbol.species] = rx.constructor;

    const splitResult = "aa,bb,cc".split(rx);
    assertSame(2, splitResult.length);
    assertSame("aa", splitResult[0]);
    assertSame("", splitResult[1]);
}
