/*
 * Copyright (c) 2022, 2022, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * @option ecmascript-version=staging
 */

load('../assert.js')

let counter = 0;

let Cs = [];
for (let i = 0; i < 2; i++) {
    Cs[i] = class C {
        accessor bla = counter++;
    };
}

let C0 = Cs[0];
let C1 = Cs[1];

let c0 = new C0();
let c1 = new C1();

assertSame(0, c0.bla);
assertSame(1, c1.bla);

let bla = Object.getOwnPropertyDescriptor(C0.prototype, "bla");
assertSame(0, bla.get.apply(c0));

//---------------------------------------------//
assertThrows(() => {

    console.log(bla.get.apply(c1));

}, TypeError, "Bad auto-accessor target.");
