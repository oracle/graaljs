/*
 * Copyright (c) 2020, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

load('../assert.js');

var javaNull = java.lang.System.getProperty("iDontExist");

assertSame(Object.prototype, Object.getPrototypeOf(Object(javaNull)));
assertSame(Object.prototype, Object.getPrototypeOf(new Object(javaNull)));
assertSame(null, Object.getPrototypeOf(Object.create(javaNull)));
