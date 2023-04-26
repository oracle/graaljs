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

let createForeignObjectWithNextMethod = () => java.util.List.of({ value: 2 }, { value: 6 }, { value: 4 }, { done: true}).iterator();
let foreignIterable = java.util.List.of(41, 42, 43);

assertSameContent([2,4], Iterator.prototype.filter.call(createForeignObjectWithNextMethod(), x => x < 5).toArray());
assertSameContent([4,36,16], Iterator.prototype.map.call(createForeignObjectWithNextMethod(), x => x*x).toArray());
assertSame(12, Iterator.prototype.reduce.call(createForeignObjectWithNextMethod(), (sum,x) => sum+x, 0));
assertSame(true, Iterator.prototype.every.call(createForeignObjectWithNextMethod(), x => x > 0));
assertSame(false, Iterator.prototype.every.call(createForeignObjectWithNextMethod(), x => x < 5));
assertSame(6, Iterator.prototype.find.call(createForeignObjectWithNextMethod(), x => x > 5));
assertSame(undefined, Iterator.prototype.find.call(createForeignObjectWithNextMethod(), x => x < 0));
assertSame(true, Iterator.prototype.some.call(createForeignObjectWithNextMethod(), x => x > 5));
assertSame(false, Iterator.prototype.some.call(createForeignObjectWithNextMethod(), x => x < 0));
assertSameContent([2,6], Iterator.prototype.take.call(createForeignObjectWithNextMethod(), 2).toArray());
assertSameContent([6,4], Iterator.prototype.drop.call(createForeignObjectWithNextMethod(), 1).toArray());
assertSameContent([2,4,6,36,4,16], Iterator.prototype.flatMap.call(createForeignObjectWithNextMethod(), x => [x, x*x]).toArray());

let forEachResult = [];
Iterator.prototype.forEach.call(createForeignObjectWithNextMethod(), x => forEachResult.push(x));
assertSameContent([2,6,4], forEachResult);

assertSameContent([41], Iterator.from(foreignIterable).take(1).toArray());
assertSameContent([42, 43], Iterator.from(foreignIterable).drop(1).toArray());

assertSameContent([41, 42, 43, 41, 42, 43], Iterator.from([10, 20]).flatMap(x => foreignIterable).toArray());
