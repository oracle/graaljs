/*
 * Copyright (c) 2022, 2022, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

// Tests that Object.setPrototypeOf() checks the validity of the prototype even
// for primitive arguments, as reported by https://github.com/oracle/graaljs/issues/653

load('assert.js');

assertThrows(() => Object.setPrototypeOf(0), TypeError);
assertThrows(() => Object.setPrototypeOf(0, true), TypeError);

assertSame('foo', Object.setPrototypeOf('foo', null));
assertSame(42n, Object.setPrototypeOf(42n, {}));
