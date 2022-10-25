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

function testInvalidReceiver() {
  helper(it => it.drop(0));
  helper(it => it.take(1));
  helper(it => it.map(x => x));
  helper(it => it.flatMap(x => x));
  helper(it => it.filter(x => true));

  function helper(helperFactory) {
    let iterator = new IteratorSequence();
    let iteratorHelper = helperFactory(iterator);
    assertSame(0, iterator.nextCalls);

    function* dummyGenerator() { yield 42; };

    for (let method of ['next', 'return']) {
      for (let invalidGenerator of [42, {}, dummyGenerator()]) {
        try {
          iteratorHelper[method].call(invalidGenerator);
          throw new DidNotThrow(TypeError);
        } catch (e) {
          assertInstanceof(e, TypeError);
        }
      }
    }

    assertSame(0, iterator.nextCalls);
    assertSame(0, iterator.returnCalls);
  }
}

function testGeneratorBrandCheck() {
  helper(it => it.drop(0));
  helper(it => it.take(1));
  helper(it => it.map(x => x));
  helper(it => it.flatMap(x => x));
  helper(it => it.filter(x => true));

  function helper(helperFactory) {
    let iterator = new IteratorSequence();
    let iteratorHelper = helperFactory(iterator);

    function* dummyGenerator() { yield 42; };
    let generator = dummyGenerator();

    for (let method of ['next', 'return', 'throw']) {
      try {
        generator[method].call(iteratorHelper);
        throw new DidNotThrow(TypeError);
      } catch (e) {
        assertInstanceof(e, TypeError);
      }
    }

    assertSame(0, iterator.nextCalls);
    assertSame(0, iterator.returnCalls);
  }
}

testInvalidReceiver();
testGeneratorBrandCheck();
