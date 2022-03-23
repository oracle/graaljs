/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/*
 * Verify that binding is preserved for obj's SetMutableBinding.
 */

var _log = [];

function log(line) {
  _log.push(line);
}

function checkOutput(expected) {
  var actual = _log.reduce((a, b) => a + "\n" + b);
  actual = actual.trim();
  expected = expected.trim();
  if (actual != expected) {
    throw `Expected:\n${expected}\n\nActual:\n${actual}`;
  }
  _log.length = 0;
}

var logHandler = {
  has(target, key) {
    var result = Reflect.has(target, key);
    if (typeof key === 'string' && key.length === 1) {
      log(`has ${key} ${result}`);
    }
    return result;
  },
  get(target, key, receiver) {
    var result = Reflect.get(target, key);
    if (typeof key === 'string' && key.length === 1) {
      log(`get ${key} ${result}`);
    }
    return result;
  },
  set(target, key, value, receiver) {
    var result = Reflect.set(target, key, value);
    if (typeof key === 'string' && key.length === 1) {
      log(`set ${key} ${value} ${result}`);
    }
    return result;
  },
};

// Note: Not supported by SpiderMonkey and JavaScriptCore
globalThis.__proto__ = new Proxy({}, logHandler);

(function() {
  "use strict";

  try {
    u = (globalThis.u = 5);
  } catch (e) {
    log(e.name);
  }
  log("--");
  try {
    globalThis.x = 1;
    x = 2;
    x = (delete globalThis.x, log("deleted"), 3);
  } catch (e) {
    log(e.name);
  }
})();

checkOutput(`
has u false
set u 5 true
ReferenceError
--
set x 1 true
has x true
has x true
set x 2 true
has x true
deleted
has x true
set x 3 true
`);

(function() {
  try {
    u = (globalThis.u = 5);
  } catch (e) {
    log(e.name);
  }
  log("--");
  try {
    globalThis.x = 1;
    x = 2;
    x = (delete globalThis.x, log("deleted"), 3);
  } catch (e) {
    log(e.name);
  }
})();

checkOutput(`
has u true
set u 5 true
has u true
set u 5 true
--
set x 1 true
has x true
has x true
set x 2 true
has x true
deleted
has x true
set x 3 true
`);
