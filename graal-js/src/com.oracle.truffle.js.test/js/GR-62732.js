/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Test of the handling of replacement patterns in String.prototype.replaceAll('', _).
 */

load("./assert.js");

assertSame('$a$b$c$', 'abc'.replaceAll('', '$$'));
assertSame('abc', 'abc'.replaceAll('', '$&'));
assertSame('aababcabc', 'abc'.replaceAll('', '$`'));
assertSame('abcabcbcc', 'abc'.replaceAll('', "$'"));
