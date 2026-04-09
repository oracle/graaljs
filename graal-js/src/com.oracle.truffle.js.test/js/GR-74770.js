/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/*
 * Regression test of String constructor evaluation order.
 */

load("assert.js");

var log = [];

const argument = {
  toString() {
    log.push("toString");
    return "foo";
  }
};

const newTarget = new Proxy(function() {}, {
  get(target, property, receiver) {
    if (property === "prototype") {
      log.push("prototype");
      return String.prototype;
    }
    return Reflect.get(target, property, receiver);
  }
});

Reflect.construct(String, [argument], newTarget);

assertSameContent(["toString", "prototype"], log);
