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

async function testInvalidReceiver() {
  await helper(it => it.drop(0));
  await helper(it => it.take(1));
  await helper(it => it.indexed());
  await helper(it => it.map(x => x));
  await helper(it => it.flatMap(x => x));
  await helper(it => it.filter(x => true));

  async function helper(helperFactory) {
    let asyncIterator = new AsyncIteratorSequence();
    let iteratorHelper = helperFactory(asyncIterator);
    assertSame(0, asyncIterator.nextCalls);

    async function* dummyGenerator() { yield 42; };

    for (let method of ['next', 'return']) {
      for (let invalidGenerator of [42, {}, dummyGenerator()]) {
        // Should not throw directly but return a rejected promise.
        let promise = iteratorHelper[method].call(invalidGenerator);
        try {
          await promise;
          throw new DidNotThrow(TypeError);
        } catch (e) {
          assertInstanceof(e, TypeError);
        }
      }
    }

    assertSame(0, asyncIterator.nextCalls);
    assertSame(0, asyncIterator.returnCalls);
  }
}

async function testGeneratorBrandCheck() {
  await helper(it => it.drop(0));
  await helper(it => it.take(1));
  await helper(it => it.indexed());
  await helper(it => it.map(x => x));
  await helper(it => it.flatMap(x => x));
  await helper(it => it.filter(x => true));

  async function helper(helperFactory) {
    let asyncIterator = new AsyncIteratorSequence();
    let iteratorHelper = helperFactory(asyncIterator);

    async function* dummyGenerator() { yield 42; };
    let generator = dummyGenerator();

    for (let method of ['next', 'return', 'throw']) {
      // Should not throw directly but return a rejected promise.
      let promise = generator[method].call(iteratorHelper);
      try {
        await promise;
        throw new DidNotThrow(TypeError);
      } catch (e) {
        assertInstanceof(e, TypeError);
      }
    }

    assertSame(0, asyncIterator.nextCalls);
    assertSame(0, asyncIterator.returnCalls);
  }
}

(async function main() {
  try {
    await testInvalidReceiver();
    await testGeneratorBrandCheck();

    debugLog("DONE");
  } catch (e) {
    console.error(e.stack ?? e);
    throw e;
  }
})();
