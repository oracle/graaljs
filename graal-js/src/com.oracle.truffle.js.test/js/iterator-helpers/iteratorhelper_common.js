/*
 * Copyright (c) 2022, 2022, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Common test functions/classes for iterator helper tests.
 *
 * @option iterator-helpers=true
 */

load("../assert.js");

function assertInstanceof(e, Type) {
  if (!(e instanceof Type)) {
    throw `Error: ${e} is not an instance of ${Type.name || Type}`;
  }
}

function assertIterResult(expected, actual) {
  assertTrue('done' in actual && 'value' in actual);
  if ('done' in expected) {
    assertSame(expected.done, actual.done);
  }
  if ('value' in expected) {
    assertSame(expected.value, actual.value);
  }
}

function expandArgs(args) {
  let tail = [[]];
  if (args.length == 0) {
    return [[]];
  } else if (args.length > 1) {
    tail = expandArgs(args.slice(1));
  }
  let head = Array.isArray(args[0]) ? args[0] : [args[0]];
  let all = [];
  for (let a of head) {
    for (let b of tail) {
      all.push([a].concat(b));
    }
  }
  return all;
}

function debugLog(...args) {
}

class ExpectedError extends Error {
  name = "ExpectedError";
}
class ExpectedReturnError extends Error {
  name = "ExpectedReturnError";
}
class DidNotThrow extends Error {
  constructor(ErrorClass = Error) {
    super(`Did not throw ${ErrorClass.name ?? ErrorClass}`);
  }
}

class IteratorCommon extends Iterator {
  nextCalls = 0;
  returnCalls = 0;

  nextCommon() {
    debugLog("NEXT", this.constructor.name);
    this.nextCalls++;
  }

  returnCommon() {
    debugLog("RETURN", this.constructor.name);
    this.returnCalls++;
  }

  return() {
    this.returnCommon();
    return {done: true, value: undefined};
  }
}

class IteratorAbruptNext extends IteratorCommon {
  next() {
    this.nextCommon();
    throw new ExpectedError();
  }
}

class IteratorAbruptValue extends IteratorCommon {
  next() {
    this.nextCommon();
    return {
      done: false,
      get value() {
          throw new ExpectedError();
      }
    };
  }
}

class IteratorSequence extends IteratorCommon {
  next() {
    this.nextCommon();
    return {
      done: false,
      value: 40 + this.nextCalls
    };
  }
}

class CloseableIteratorSequence extends IteratorSequence {
  next() {
    if (this.returnCalls > 0) {
      return {done: true, value: undefined};
    }
    return super.next();
  }
}

class IteratorSequenceReturnError extends IteratorSequence {
  return() {
    this.returnCommon();
    throw new ExpectedReturnError();
  }
}

class AsyncIteratorCommon extends AsyncIterator {
  nextCalls = 0;
  returnCalls = 0;

  nextCommon() {
    debugLog("NEXT", this.constructor.name);
    this.nextCalls++;
  }

  returnCommon() {
    debugLog("RETURN", this.constructor.name);
    this.returnCalls++;
  }

  async return() {
    this.returnCommon();
    return {done: true, value: undefined};
  }
}

class AsyncIteratorAbruptNext extends AsyncIteratorCommon {
  async next() {
    this.nextCommon();
    return Promise.reject(new ExpectedError());
  }
}

class AsyncIteratorAbruptValue extends AsyncIteratorCommon {
  async next() {
    this.nextCommon();
    return {
      done: false,
      get value() {
        throw new ExpectedError();
      }
    };
  }
}

class AsyncIteratorSequence extends AsyncIteratorCommon {
  async next() {
    this.nextCommon();
    return {
      done: false,
      value: 40 + this.nextCalls
    };
  }
}

class CloseableAsyncIteratorSequence extends AsyncIteratorSequence {
  async next() {
    if (this.returnCalls > 0) {
      return {done: true, value: undefined};
    }
    return await super.next();
  }
}

class AsyncIteratorSequenceReturnError extends AsyncIteratorSequence {
  async return() {
    this.returnCommon();
    throw new ExpectedReturnError();
  }
}

class AsyncIteratorSequenceReturnReject extends AsyncIteratorSequence {
  async return() {
    this.returnCommon();
    return Promise.reject(new ExpectedReturnError());
  }
}

class AsyncIteratorPromiseValue extends AsyncIteratorCommon {
  async next() {
    this.nextCommon();
    return {
      done: false,
      value: Promise.resolve(Promise.resolve(40 + this.nextCalls))
    };
  }
  async return() {
    return await super.return();
  }
}
