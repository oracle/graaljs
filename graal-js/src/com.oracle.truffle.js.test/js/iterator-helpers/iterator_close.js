/*
 * Copyright (c) 2022, 2022, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Tests iterator helpers.
 *
 * @option iterator-helpers=true
 */

load("iteratorhelper_common.js");

function testDropAbruptNext(n) {
  let iterator = new IteratorAbruptNext();
  let iteratorHelper = iterator.drop(n);
  assertSame(0, iterator.nextCalls);

  try {
    iteratorHelper.next();
    throw new DidNotThrow(ExpectedError);
  } catch (e) {
    assertInstanceof(e, ExpectedError);
  }
  assertSame(1, iterator.nextCalls);
  assertSame(0, iterator.returnCalls);

  assertIterResult({done: true, value: undefined}, iteratorHelper.next());

  assertSame(1, iterator.nextCalls);
  assertSame(0, iterator.returnCalls);
}

function testTakeAbruptNext(n) {
  let iterator = new IteratorAbruptNext();
  let iteratorHelper = iterator.take(n);
  assertSame(0, iterator.nextCalls);

  if (n == 0) {
    assertIterResult({done: true, value: undefined}, iteratorHelper.next());
    assertSame(0, iterator.nextCalls);
    assertSame(1, iterator.returnCalls);
    return;
  }

  try {
    iteratorHelper.next();
    throw new DidNotThrow(ExpectedError);
  } catch (e) {
    assertInstanceof(e, ExpectedError);
  }
  assertSame(1, iterator.nextCalls);
  assertSame(0, iterator.returnCalls);

  assertIterResult({done: true, value: undefined}, iteratorHelper.next());

  assertSame(1, iterator.nextCalls);
  assertSame(0, iterator.returnCalls);
}

function testDropAbruptValue(n) {
  let iterator = new IteratorAbruptValue();
  let iteratorHelper = iterator.drop(n);
  assertSame(0, iterator.nextCalls);

  try {
    iteratorHelper.next();
    throw new DidNotThrow(ExpectedError);
  } catch (e) {
    assertInstanceof(e, ExpectedError);
  }
  assertSame(n + 1, iterator.nextCalls);
  assertSame(0, iterator.returnCalls);

  assertIterResult({done: true, value: undefined}, iteratorHelper.next());

  assertSame(n + 1, iterator.nextCalls);
  assertSame(0, iterator.returnCalls);
}

function testTakeAbruptValue(n) {
  let iterator = new IteratorAbruptValue();
  let iteratorHelper = iterator.take(n);
  assertSame(0, iterator.nextCalls);

  if (n == 0) {
    assertIterResult({done: true, value: undefined}, iteratorHelper.next());
    assertSame(0, iterator.nextCalls);
    assertSame(1, iterator.returnCalls);
    return;
  }

  try {
    iteratorHelper.next();
    throw new DidNotThrow(ExpectedError);
  } catch (e) {
    assertInstanceof(e, ExpectedError);
  }
  assertSame(1, iterator.nextCalls);
  assertSame(0, iterator.returnCalls);

  assertIterResult({done: true, value: undefined}, iteratorHelper.next());

  assertSame(1, iterator.nextCalls);
  assertSame(0, iterator.returnCalls);
}

function testDropForThrow(n) {
  let iterator = new IteratorSequence();
  let iteratorHelper = iterator.drop(n);
  assertSame(0, iterator.nextCalls);

  try {
    for (const v of iteratorHelper) {
      assertSame(41 + n, v);
      throw new ExpectedError();
    }
    throw new DidNotThrow(ExpectedError);
  } catch (e) {
    assertInstanceof(e, ExpectedError);
  }

  assertSame(n + 1, iterator.nextCalls);
  assertSame(1, iterator.returnCalls);

  assertIterResult({done: true, value: undefined}, iteratorHelper.next());

  assertSame(n + 1, iterator.nextCalls);
  assertSame(1, iterator.returnCalls);
}

function testTakeForThrow(n) {
  let iterator = new IteratorSequence();
  let iteratorHelper = iterator.take(n);
  assertSame(0, iterator.nextCalls);

  if (n == 0) {
    assertIterResult({done: true, value: undefined}, iteratorHelper.next());
    assertSame(0, iterator.nextCalls);
    assertSame(1, iterator.returnCalls);
    return;
  }

  try {
    for (const v of iteratorHelper) {
      assertSame(41, v);
      throw new ExpectedError();
    }
    throw new DidNotThrow(ExpectedError);
  } catch (e) {
    assertInstanceof(e, ExpectedError);
  }

  assertSame(1, iterator.nextCalls);
  assertSame(1, iterator.returnCalls);

  assertIterResult({done: true, value: undefined}, iteratorHelper.next());

  assertSame(1, iterator.nextCalls);
  assertSame(1, iterator.returnCalls);
}

function testDropForBreak(n) {
  let iterator = new IteratorSequence();
  let iteratorHelper = iterator.drop(n);
  assertSame(0, iterator.nextCalls);

  for (const v of iteratorHelper) {
    assertSame(41 + n, v);
    break;
  }

  assertSame(n + 1, iterator.nextCalls);
  assertSame(1, iterator.returnCalls);

  assertIterResult({done: true, value: undefined}, iteratorHelper.next());

  assertSame(n + 1, iterator.nextCalls);
  assertSame(1, iterator.returnCalls);
}

function testTakeForBreak(n) {
  let iterator = new IteratorSequence();
  let iteratorHelper = iterator.take(n);
  assertSame(0, iterator.nextCalls);

  if (n == 0) {
    assertIterResult({done: true, value: undefined}, iteratorHelper.next());
    assertSame(0, iterator.nextCalls);
    assertSame(1, iterator.returnCalls);
    return;
  }

  for (const v of iteratorHelper) {
    assertSame(41, v);
    break;
  }

  assertSame(1, iterator.nextCalls);
  assertSame(1, iterator.returnCalls);

  assertIterResult({done: true, value: undefined}, iteratorHelper.next());

  assertSame(1, iterator.nextCalls);
  assertSame(1, iterator.returnCalls);
}

function testDropManualReturn(n) {
  let iterator = new IteratorSequence();
  let iteratorHelper = iterator.drop(n);
  assertSame(0, iterator.nextCalls);

  assertIterResult({done: false, value: 41 + n}, iteratorHelper.next());
  assertIterResult({done: false, value: 42 + n}, iteratorHelper.next());

  iteratorHelper.return();

  assertSame(n + 2, iterator.nextCalls);
  // Iterator helper return() closes the underlying iterator.
  assertSame(1, iterator.returnCalls);

  assertIterResult({done: true, value: undefined}, iteratorHelper.next());

  assertSame(n + 2, iterator.nextCalls);
  assertSame(1, iterator.returnCalls);
}

function testTakeManualReturn(n) {
  let iterator = new IteratorSequence();
  let iteratorHelper = iterator.take(n);
  assertSame(0, iterator.nextCalls);

  if (n == 0) {
    assertIterResult({done: true, value: undefined}, iteratorHelper.next());
    assertSame(0, iterator.nextCalls);
    assertSame(1, iterator.returnCalls);
    return;
  }

  for (let i = 0; i < n; i++) {
    assertIterResult({done: false, value: 41 + i}, iteratorHelper.next());
  }

  iteratorHelper.return();

  assertSame(n, iterator.nextCalls);
  // Iterator helper return() closes the underlying iterator.
  assertSame(1, iterator.returnCalls);

  assertIterResult({done: true, value: undefined}, iteratorHelper.next());

  assertSame(n, iterator.nextCalls);
  assertSame(1, iterator.returnCalls);
}

function testDropForThrowReturnError(n) {
  let iterator = new IteratorSequenceReturnError();
  let iteratorHelper = iterator.drop(n);
  assertSame(0, iterator.nextCalls);

  try {
    for (const v of iteratorHelper) {
      assertSame(41 + n, v);
      throw new ExpectedError();
    }
    throw new DidNotThrow(ExpectedError);
  } catch (e) {
    assertInstanceof(e, ExpectedError);
  }

  assertSame(n + 1, iterator.nextCalls);
  assertSame(1, iterator.returnCalls);

  assertIterResult({done: true, value: undefined}, iteratorHelper.next());

  assertSame(n + 1, iterator.nextCalls);
  assertSame(1, iterator.returnCalls);
}

function testTakeForThrowReturnError(n) {
  let iterator = new IteratorSequenceReturnError();
  let iteratorHelper = iterator.take(n);
  assertSame(0, iterator.nextCalls);

  let expectedError = n == 0 ? ExpectedReturnError : ExpectedError;
  try {
    for (const v of iteratorHelper) {
      assertSame(41, v);
      throw new ExpectedError();
    }
    throw new DidNotThrow(expectedError);
  } catch (e) {
    assertInstanceof(e, expectedError);
  }

  assertSame(n == 0 ? 0 : 1, iterator.nextCalls);
  assertSame(1, iterator.returnCalls);

  assertIterResult({done: true, value: undefined}, iteratorHelper.next());

  assertSame(n == 0 ? 0 : 1, iterator.nextCalls);
  assertSame(1, iterator.returnCalls);
}

function testDropForBreakReturnError(n) {
  let iterator = new IteratorSequenceReturnError();
  let iteratorHelper = iterator.drop(n);
  assertSame(0, iterator.nextCalls);

  try {
    for (const v of iteratorHelper) {
      assertSame(41 + n, v);
      break;
    }
    throw new DidNotThrow(ExpectedReturnError);
  } catch (e) {
    assertInstanceof(e, ExpectedReturnError);
  }

  assertSame(n + 1, iterator.nextCalls);
  assertSame(1, iterator.returnCalls);

  assertIterResult({done: true, value: undefined}, iteratorHelper.next());

  assertSame(n + 1, iterator.nextCalls);
  assertSame(1, iterator.returnCalls);
}

function testTakeForBreakReturnError(n) {
  let iterator = new IteratorSequenceReturnError();
  let iteratorHelper = iterator.take(n);
  assertSame(0, iterator.nextCalls);

  try {
    for (const v of iteratorHelper) {
      assertSame(41, v);
      break;
    }
    throw new DidNotThrow(ExpectedReturnError);
  } catch (e) {
    assertInstanceof(e, ExpectedReturnError);
  }

  assertSame(n == 0 ? 0 : 1, iterator.nextCalls);
  assertSame(1, iterator.returnCalls);

  assertIterResult({done: true, value: undefined}, iteratorHelper.next());

  assertSame(n == 0 ? 0 : 1, iterator.nextCalls);
  assertSame(1, iterator.returnCalls);
}

function run(fn, ...args) {
  for (let nextArgs of expandArgs(args)) {
    debugLog("**", fn.name, nextArgs.map(a => typeof a == 'function' ? a.name : a));

    fn(...nextArgs);

    debugLog("OK", fn.name, nextArgs.map(a => typeof a == 'function' ? a.name : a));
  }
}

(function main() {
  try {
    run(testDropAbruptNext, [0, 1, 2]);
    run(testDropAbruptValue, [0, 1, 2]);
    run(testDropForThrow, [0, 1, 2]);
    run(testDropForBreak, [0, 1, 2]);
    run(testDropManualReturn, [0, 1, 2]);
    run(testDropForThrowReturnError, [0, 1, 2]);
    run(testDropForBreakReturnError, [0, 1, 2]);

    run(testTakeAbruptNext, [0, 1, 2]);
    run(testTakeAbruptValue, [0, 1, 2]);
    run(testTakeForThrow, [0, 1, 2]);
    run(testTakeForBreak, [0, 1, 2]);
    run(testTakeManualReturn, [0, 1, 2]);
    run(testTakeForThrowReturnError, [0, 1, 2]);
    run(testTakeForBreakReturnError, [0, 1, 2]);

    debugLog("DONE");
  } catch (e) {
    console.error(e.stack ?? e);
    throw e;
  }
})();
