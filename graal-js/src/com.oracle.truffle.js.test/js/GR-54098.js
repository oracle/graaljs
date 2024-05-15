/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * RegExp.prototype.exec and test should not perform ToPrimitive if `this` is not a valid RegExp.
 */

load("assert.js");

const rx = /(8+)/yudgs;
const no_tostring = {
  __proto__: rx,
  [Symbol.toPrimitive](hint) {
      throw new Error("@@toPrimitive should not be called");
  }
};
assertThrows(() => no_tostring.exec(no_tostring), TypeError);
assertThrows(() => no_tostring.test(), TypeError);

const no_exec = {
  __proto__: rx,
  exec: 1,
  [Symbol.toPrimitive](hint) {
    throw new Error("@@toPrimitive should not be called");
  }
};
assertThrows(() => no_exec.test(), TypeError);

const fake_regexp = {
  __proto__: rx,
  get_exec_count: 0,
  get exec() {
    this.get_exec_count++;
    return RegExp.prototype.exec;
  },
  [Symbol.toPrimitive](hint) {
    throw new Error("@@toPrimitive should not be called");
  }
};
assertThrows(() => fake_regexp.exec(no_tostring), TypeError);
assertSame(1, fake_regexp.get_exec_count);
