/*
 * Copyright (c) 2020, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

// Tests the length of function that is using binding pattern in rest parameter
// as reported at https://github.com/graalvm/graaljs/issues/328

load('assert.js');

assertSame(0, (function(...[x]) {}).length);
assertSame(0, (function(...{x}) {}).length);
assertSame(1, (function(y, ...[x]) {}).length);
assertSame(1, (function(y, ...{x}) {}).length);

true;
