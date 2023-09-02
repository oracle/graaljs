/*
 * Copyright (c) 2022, 2022, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Tests that iterator helper methods validate arguments in the correct order.
 *
 * @option iterator-helpers=true
 */

load("../assert.js");

class BadIterator extends Iterator {
  get next() {
    throw new Error('should not reach here');
  }
}
class BadAsyncIterator extends AsyncIterator {
  get next() {
    throw new Error('should not reach here');
  }
}

for (let Iter of [BadIterator, BadAsyncIterator]) {
  for (let method of ['filter', 'map', 'reduce', 'every', 'find', 'some', 'flatMap', 'forEach']) {
    try {
      new Iter()[method]("not a function");
    } catch (e) {
      if (!(e instanceof TypeError)) {
        console.error("ERR", Iter.name, method, e);
        throw e;
      }
    }
  }
}
