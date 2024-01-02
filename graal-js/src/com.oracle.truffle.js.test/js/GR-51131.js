/*
 * Copyright (c) 2024, 2024, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

load("assert.js");

var proto = {
  isProto: true,
  get a() {
    return this.isProto;
  },
  get b() {
    return this.isProto;
  },
  get c() {
    return this.isProto;
  }
};

var object = {
  isProto: false,
  test() {
    for (let key of 'abc') {
      assertFalse(super[key]);
    }
  }
};

Object.setPrototypeOf(object, proto);

for (let i = 0; i < 100; i++) {
    object.test();
}
