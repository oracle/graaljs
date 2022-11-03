/*
 * Copyright (c) 2022, 2022, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Tests async iterator helpers.
 *
 * @option iterator-helpers=true
 * @option unhandled-rejections=throw
 */

load("iteratorhelper_common.js");

function testMap(method, expectedValues, mapper) {
  let iterator = Iterator.from([41, 42, 43, 44]);
  let iteratorHelper = iterator[method](mapper);

  for (let expectedValue of expectedValues) {
    assertIterResult({done: false, value: expectedValue}, iteratorHelper.next());
  }

  assertIterResult({done: true, value: undefined}, iteratorHelper.next());
}

function testReduce(method, reducer, ...args) {
  let iterator = Iterator.from([41, 42, 43, 44]);
  return iterator[method](reducer, ...args);
}

{
  let expected = [0, 41, 410, 1, 42, 420, 2, 43, 430, 3, 44, 440];
  testMap('flatMap', expected, (x, i) => [i, x, x * 10]);
  testMap('flatMap', expected, (x, i) => [i, x, x * 10].values());
  testMap('flatMap', expected, (x, i) => Iterator.from([i, x, x * 10]));
}

{
  testMap('map', [410, 421, 432, 443], (x, i) => x * 10 + i);
}

{
  testMap('filter', [41, 43], (x, i) => x % 2 == 1);
  testMap('filter', [42, 44], (x, i) => x % 2 == 0);
  testMap('filter', [41, 42], (x, i) => i < 2);
  testMap('filter', [43, 44], (x, i) => i >= 2);
}

{
  assertSame("41,42#1,43#2,44#3",      testReduce('reduce', (a, b, i) => a + "," + b + "#" + i));
  assertSame("40,41#0,42#1,43#2,44#3", testReduce('reduce', (a, b, i) => a + "," + b + "#" + i, 40));
}

{
  let expected = [0, 41, 1, 42, 2, 43, 3, 44];
  let ctr = 0;
  assertSame(undefined, testReduce('forEach', (x, i) => { ctr++; assertSame(i, expected[i * 2]), assertSame(x, expected[i * 2 + 1]); return x; }));
  assertSame(undefined, testReduce('forEach', (x, i) => { ctr++; assertSame(i, expected[i * 2]), assertSame(x, expected[i * 2 + 1]); return x; }));
  assertSame(ctr, 8); ctr = 0;


  assertSame(true,      testReduce('every',   (x, i) => { ctr++; assertSame(i, expected[i * 2]), assertSame(x, expected[i * 2 + 1]); return i <  4; }));
  assertSame(false,     testReduce('every',   (x, i) => { ctr++; assertSame(i, expected[i * 2]), assertSame(x, expected[i * 2 + 1]); return i <  3; }));
  assertSame(ctr, 8); ctr = 0;


  assertSame(false,     testReduce('some',    (x, i) => { ctr++; assertSame(i, expected[i * 2]), assertSame(x, expected[i * 2 + 1]); return i >= 4; }));
  assertSame(true,      testReduce('some',    (x, i) => { ctr++; assertSame(i, expected[i * 2]), assertSame(x, expected[i * 2 + 1]); return i >= 3; }));
  assertSame(ctr, 8); ctr = 0;


  assertSame(43,        testReduce('find',    (x, i) => { ctr++; assertSame(i, expected[i * 2]), assertSame(x, expected[i * 2 + 1]); return x === 43; }));
  assertSame(44,        testReduce('find',    (x, i) => { ctr++; assertSame(i, expected[i * 2]), assertSame(x, expected[i * 2 + 1]); return i >= 3; }));
  assertSame(undefined, testReduce('find',    (x, i) => { ctr++; assertSame(i, expected[i * 2]), assertSame(x, expected[i * 2 + 1]); return i >= 4; }));
  assertSame(ctr, 11); ctr = 0;
}

{
  let ctr = 0;
  Iterator.from([]).take({ valueOf() { ctr++; return 0; } }).toArray();
  assertSame(ctr, 1); ctr = 0;

  Iterator.from([]).drop({ valueOf() { ctr++; return 0; } }).toArray();
  assertSame(ctr, 1); ctr = 0;

  assertSame(undefined, Iterator.from([]).reduce(() => 42, undefined));
}
