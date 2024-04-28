/*
 * Copyright (c) 2024, 2024, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Regression test of the handling of long values.
 */

load('assert.js');

var long0 = java.lang.Long.parseLong("0");
var long20 = java.lang.Long.parseLong("20");

assertSame('22', 42..toString(long20));
assertSameContent([], [].splice(long0));
