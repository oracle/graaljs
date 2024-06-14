/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

load("assert.js");

var time = java.lang.System.currentTimeMillis?.();
assertSame('number', typeof time);

var result = java.lang.System.iDontExist?.();
assertSame('undefined', typeof result);
