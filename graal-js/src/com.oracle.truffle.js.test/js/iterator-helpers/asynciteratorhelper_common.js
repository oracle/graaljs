/*
 * Copyright (c) 2023, 2023, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Common test functions/classes for async iterator helper tests.
 *
 * @option async-iterator-helpers=true
 */

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
