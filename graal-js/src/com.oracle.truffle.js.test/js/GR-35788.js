/*
 * Copyright (c) 2022, 2022, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Regression test of Array.prototype.groupBy(ToMap).
 * 
 * @option ecmascript-version=staging
 * @option debug-builtin
 */

load('assert.js');

assertSame(1, Array.prototype.groupBy.length);
assertSame(1, Array.prototype.groupByToMap.length);

var groupByResult = [].groupBy(x => x);
var prototype = Object.getPrototypeOf(groupByResult);
assertSame(null, prototype);

// checks that Array.prototype.groupByToMap normalizes keys of the resulting map
var map = [42].groupByToMap(x => Debug.createSafeInteger(x));
assertTrue(map.get(42) !== undefined);

// checks that Array.prototype.groupBy(ToMap) does not have a quadratic complexity
var size = 300000;
var array = [];
for (var i = 0; i < size; i++) {
    array[i] = i;
}
array.groupBy(x => x);
array.groupByToMap(x => x);
