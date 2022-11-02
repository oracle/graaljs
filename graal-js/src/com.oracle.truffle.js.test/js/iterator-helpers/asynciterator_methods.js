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

async function testMap(method, expectedValues, mapper) {
  let asyncIterator = AsyncIterator.from([41, 42, 43, 44]);
  let iteratorHelper = asyncIterator[method](mapper);

  for (let expectedValue of expectedValues) {
    assertIterResult({done: false, value: expectedValue}, await iteratorHelper.next());
  }

  assertIterResult({done: true, value: undefined}, await iteratorHelper.next());
}

async function testReduce(method, reducer, ...args) {
  let asyncIterator = AsyncIterator.from([41, 42, 43, 44]);
  return await asyncIterator[method](reducer, ...args);
}

(async function main() {
  try {
    {
      let expected = [0, 41, 410, 1, 42, 420, 2, 43, 430, 3, 44, 440];
      await testMap('flatMap', expected, (x, i) => [i, x, x * 10]);
      await testMap('flatMap', expected, (x, i) => [i, x, x * 10].values());
      await testMap('flatMap', expected, (x, i) => Iterator.from([i, x, x * 10]));
      await testMap('flatMap', expected, (x, i) => AsyncIterator.from([i, x, x * 10]));
      await testMap('flatMap', expected, (x, i) => Promise.resolve([i, x, x * 10]));
    }

    {
      let expected = [410, 421, 432, 443];
      await testMap('map', expected, (x, i) => x * 10 + i);
      await testMap('map', expected, (x, i) => Promise.resolve(x * 10 + i));
    }

    {
      await testMap('filter', [41, 43], (x, i) => x % 2 == 1);
      await testMap('filter', [42, 44], (x, i) => x % 2 == 0);
      await testMap('filter', [41, 42], (x, i) => i < 2);
      await testMap('filter', [43, 44], (x, i) => i >= 2);

      await testMap('filter', [41, 43], (x, i) => Promise.resolve(x % 2 == 1));
      await testMap('filter', [42, 44], (x, i) => Promise.resolve(x % 2 == 0));
      await testMap('filter', [41, 42], (x, i) => Promise.resolve(i < 2));
      await testMap('filter', [43, 44], (x, i) => Promise.resolve(i >= 2));
    }

    {
      assertSame("41,42#1,43#2,44#3",      await testReduce('reduce', (a, b, i) => a + "," + b + "#" + i));
      assertSame("40,41#0,42#1,43#2,44#3", await testReduce('reduce', (a, b, i) => a + "," + b + "#" + i, 40));
    }

    {
      let expected = [0, 41, 1, 42, 2, 43, 3, 44];
      let ctr = 0;
      assertSame(undefined, await testReduce('forEach', (x, i) => { ctr++; assertSame(i, expected[i * 2]), assertSame(x, expected[i * 2 + 1]); return x; }));
      assertSame(undefined, await testReduce('forEach', (x, i) => { ctr++; assertSame(i, expected[i * 2]), assertSame(x, expected[i * 2 + 1]); return x; }));
      assertSame(ctr, 8); ctr = 0;


      assertSame(true,      await testReduce('every',   (x, i) => { ctr++; assertSame(i, expected[i * 2]), assertSame(x, expected[i * 2 + 1]); return i <  4; }));
      assertSame(false,     await testReduce('every',   (x, i) => { ctr++; assertSame(i, expected[i * 2]), assertSame(x, expected[i * 2 + 1]); return i <  3; }));
      assertSame(ctr, 8); ctr = 0;


      assertSame(false,     await testReduce('some',    (x, i) => { ctr++; assertSame(i, expected[i * 2]), assertSame(x, expected[i * 2 + 1]); return i >= 4; }));
      assertSame(true,      await testReduce('some',    (x, i) => { ctr++; assertSame(i, expected[i * 2]), assertSame(x, expected[i * 2 + 1]); return i >= 3; }));
      assertSame(ctr, 8); ctr = 0;

      assertSame(43,        await testReduce('find',    (x, i) => { ctr++; assertSame(i, expected[i * 2]), assertSame(x, expected[i * 2 + 1]); return x === 43; }));
      assertSame(44,        await testReduce('find',    (x, i) => { ctr++; assertSame(i, expected[i * 2]), assertSame(x, expected[i * 2 + 1]); return i >= 3; }));
      assertSame(undefined, await testReduce('find',    (x, i) => { ctr++; assertSame(i, expected[i * 2]), assertSame(x, expected[i * 2 + 1]); return i >= 4; }));
      assertSame(ctr, 11); ctr = 0;
    }

    {
      let ctr = 0;
      await AsyncIterator.from([]).take({ valueOf() { ctr++; return 0; } }).toArray();
      assertSame(ctr, 1); ctr = 0;

      await AsyncIterator.from([]).drop({ valueOf() { ctr++; return 0; } }).toArray();
      assertSame(ctr, 1); ctr = 0;
    }

    debugLog("DONE");
  } catch (e) {
    console.error(e.stack ?? e);
    throw e;
  }
})();
