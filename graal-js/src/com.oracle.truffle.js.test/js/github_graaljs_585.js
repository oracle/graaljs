/*
 * Copyright (c) 2022, 2022, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

// Tests parseInt(number, radix) with radix < 10
// as reported by https://github.com/oracle/graaljs/issues/585

load('assert.js');

assertSame(8, parseInt(108, 8));
assertSame(8, parseInt(108.5, 8));
assertTrue(isNaN(parseInt(801, 8)));
assertTrue(isNaN(parseInt(801.5, 8)));
