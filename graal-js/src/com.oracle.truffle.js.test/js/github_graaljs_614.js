/*
 * Copyright (c) 2022, 2022, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

// Regression test for a bug in Java.isType()
// as reported by https://github.com/oracle/graaljs/issues/614

load('assert.js');

assertTrue(Java.isType(java.lang.Object));
assertFalse(Java.isType(java.lang.Object.class));
