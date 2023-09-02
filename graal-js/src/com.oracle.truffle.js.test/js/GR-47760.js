/*
 * Copyright (c) 2023, 2023, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

load("assert.js");

var stringified = "1234567890123456789";
var long = java.lang.Long.valueOf(stringified);
assertSame(stringified, long.toString());
assertSame(stringified, String(long));
assertSame(stringified, '' + long);
