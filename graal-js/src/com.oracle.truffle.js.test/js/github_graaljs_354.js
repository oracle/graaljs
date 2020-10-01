/*
 * Copyright (c) 2020, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

// Tests that Java objects can be passed to Array.from()
// as reported at https://github.com/graalvm/graaljs/issues/354

load('assert.js');

const set = new java.util.HashSet();
set.add('test');
let array = Array.from(set);
assertSame(1, array.length);
assertSame('test', array[0]);

const object = new java.lang.Object();
array = Array.from(object);
assertSame(0, array.length);

true;
