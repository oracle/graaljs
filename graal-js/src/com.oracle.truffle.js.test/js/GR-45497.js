/*
 * Copyright (c) 2023, 2023, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Regression test of Set.prototype.size getter.
 */

load("assert.js");

const sizeOfThisSet = Object.getOwnPropertyDescriptor(Set.prototype, 'size').get;

assertThrows(() => sizeOfThisSet.call({}), TypeError);
assertSame(0, sizeOfThisSet.call(new Set()));
assertThrows(() => sizeOfThisSet.call(new Map()), TypeError);
