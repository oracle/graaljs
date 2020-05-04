/*
 * Copyright (c) 2020, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Tests of logical assignment operators.
 * 
 * @option ecmascript-version=2021
 */

load('assert.js');

var return0 = function() {
    return 0;
};
var return1 = function() {
    return 1;
};
var thrower = function() {
    throw new Error('Unexpected invocation of setter.');
};

var obj = {};
Object.defineProperty(obj, "prop0", { get: return0, set: thrower });
Object.defineProperty(obj, "prop1", { get: return1, set: thrower });

assertSame(0, obj['prop0'] &&= 42);
assertSame(1, obj['prop1'] ||= 42);
assertSame(1, obj['prop1'] ??= 42);

with (obj) {
  assertSame(0, prop0 &&= 42);
  assertSame(1, prop1 ||= 42);
  assertSame(1, prop1 ??= 42);
}

Object.defineProperty(this, "prop0", { get: return0, set: thrower });
Object.defineProperty(this, "prop1", { get: return1, set: thrower });

assertSame(0, prop0 &&= 42);
assertSame(1, prop1 ||= 42);
assertSame(1, prop1 ??= 42);

true;
