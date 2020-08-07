/*
 * Copyright (c) 2020, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

// Tests that String.prototype.padStart and String.prototype.padEnd do not
// throw RangeError when the maxLength is large but filler is an empty string,
// as reported at https://github.com/graalvm/graaljs/issues/325

load('assert.js');

assertSame("42", "42".padStart(Infinity, ""));
assertSame("211", "211".padEnd(Infinity, ""));

true;
