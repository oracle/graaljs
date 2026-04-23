/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Tests Iterator.zip() and Iterator.zipKeyed() a.k.a. joint iteration proposal.
 *
 * @option ecmascript-version=staging
 */

load("./iteratorhelper_common.js");

{
  let iter = Iterator.zip([[1, 2], [3]], {mode: "shortest"});

  let result = iter.next();
  assertSame(false, result.done);
  assertSameContent([1, 3], result.value);

  assertIterResult({done: true, value: undefined}, iter.next());
  assertIterResult({done: true, value: undefined}, iter.next());
}

{
  let iter = Iterator.zip([[1, 2], [3]], {mode: "longest"});

  let result = iter.next();
  assertSame(false, result.done);
  assertSameContent([1, 3], result.value);

  result = iter.next();
  assertSame(false, result.done);
  assertSameContent([2, undefined], result.value);

  assertIterResult({done: true, value: undefined}, iter.next());
  assertIterResult({done: true, value: undefined}, iter.next());
}

assertThrows(() => Iterator.zip([[1], []], {mode: "strict"}).next(), TypeError);

{
  let first = new CloseableIteratorSequence();
  let second = new CloseableIteratorSequence();
  let iter = Iterator.zip([first, second], {mode: "strict"});

  let result = iter.next();
  assertSame(false, result.done);
  assertSameContent([41, 41], result.value);

  assertIterResult({done: true, value: undefined}, iter.return());
  assertSame(1, first.nextCalls);
  assertSame(1, first.returnCalls);
  assertSame(1, second.nextCalls);
  assertSame(1, second.returnCalls);
  assertIterResult({done: true, value: undefined}, iter.next());
}

{
  let iter = Iterator.zipKeyed({a: [1, 2], b: [3]}, {mode: "shortest"});

  let result = iter.next();
  assertSame(false, result.done);
  assertSame(null, Object.getPrototypeOf(result.value));
  assertSameContent(["a", "b"], Object.keys(result.value));
  assertSame(1, result.value.a);
  assertSame(3, result.value.b);

  assertIterResult({done: true, value: undefined}, iter.next());
  assertIterResult({done: true, value: undefined}, iter.next());
}

{
  let iter = Iterator.zipKeyed({a: [1, 2], b: [3]}, {mode: "longest"});

  let result = iter.next();
  assertSame(false, result.done);
  assertSame(null, Object.getPrototypeOf(result.value));
  assertSameContent(["a", "b"], Object.keys(result.value));
  assertSame(1, result.value.a);
  assertSame(3, result.value.b);

  result = iter.next();
  assertSame(false, result.done);
  assertSame(null, Object.getPrototypeOf(result.value));
  assertSameContent(["a", "b"], Object.keys(result.value));
  assertSame(2, result.value.a);
  assertSame(undefined, result.value.b);

  assertIterResult({done: true, value: undefined}, iter.next());
  assertIterResult({done: true, value: undefined}, iter.next());
}

assertThrows(() => Iterator.zipKeyed({a: [1], b: []}, {mode: "strict"}).next(), TypeError);

{
  let a = new CloseableIteratorSequence();
  let b = new CloseableIteratorSequence();
  let iter = Iterator.zipKeyed({a, b}, {mode: "strict"});

  let result = iter.next();
  assertSame(false, result.done);
  assertSame(41, result.value.a);
  assertSame(41, result.value.b);

  assertIterResult({done: true, value: undefined}, iter.return());
  assertSame(1, a.nextCalls);
  assertSame(1, a.returnCalls);
  assertSame(1, b.nextCalls);
  assertSame(1, b.returnCalls);
  assertIterResult({done: true, value: undefined}, iter.next());
}
