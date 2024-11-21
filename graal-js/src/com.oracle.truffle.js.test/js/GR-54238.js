/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

load("assert.js");

class C1 extends Array {
    constructor() {
        for (let i7 = 0;
            (() => {
                super();
                return i7 < 1;
            })(); i7++
            ) {
        }
    }
}
assertThrows(() => new C1(), ReferenceError); // super() called twice

class C2 extends Object {
    constructor() {
        let i7;
        for (i7 = 0;
            (() => {
                super();
                return i7 < 1;
            })(); i7++
            ) {
        }
    }
}
assertThrows(() => new C2(), ReferenceError); // super() called twice

class C3 extends Array {
    constructor(n = 0) {
        return ((() => (() => super(n))())());
    }
}
new C3();

class Cnt extends Object {
    constructor() {
        for (let i7 = 0;
            (() => {
                return new.target === Cnt && i7 < 1;
            })(); i7++
            ) {
            super();
        }
    }
}
new Cnt();
