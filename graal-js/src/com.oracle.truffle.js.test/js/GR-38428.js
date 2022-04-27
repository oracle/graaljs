/*
 * Copyright (c) 2022, 2022, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

load("assert.js");

(function testZeroBasedObjectArray() {
    var a = new Array(3);
    a[0] = 42;
    a[1] = 'foo';
    a.splice(0,1);
    assertSame(undefined, a[1]);
})();

(function testZeroBasedJSObjectArray() {
    var a = new Array(3);
    a[0] = {};
    a[1] = {};
    a.splice(0,1);
    assertSame(undefined, a[1]);
})();

(function testZeroBasedIntArray() {
    var a = new Array(3);
    a[0] = 42;
    a[1] = 211;
    a.splice(0,1);
    assertSame(undefined, a[1]);
})();

(function testZeroBasedDoubleArray() {
    var a = new Array(3);
    a[0] = 4.2;
    a[1] = 2.11;
    a.splice(0,1);
    assertSame(undefined, a[1]);
})();
