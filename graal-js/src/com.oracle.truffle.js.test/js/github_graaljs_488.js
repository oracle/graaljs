/*
 * Copyright (c) 2021, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

// Tests some corner cases of String.prototype.replace
// as reported at https://github.com/oracle/graaljs/issues/488

load('assert.js');

let evil = new RegExp;
evil.exec = () => ({ 0: "1234567", length: 1, index: 0 });
assertSame("", "abc".replace(evil, "$'"));

evil.exec = () => ({ 0: "x", length: 1, index: 3 });
assertSame("abc", "abc".replace(evil, "$'"));
