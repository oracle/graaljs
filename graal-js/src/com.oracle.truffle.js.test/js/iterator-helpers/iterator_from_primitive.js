/*
 * Copyright (c) 2022, 2022, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Tests GetIteratorFlattenable error cases.
 *
 * @option iterator-helpers=true
 */

load("../assert.js");

function testIteratorFromPrimitive(Iterator) {
  assertThrows(() => Iterator.from(42), TypeError);

  Number.prototype[Symbol.iterator] = function() { throw new Error('[Symbol.iterator]() should not be invoked!'); }
  Number.prototype[Symbol.asyncIterator] = function() { throw new Error('[Symbol.asyncIterator]() should not be invoked!'); }
  assertThrows(() => Iterator.from(42), TypeError);
  delete Number.prototype[Symbol.asyncIterator];
  delete Number.prototype[Symbol.iterator];

  Number.prototype[Symbol.iterator] = 42
  assertThrows(() => Iterator.from(42), TypeError);
  delete Number.prototype[Symbol.iterator];

  Number.prototype.next = function() {}
  assertThrows(() => Iterator.from(42), TypeError);
  delete Number.prototype.next;
}

testIteratorFromPrimitive(Iterator);
testIteratorFromPrimitive(AsyncIterator);
