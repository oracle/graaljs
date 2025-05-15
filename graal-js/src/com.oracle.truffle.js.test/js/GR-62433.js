/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Regression test for implicit use of lexical `this` via `super` in a nested arrow function.
 */
new class C1 {
    constructor() {
        (() => {
            for (let i = (() => super.a = 0)();
                i >= 0;
                (() => (this, i--))()) {
            }
        })();
    }
};

// original test case
class C2 {
    constructor(a4, a5) {
        class C6 {
        }
        for (let i9 = 0, i10 = 10;
            i10--;
            (() => {
                for (let [i15, i16] = (() => {
                        super.a = 0;
                        return [0, 10];
                    })();
                    i16;
                    (() => {
                        this * Uint8Array;
                        C6[8] = "o";
                        i16--;
                    })()) {
                }
            })()) {
        }
    }
}
new C2();
