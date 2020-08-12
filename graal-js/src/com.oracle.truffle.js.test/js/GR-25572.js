/*
 * Copyright (c) 2020, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

'use strict';

load('assert.js');

assertSame(Infinity, JSON.parse("1e10000000000"));
assertSame(-Infinity, JSON.parse("-1e10000000000"));
assertSame(0, JSON.parse("0e10000000000"));

var result = JSON.parse("-0e10000000000");
// result should be the negative zero
assertSame(0, result);
assertSame(-Infinity, 1/result);

true;
