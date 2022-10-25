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

async function testDropPromiseValue(n) {
  let asyncIterator = new AsyncIteratorPromiseValue();
  let iteratorHelper = asyncIterator.drop(n);
  assertSame(0, asyncIterator.nextCalls);

  assertIterResult({done: false, value: 41 + n}, await iteratorHelper.next());
  assertIterResult({done: false, value: 42 + n}, await iteratorHelper.next());

  assertIterResult({done: true, value: undefined}, await iteratorHelper.return());
  assertIterResult({done: true, value: undefined}, await iteratorHelper.next());

  assertSame(n + 2, asyncIterator.nextCalls);
  assertSame(1, asyncIterator.returnCalls);
}

async function testTakePromiseValue(n) {
  let asyncIterator = new AsyncIteratorPromiseValue();
  let iteratorHelper = asyncIterator.take(n);
  assertSame(0, asyncIterator.nextCalls);

  for (let i = 0; i < n; i++) {
    assertIterResult({done: false, value: 41 + i}, await iteratorHelper.next());
  }

  assertIterResult({done: true, value: undefined}, await iteratorHelper.next());

  assertSame(n, asyncIterator.nextCalls);
  assertSame(1, asyncIterator.returnCalls);
}

async function testMapPromiseValue() {
  let asyncIterator = new AsyncIteratorPromiseValue();
  let iteratorHelper = asyncIterator.map(async x => await x + 13);

  assertIterResult({done: false, value: 41 + 13}, await iteratorHelper.next());
  assertIterResult({done: false, value: 42 + 13}, await iteratorHelper.next());

  assertIterResult({done: true, value: undefined}, await iteratorHelper.return());
  assertIterResult({done: true, value: undefined}, await iteratorHelper.next());

  assertSame(2, asyncIterator.nextCalls);
  assertSame(1, asyncIterator.returnCalls);

  asyncIterator = new AsyncIteratorPromiseValue();
  iteratorHelper = asyncIterator.map(value => {
    assertInstanceof(value, Promise);
    return value;
  });

  assertIterResult({done: false, value: 41}, await iteratorHelper.next());
  assertIterResult({done: false, value: 42}, await iteratorHelper.next());

  assertIterResult({done: true, value: undefined}, await iteratorHelper.return());
  assertIterResult({done: true, value: undefined}, await iteratorHelper.next());
}

async function testFlatMapPromiseValue() {
  let asyncIterator = new AsyncIteratorPromiseValue();
  let iteratorHelper = asyncIterator.flatMap(async x => [await x, await x * 10]);
  assertSame(0, asyncIterator.nextCalls);

  assertIterResult({done: false, value: 41}, await iteratorHelper.next());
  assertIterResult({done: false, value: 410}, await iteratorHelper.next());
  assertIterResult({done: false, value: 42}, await iteratorHelper.next());
  assertIterResult({done: false, value: 420}, await iteratorHelper.next());
  assertIterResult({done: false, value: 43}, await iteratorHelper.next());

  assertIterResult({done: true, value: undefined}, await iteratorHelper.return());
  assertIterResult({done: true, value: undefined}, await iteratorHelper.next());

  assertSame(3, asyncIterator.nextCalls);
  assertSame(1, asyncIterator.returnCalls);
}

async function testFilterPromiseValue() {
  let asyncIterator = new AsyncIteratorPromiseValue();
  let iteratorHelper = asyncIterator.filter(async x => await x > 41);
  assertSame(0, asyncIterator.nextCalls);

  assertIterResult({done: false, value: 42}, await iteratorHelper.next());
  assertIterResult({done: false, value: 43}, await iteratorHelper.next());

  assertIterResult({done: true, value: undefined}, await iteratorHelper.return());
  assertIterResult({done: true, value: undefined}, await iteratorHelper.next());

  assertSame(3, asyncIterator.nextCalls);
  assertSame(1, asyncIterator.returnCalls);
}

async function testDropAbruptNext(n) {
  let asyncIterator = new AsyncIteratorAbruptNext();
  let iteratorHelper = asyncIterator.drop(n);
  assertSame(0, asyncIterator.nextCalls);

  try {
    await iteratorHelper.next();
    throw new DidNotThrow(ExpectedError);
  } catch (e) {
    assertInstanceof(e, ExpectedError);
  }
  assertSame(1, asyncIterator.nextCalls);
  assertSame(0, asyncIterator.returnCalls);

  assertIterResult({done: true, value: undefined}, await iteratorHelper.next());

  assertSame(1, asyncIterator.nextCalls);
  assertSame(0, asyncIterator.returnCalls);
}

async function testTakeAbruptNext(n) {
  let asyncIterator = new AsyncIteratorAbruptNext();
  let iteratorHelper = asyncIterator.take(n);
  assertSame(0, asyncIterator.nextCalls);

  if (n == 0) {
    assertIterResult({done: true, value: undefined}, await iteratorHelper.next());
    assertSame(0, asyncIterator.nextCalls);
    assertSame(1, asyncIterator.returnCalls);
    return;
  }

  try {
    await iteratorHelper.next();
    throw new DidNotThrow(ExpectedError);
  } catch (e) {
    assertInstanceof(e, ExpectedError);
  }
  assertSame(1, asyncIterator.nextCalls);
  assertSame(0, asyncIterator.returnCalls);

  assertIterResult({done: true, value: undefined}, await iteratorHelper.next());

  assertSame(1, asyncIterator.nextCalls);
  assertSame(0, asyncIterator.returnCalls);
}

async function testDropAbruptValue(n) {
  let asyncIterator = new AsyncIteratorAbruptValue();
  let iteratorHelper = asyncIterator.drop(n);
  assertSame(0, asyncIterator.nextCalls);

  try {
    await iteratorHelper.next();
    throw new DidNotThrow(ExpectedError);
  } catch (e) {
    assertInstanceof(e, ExpectedError);
  }
  assertSame(n + 1, asyncIterator.nextCalls);
  assertSame(0, asyncIterator.returnCalls);

  assertIterResult({done: true, value: undefined}, await iteratorHelper.next());

  assertSame(n + 1, asyncIterator.nextCalls);
  assertSame(0, asyncIterator.returnCalls);
}

async function testTakeAbruptValue(n) {
  let asyncIterator = new AsyncIteratorAbruptValue();
  let iteratorHelper = asyncIterator.take(n);
  assertSame(0, asyncIterator.nextCalls);

  if (n == 0) {
    assertIterResult({done: true, value: undefined}, await iteratorHelper.next());
    assertSame(0, asyncIterator.nextCalls);
    assertSame(1, asyncIterator.returnCalls);
    return;
  }

  try {
    await iteratorHelper.next();
    throw new DidNotThrow(ExpectedError);
  } catch (e) {
    assertInstanceof(e, ExpectedError);
  }
  assertSame(1, asyncIterator.nextCalls);
  assertSame(0, asyncIterator.returnCalls);

  assertIterResult({done: true, value: undefined}, await iteratorHelper.next());

  assertSame(1, asyncIterator.nextCalls);
  assertSame(0, asyncIterator.returnCalls);
}

async function testDropForAwaitThrow(n) {
  let asyncIterator = new AsyncIteratorSequence();
  let iteratorHelper = asyncIterator.drop(n);
  assertSame(0, asyncIterator.nextCalls);

  try {
    for await (const v of iteratorHelper) {
      assertSame(41 + n, v);
      throw new ExpectedError();
    }
    throw new DidNotThrow(ExpectedError);
  } catch (e) {
    assertInstanceof(e, ExpectedError);
  }

  assertSame(n + 1, asyncIterator.nextCalls);
  assertSame(1, asyncIterator.returnCalls);

  assertIterResult({done: true, value: undefined}, await iteratorHelper.next());

  assertSame(n + 1, asyncIterator.nextCalls);
  assertSame(1, asyncIterator.returnCalls);
}

async function testTakeForAwaitThrow(n) {
  let asyncIterator = new AsyncIteratorSequence();
  let iteratorHelper = asyncIterator.take(n);
  assertSame(0, asyncIterator.nextCalls);

  if (n == 0) {
    assertIterResult({done: true, value: undefined}, await iteratorHelper.next());
    assertSame(0, asyncIterator.nextCalls);
    assertSame(1, asyncIterator.returnCalls);
    return;
  }

  try {
    for await (const v of iteratorHelper) {
      assertSame(41, v);
      throw new ExpectedError();
    }
    throw new DidNotThrow(ExpectedError);
  } catch (e) {
    assertInstanceof(e, ExpectedError);
  }

  assertSame(1, asyncIterator.nextCalls);
  assertSame(1, asyncIterator.returnCalls);

  assertIterResult({done: true, value: undefined}, await iteratorHelper.next());

  assertSame(1, asyncIterator.nextCalls);
  assertSame(1, asyncIterator.returnCalls);
}

async function testDropForAwaitBreak(n) {
  let asyncIterator = new AsyncIteratorSequence();
  let iteratorHelper = asyncIterator.drop(n);
  assertSame(0, asyncIterator.nextCalls);

  for await (const v of iteratorHelper) {
    assertSame(41 + n, v);
    break;
  }

  assertSame(n + 1, asyncIterator.nextCalls);
  assertSame(1, asyncIterator.returnCalls);

  assertIterResult({done: true, value: undefined}, await iteratorHelper.next());

  assertSame(n + 1, asyncIterator.nextCalls);
  assertSame(1, asyncIterator.returnCalls);
}

async function testTakeForAwaitBreak(n) {
  let asyncIterator = new AsyncIteratorSequence();
  let iteratorHelper = asyncIterator.take(n);
  assertSame(0, asyncIterator.nextCalls);

  if (n == 0) {
    assertIterResult({done: true, value: undefined}, await iteratorHelper.next());
    assertSame(0, asyncIterator.nextCalls);
    assertSame(1, asyncIterator.returnCalls);
    return;
  }

  for await (const v of iteratorHelper) {
    assertSame(41, v);
    break;
  }

  assertSame(1, asyncIterator.nextCalls);
  assertSame(1, asyncIterator.returnCalls);

  assertIterResult({done: true, value: undefined}, await iteratorHelper.next());

  assertSame(1, asyncIterator.nextCalls);
  assertSame(1, asyncIterator.returnCalls);
}

async function testDropManualReturn(n) {
  let asyncIterator = new AsyncIteratorSequence();
  let iteratorHelper = asyncIterator.drop(n);
  assertSame(0, asyncIterator.nextCalls);

  assertIterResult({done: false, value: 41 + n}, await iteratorHelper.next());
  assertIterResult({done: false, value: 42 + n}, await iteratorHelper.next());

  await iteratorHelper.return();

  assertSame(n + 2, asyncIterator.nextCalls);
  // Iterator helper return() closes the underlying iterator.
  assertSame(1, asyncIterator.returnCalls);

  assertIterResult({done: true, value: undefined}, await iteratorHelper.next());

  assertSame(n + 2, asyncIterator.nextCalls);
  assertSame(1, asyncIterator.returnCalls);
}

async function testTakeManualReturn(n) {
  let asyncIterator = new AsyncIteratorSequence();
  let iteratorHelper = asyncIterator.take(n);
  assertSame(0, asyncIterator.nextCalls);

  if (n == 0) {
    assertIterResult({done: true, value: undefined}, await iteratorHelper.next());
    assertSame(0, asyncIterator.nextCalls);
    assertSame(1, asyncIterator.returnCalls);
    return;
  }

  for (let i = 0; i < n; i++) {
    assertIterResult({done: false, value: 41 + i}, await iteratorHelper.next());
  }

  await iteratorHelper.return();

  assertSame(n, asyncIterator.nextCalls);
  // Iterator helper return() closes the underlying iterator.
  assertSame(1, asyncIterator.returnCalls);

  assertIterResult({done: true, value: undefined}, await iteratorHelper.next());

  assertSame(n, asyncIterator.nextCalls);
  assertSame(1, asyncIterator.returnCalls);
}

async function testDropForAwaitThrowReturnError(n, AsyncIteratorClass) {
  let asyncIterator = new AsyncIteratorClass();
  let iteratorHelper = asyncIterator.drop(n);
  assertSame(0, asyncIterator.nextCalls);

  try {
    for await (const v of iteratorHelper) {
      assertSame(41 + n, v);
      throw new ExpectedError();
    }
    throw new DidNotThrow(ExpectedError);
  } catch (e) {
    assertInstanceof(e, ExpectedError);
  }

  assertSame(n + 1, asyncIterator.nextCalls);
  assertSame(1, asyncIterator.returnCalls);

  assertIterResult({done: true, value: undefined}, await iteratorHelper.next());

  assertSame(n + 1, asyncIterator.nextCalls);
  assertSame(1, asyncIterator.returnCalls);
}

async function testTakeForAwaitThrowReturnError(n, AsyncIteratorClass) {
  let asyncIterator = new AsyncIteratorClass();
  let iteratorHelper = asyncIterator.take(n);
  assertSame(0, asyncIterator.nextCalls);

  let expectedError = n == 0 ? ExpectedReturnError : ExpectedError;
  try {
    for await (const v of iteratorHelper) {
      assertSame(41, v);
      throw new ExpectedError();
    }
    throw new DidNotThrow(expectedError);
  } catch (e) {
    assertInstanceof(e, expectedError);
  }

  assertSame(n == 0 ? 0 : 1, asyncIterator.nextCalls);
  assertSame(1, asyncIterator.returnCalls);

  assertIterResult({done: true, value: undefined}, await iteratorHelper.next());

  assertSame(n == 0 ? 0 : 1, asyncIterator.nextCalls);
  assertSame(1, asyncIterator.returnCalls);
}

async function testDropForAwaitBreakReturnError(n, AsyncIteratorClass) {
  let asyncIterator = new AsyncIteratorClass();
  let iteratorHelper = asyncIterator.drop(n);
  assertSame(0, asyncIterator.nextCalls);

  try {
    for await (const v of iteratorHelper) {
      assertSame(41 + n, v);
      break;
    }
    throw new DidNotThrow(ExpectedReturnError);
  } catch (e) {
    assertInstanceof(e, ExpectedReturnError);
  }

  assertSame(n + 1, asyncIterator.nextCalls);
  assertSame(1, asyncIterator.returnCalls);

  assertIterResult({done: true, value: undefined}, await iteratorHelper.next());

  assertSame(n + 1, asyncIterator.nextCalls);
  assertSame(1, asyncIterator.returnCalls);
}

async function testTakeForAwaitBreakReturnError(n, AsyncIteratorClass) {
  let asyncIterator = new AsyncIteratorClass();
  let iteratorHelper = asyncIterator.take(n);
  assertSame(0, asyncIterator.nextCalls);

  try {
    for await (const v of iteratorHelper) {
      assertSame(41, v);
      break;
    }
    throw new DidNotThrow(ExpectedReturnError);
  } catch (e) {
    assertInstanceof(e, ExpectedReturnError);
  }

  assertSame(n == 0 ? 0 : 1, asyncIterator.nextCalls);
  assertSame(1, asyncIterator.returnCalls);

  assertIterResult({done: true, value: undefined}, await iteratorHelper.next());

  assertSame(n == 0 ? 0 : 1, asyncIterator.nextCalls);
  assertSame(1, asyncIterator.returnCalls);
}

async function run(fn, ...args) {
  for (let nextArgs of expandArgs(args)) {
    debugLog("**", fn.name, nextArgs.map(a => typeof a == 'function' ? a.name : a));

    await fn(...nextArgs);

    debugLog("OK", fn.name, nextArgs.map(a => typeof a == 'function' ? a.name : a));
  }
}

(async function main() {
  try {
    await run(testDropPromiseValue, [0, 1, 2]);
    await run(testDropAbruptNext, [0, 1, 2]);
    await run(testDropAbruptValue, [0, 1, 2]);
    await run(testDropForAwaitThrow, [0, 1, 2]);
    await run(testDropForAwaitBreak, [0, 1, 2]);
    await run(testDropManualReturn, [0, 1, 2]);
    await run(testDropForAwaitThrowReturnError, [0, 1, 2], [AsyncIteratorSequenceReturnError, AsyncIteratorSequenceReturnReject]);
    await run(testDropForAwaitBreakReturnError, [0, 1, 2], [AsyncIteratorSequenceReturnError, AsyncIteratorSequenceReturnReject]);

    await run(testTakePromiseValue, [0, 1, 2]);
    await run(testTakeAbruptNext, [0, 1, 2]);
    await run(testTakeAbruptValue, [0, 1, 2]);
    await run(testTakeForAwaitThrow, [0, 1, 2]);
    await run(testTakeForAwaitBreak, [0, 1, 2]);
    await run(testTakeManualReturn, [0, 1, 2]);
    await run(testTakeForAwaitThrowReturnError, [0, 1, 2], [AsyncIteratorSequenceReturnError, AsyncIteratorSequenceReturnReject]);
    await run(testTakeForAwaitBreakReturnError, [0, 1, 2], [AsyncIteratorSequenceReturnError, AsyncIteratorSequenceReturnReject]);

    await run(testMapPromiseValue);
    await run(testFlatMapPromiseValue);
    await run(testFilterPromiseValue);

    debugLog("DONE");
  } catch (e) {
    console.error(e.stack ?? e);
    throw e;
  }
})();
