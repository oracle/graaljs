/*
 * Copyright (c) 2022, 2022, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

// Tests how Java null is handled by Array.prototype.join()
// as reported by https://github.com/oracle/graaljs/issues/584

load('assert.js');

var javaNull = java.lang.System.getProperty("iDontExist");
assertSame('', [javaNull].join());
assertSame(',', [javaNull, javaNull].join());
assertSame(',,', [javaNull, javaNull, javaNull].join());
