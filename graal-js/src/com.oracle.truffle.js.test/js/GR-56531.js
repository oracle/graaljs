/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

load("assert.js");

var array = [42,,211];
array.length = 2;
assertSameContent([], array.splice(2, 0, 1, 2, 3));
assertSameContent([42,,1,2,3], array);

// Original test-case from the fuzzer

const v2 = [-361.1425116275642];
v2[2] = 2.0;
v2.length = 2;
v2.splice(113, 113, 113, [-65537,-2147483648,8], 113);
