/*
 * Copyright (c) 2022, 2022, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Tests potential memory leaks caused by Array.prototype.splice
 */

load('assert.js');

var collected = 0;
var expected = 4;

var registry = new FinalizationRegistry(function() {
    collected++;
});

var o1 = {};
var array1 = [o1];
registry.register(o1);
array1.splice(0,1);
o1 = null;

var o2 = {};
var array2 = [];
array2.push(o2);
registry.register(o2);
array2.splice(0,1);
o2 = null;

var o3 = {};
var array3 = [,o3];
registry.register(o3);
array3.splice(1,1);
o3 = null;

var o4 = {};
var array4 = [,42,o4];
registry.register(o4);
array4.splice(2,1);
o4 = null;

for (var i = 0; collected !== expected && i < 10; i++) {
    java.lang.System.gc();
    registry.cleanupSome();
}

assertSame(expected, collected);

