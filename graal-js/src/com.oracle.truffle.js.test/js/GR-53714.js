/*
 * Copyright (c) 2024, 2025, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Regression test of the handling of long values.
 * 
 * @option temporal
 * @option v8-compat
 */

load('assert.js');

var long0 = java.lang.Long.parseLong("0");
var long20 = java.lang.Long.parseLong("20");
var long42 = java.lang.Long.parseLong("42");
var long1234 = java.lang.Long.parseLong("1234");

assertSame('22', 42..toString(long20));
assertSameContent([], [].splice(long0));
assertSameContent([], (new Array('foo'), new Array(long0)));
assertSameContent([long20, long42], [long42, long20].sort());
assertSame(Number.prototype, Object.getPrototypeOf(long42));

var o = { 42: 'foo'};

assertSame(JSON.stringify(o, null, 20), JSON.stringify(o, null, long20));
assertSame(JSON.stringify(o, [42]), JSON.stringify(o, [long42]));

assertThrows(() => (0 instanceof long0), TypeError);

var f = function() {};
Object.defineProperty(f, 'length', { value: long42 });
assertSame(42, f.bind().length);

o = {};
Object.defineProperty(o, 'foo', { value: long1234 });
Object.defineProperty(o, 'foo', { value: long1234 });

// inspired by mjsunit/regress-3718.js
function getTypeName(receiver) {
  Error.prepareStackTrace = function(e, stack) { return stack; }
  var stack = (function() { return new Error().stack; }).call(receiver);
  Error.prepareStackTrace = undefined;
  return stack[0].getTypeName();
}

assertSame("Number", getTypeName(long42));
