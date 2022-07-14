/*
 * Copyright (c) 2022, 2022, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Tests of foreign getters and setters.
 */

load("assert.js");

var d = new java.util.Date();
var o = {};
Object.defineProperty(o, 'time', { get: d.getTime, set: d.setTime });

assertSame(d.getTime(), o.time);

o.time = 42;
assertSame(42, o.time);
assertSame(42, d.getTime());

d.setTime(211);
assertSame(211, o.time);
assertSame(211, d.getTime());
