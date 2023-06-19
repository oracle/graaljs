/*
 * Copyright (c) 2023, 2023, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

load("assert.js");

var match = /(.)(.)/.exec('ab');
match.splice(1,1); // materialize the lazy regex result array
var desc = Object.getOwnPropertyDescriptor(match, 'index');
assertSame(0, desc.value);
assertSame(true, desc.writable);
assertSame(true, desc.enumerable);
assertSame(true, desc.configurable);
