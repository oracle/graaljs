/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Regression test for an incorrect guard in ToArrayLengthNode.
 */

load("./assert.js");

assertThrows(() => new Array(-1), RangeError)

var o = new java.lang.Object();
var array = new Array(o);
assertSameContent([o], array);

array = new Array(java.lang.Long.valueOf(42));
assertSameContent(new Array(42), array);
