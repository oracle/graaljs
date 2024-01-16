/*
 * Copyright (c) 2024, 2024, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Regression test checking the handling of the hole value.
 */

load("assert.js");

var array = new Array(4);
array[2] = -1 << 31; // hole value for int arrays
array[4] = 42;

assertSameContent([,,-2147483648,,42], array);
