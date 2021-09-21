/*
 * Copyright (c) 2021, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

// Tests an incorrect handling of Integer.MIN_VALUE
// as reported at https://github.com/oracle/graaljs/issues/506

load('assert.js');

assertSame(-2147483648, 1 << -1);
assertSame(2147483648, -(1 << -1));
