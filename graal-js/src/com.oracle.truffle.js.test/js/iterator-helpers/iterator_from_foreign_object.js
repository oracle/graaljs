/*
 * Copyright (c) 2022, 2022, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Tests iterator helpers with foreign objects.
 *
 * @option iterator-helpers=true
 */

load("../assert.js");

let foreignObjectWithNextMethod = java.util.List.of().iterator();
let foreignIterable = java.util.List.of(41, 42, 43);

// Iterator methods are currently unsupported on foreign iterator/iterable objects.
for (let method of ['filter', 'map', 'reduce', 'every', 'find', 'some', 'flatMap', 'forEach', 'take', 'drop', 'toArray']) {
  assertThrows(() => Iterator.prototype[method].call(foreignObjectWithNextMethod), TypeError);
  assertThrows(() => AsyncIterator.prototype[method].call(foreignObjectWithNextMethod), TypeError);
  assertThrows(() => Iterator.prototype[method].call(foreignIterable), TypeError);
  assertThrows(() => AsyncIterator.prototype[method].call(foreignIterable), TypeError);
}

assertSameContent([41], Iterator.from(foreignIterable).take(1).toArray());
assertSameContent([42, 43], Iterator.from(foreignIterable).drop(1).toArray());

assertSameContent([41, 42, 43, 41, 42, 43], Iterator.from([10, 20]).flatMap(x => foreignIterable).toArray());
