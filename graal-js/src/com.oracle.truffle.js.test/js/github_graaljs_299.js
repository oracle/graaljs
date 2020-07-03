/*
 * Copyright (c) 2020, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */
// test for the checks in ES2020 9.1.6.3, 4.a and 4.b
// as brought up in https://github.com/graalvm/graaljs/issues/299

load('assert.js');

//ES2020 9.1.6.3, 4.a
function test4a() {
  var arr = [0, 1, 2, 3];
  try {
    Object.defineProperty(arr, "length", { value:1, configurable:true });
    fail("should not reach");
  } catch (ex) {
    assertTrue(ex instanceof TypeError);
  }
  assertSame(4, arr.length);
};
test4a();


//ES2020 9.1.6.3, 4.b
function test4b() {
  var arr = [0, 1, 2, 3];
  try {
    Object.defineProperty(arr, 'length', { value: 2, enumerable: true })
    fail("should not reach");
  } catch (ex) {
    assertTrue(ex instanceof TypeError);
  }
  assertSame(4, arr.length);
};
test4b();

true;
