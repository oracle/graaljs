/*
 * Copyright (c) 2024, 2024, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Regression test checking successful transition from sealed JSObjectArray to ObjectArray strategy.
 *
 * @option debug-builtin
 */

load("assert.js");

const o = {};
const x = 42;

for (const jsobjectArrayType of ["ZeroBasedJSObjectArray", "ContiguousJSObjectArray", "HolesJSObjectArray"]) {
  const objectArrayType = jsobjectArrayType.replace("JS", "");
  switch (jsobjectArrayType) {
    case "ZeroBasedJSObjectArray": {
      let a = [];
      for (let i = 0; i < 8; i++) {
        a[i] = o;
      }
      Object.seal(a);
      assertArrayType(a, jsobjectArrayType);

      a[5] = x;

      assertArrayType(a, objectArrayType);
      assertSameContent([o,o,o,o,o,x,o,o], a);
      assertTrue(Object.isSealed(a));
      break;
    }
    case "ContiguousJSObjectArray": {
      let a = [];
      for (let i = 1; i < 8; i++) {
        a[i] = o;
      }
      Object.seal(a);
      assertArrayType(a, jsobjectArrayType);

      a[0] = x;
      assertArrayType(a, jsobjectArrayType);

      a[5] = x;
      assertArrayType(a, objectArrayType);
      assertSameContent([ ,o,o,o,o,x,o,o], a);
      assertTrue(Object.isSealed(a));
      break;
    }
    case "HolesJSObjectArray": {
      let a = [];
      for (let i = 0; i < 8; i++) {
        if (i == 4) {
          continue;
        }
        a[i] = o;
      }
      Object.seal(a);
      assertArrayType(a, jsobjectArrayType);

      a[4] = x;
      assertArrayType(a, jsobjectArrayType);
      a[5] = x;

      assertArrayType(a, objectArrayType);
      assertSameContent([o,o,o,o, ,x,o,o], a);
      assertTrue(Object.isSealed(a));
      break;
    }
    default: throw new TypeError(jsobjectArrayType);
  }
}

function assertArrayType(array, expected) {
  assertSame(expected, Debug.arraytype(array))
}
