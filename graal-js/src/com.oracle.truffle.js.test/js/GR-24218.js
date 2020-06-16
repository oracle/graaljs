/*
 * Copyright (c) 2020, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

load('assert.js');

let array = new Uint8Array(8);

array.set(211);
array.set(42n);
array.set(Math.PI);
array.set(Symbol());
array.set(true);

for (let i=0; i<array.length; i++) {
    assertSame(0, array[i]);
}

array.set('246');

for (let i=0; i<array.length; i++) {
    let expected = (i < 3) ? 2*(i+1) : 0;
    assertSame(expected, array[i]);
}

let list = new java.util.ArrayList();
list.add(20);
list.add(15);
list.add(10);
list.add(5);
array.set(list);

for (let i=0; i<array.length; i++) {
    let expected = (i < 4) ? (20-5*i) : 0;
    assertSame(expected, array[i]);
}

true;
