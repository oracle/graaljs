/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Regression test for implicit use of lexical `this` via `super` in a nested arrow function.
 */
class C0 {
    constructor() {
        for (let i = 0;
            (() => {
                this; // explicit use of `this`
                for (let j = 0;
                    (() => {
                        super.toString(); // implicit use of `this`
                        return j < 1;
                    })();
                    j++) {
                }
                return i < 1;
            })();
            i++) {
        }
    }
}
new C0();

// bonus test case
class C1 extends C0 {
    constructor() {
        for (let i = 0;
            (() => {
                for (let j = 0;
                    (() => {
                        super();
                        return j < 0;
                    })();
                    j++) {
                }
                for (let j = 0;
                     (() => {
                         super.toString();
                         return j < 0;
                     })();
                     j++) {
                }
                this.toString();
                return i < 1;
            })();
            i++) {
            break;
        }
    }
}
new C1();
