/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Tests Iterator.concat() a.k.a. iterator-sequencing proposal.
 *
 * @option ecmascript-version=staging
 */

load("./iteratorhelper_common.js");

{
  let iter = Iterator.concat(["a", "b"], ["c", "d"], ["e"]);

  assertIterResult({done: false, value: "a"}, iter.next());
  assertIterResult({done: false, value: "b"}, iter.next());
  assertIterResult({done: false, value: "c"}, iter.next());
  assertIterResult({done: false, value: "d"}, iter.next());
  assertIterResult({done: false, value: "e"}, iter.next());
  assertIterResult({done: true, value: undefined}, iter.next());
  assertIterResult({done: true, value: undefined}, iter.next());

  // generator state: completed
  assertIterResult({done: true, value: undefined}, iter.return());

  assertIterResult({done: true, value: undefined}, iter.next());
}

{
  let iter = Iterator.concat(["a", "b"], ["c", "d"], ["e"]);

  assertIterResult({done: false, value: "a"}, iter.next());
  assertIterResult({done: false, value: "b"}, iter.next());
  assertIterResult({done: false, value: "c"}, iter.next());

  // generator state: suspended-yield
  assertIterResult({done: true, value: undefined}, iter.return());

  assertIterResult({done: true, value: undefined}, iter.next());
  assertIterResult({done: true, value: undefined}, iter.next());
}

{
  let iter = Iterator.concat(["a", "b"], ["c", "d"], ["e"]);

  // generator state: suspended-start
  assertIterResult({done: true, value: undefined}, iter.return());

  assertIterResult({done: true, value: undefined}, iter.next());
  assertIterResult({done: true, value: undefined}, iter.next());
}

{
  let iter = Iterator.concat(["a", "b"], ["c", "d"], Iterator.from(["e"]));

  assertSameContent(["a", "b", "c", "d", "e"], Array.from(iter));
}

{
  let iter = Iterator.concat(["a", "b"], Iterator.concat(["c", "d"], ["e"]));

  assertSameContent(["a", "b", "c", "d", "e"], [...iter]);
}

{
  let iter = Iterator.concat(["a", "b"], ["c", "d"], ["e"]);

  let items = [];
  for (let item of iter) {
    items.push(item);
    if (item === "c") {
      break;
    }
  }
  assertSameContent(["a", "b", "c"], items);

  assertIterResult({done: true, value: undefined}, iter.next());
}
