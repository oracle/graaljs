/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Tests that string case conversion enforces the implementation string length limit.
 *
 * @option string-length-limit=20
 */

load("./assert.js");

assertSame("SS".repeat(10), "\u00df".repeat(10).toUpperCase());
assertThrows(() => "\u00df".repeat(11).toUpperCase(), RangeError);
assertThrows(() => "\u00df".repeat(11).toLocaleUpperCase("en"), RangeError);

assertSame("i\u0307".repeat(10), "\u0130".repeat(10).toLowerCase());
assertThrows(() => "\u0130".repeat(11).toLowerCase(), RangeError);
assertThrows(() => "\u0130".repeat(11).toLocaleLowerCase("en"), RangeError);
