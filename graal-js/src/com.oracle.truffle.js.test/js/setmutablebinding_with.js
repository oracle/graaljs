/*
 * Copyright (c) 2021, 2024, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/*
 * Verify if binding is preserved for with obj's SetMutableBinding.
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

(function() {
  var obj = {y: 42};
  with (new Proxy(obj, logHandler)) {
    y;
    log("--");
    y = (log("rhs"), 43);
    log("--");
    y = (delete obj.y, log("deleted"), 42);
    log("--");
    y;
  }
})();

checkOutput(`
has y true
has y true
get y 42
--
has y true
rhs
has y true
set y 43 true
--
has y true
deleted
has y false
set y 42 true
--
has y true
has y true
get y 42
`);


(function() {
  var obj = {};
  with (new Proxy(obj, logHandler)) {
    u = (obj.u = 41, log("rhs"), 42);
  }
})();

checkOutput(`
has u false
rhs
`);
