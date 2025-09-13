/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Regression tests of possible memory leaks during destructuring assignment.
 */

load("./assert.js")

// array destructuring plain
var o = new java.awt.Point();
var array = [o];
var ref = new java.lang.ref.WeakReference(o);
o = null;
[x] = array;
array = null;
x = null;
java.lang.System.gc();
assertSame(null, ref.get());

// array destructuring throwing
const y = 42;
o = new java.awt.Point();
array = [o];
ref = new java.lang.ref.WeakReference(o);
o = null;
var caughtException = false;
try {
    [y] = array;
} catch (e) {
    caughtException = true;
}
assertTrue(caughtException);
array = null;
java.lang.System.gc();
assertSame(null, ref.get());

// object destructuring plain
o = new java.awt.Point();
ref = new java.lang.ref.WeakReference(o);
var {x} = o;
o = null;
java.lang.System.gc();
assertSame(null, ref.get());

// object destructuring throwing
o = new java.awt.Point();
ref = new java.lang.ref.WeakReference(o);
var caughtException = false;
try {
    var { [null.x]: x } = o;
} catch (e) {
    caughtException = true;
}
assertTrue(caughtException);
o = null;
java.lang.System.gc();
assertSame(null, ref.get());
