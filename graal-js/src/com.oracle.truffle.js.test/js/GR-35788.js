/*
 * Copyright (c) 2022, 2023, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Regression test of Object/Map.groupBy.
 * 
 * @option ecmascript-version=staging
 * @option debug-builtin
 */

load('assert.js');

assertSame(2, Object.groupBy.length);
assertSame(2, Map.groupBy.length);

var groupResult = Object.groupBy([], x => x);
var prototype = Object.getPrototypeOf(groupResult);
assertSame(null, prototype);

// checks that Map.groupBy normalizes keys of the resulting map
var map = Map.groupBy([42], x => Debug.createSafeInteger(x));
assertTrue(map.get(42) !== undefined);

// checks that Object/Map.groupBy does not have a quadratic complexity
var size = 300000;
var array = [];
for (var i = 0; i < size; i++) {
    array[i] = i;
}
Object.groupBy(array, x => x);
Map.groupBy(array, x => x);
