/*
 * Copyright (c) 2022, 2023, Oracle and/or its affiliates. All rights reserved.
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

let createJavaIterator = () => java.util.List.of(2, 6, 4).iterator();
let foreignIterable = java.util.List.of(41, 42, 43);

assertSameContent([2,4], createJavaIterator().filter(x => x < 5).toArray());
assertSameContent([4,36,16], createJavaIterator().map(x => x*x).toArray());
assertSame(12, createJavaIterator().reduce((sum,x) => sum+x, 0));
assertSame(true, createJavaIterator().every(x => x > 0));
assertSame(false, createJavaIterator().every(x => x < 5));
assertSame(6, createJavaIterator().find(x => x > 5));
assertSame(undefined, createJavaIterator().find(x => x < 0));
assertSame(true, createJavaIterator().some(x => x > 5));
assertSame(false, createJavaIterator().some(x => x < 0));
assertSameContent([2,6], createJavaIterator().take(2).toArray());
assertSameContent([6,4], createJavaIterator().drop(1).toArray());
assertSameContent([2,4,6,36,4,16], createJavaIterator().flatMap(x => [x, x*x]).toArray());

let forEachResult = [];
createJavaIterator().forEach(x => forEachResult.push(x));
assertSameContent([2,6,4], forEachResult);

assertSameContent([41], Iterator.from(foreignIterable).take(1).toArray());
assertSameContent([42, 43], Iterator.from(foreignIterable).drop(1).toArray());

assertSameContent([41, 42, 43, 41, 42, 43], Iterator.from([10, 20]).flatMap(x => foreignIterable).toArray());
