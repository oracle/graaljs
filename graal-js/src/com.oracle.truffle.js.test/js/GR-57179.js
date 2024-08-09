/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

load("assert.js");

var array = [,-1];
for (var i = 1; i <= 1000; i++) {
    array.splice(0,0,i);
}

assertSame(1002, array.length);
for (var i = 0; i < array.length; i++) {
    var expected = (i === 1000) ? undefined : (1000 - i);
    assertSame(expected, array[i]);
}

// Regression test of an incorrect transition to zero-based array
array = [];
for (var i = 8; i >= 0; i--) {
    array[i] = i;
}
assertSameContent([0,1,2,3,4,5,6,7,8], array);
