/*
 * Copyright (c) 2021, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

load("assert.js");

// Check that has/get/set is called while sorting an array of length 1.
var log = [];
var handler = {
    has: (target, key) => (log.push("has " + key), Reflect.has(target, key)),
    get: (target, key) => (log.push("get " + key), Reflect.get(target, key)),
    set: (target, key, value) => (log.push("set " + key), Reflect.set(target, key, value))
};
var proxy = new Proxy([42], handler);
proxy.sort();

assertSame(5, log.length);
assertSame("get sort", log[0]);
assertSame("get length", log[1]);
assertSame("has 0", log[2]);
assertSame("get 0", log[3]);
assertSame("set 0", log[4]);
