/*
 * Copyright (c) 2020, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

// Tests for the order of properties of functions of various types,
// motivated by https://github.com/graalvm/graaljs/issues/323

load('assert.js');

function check(fn) {
    var names = Object.getOwnPropertyNames(fn);
    assertTrue(names.length >= 2);
    assertSame("length", names[0]);
    assertSame("name", names[1]);
}

function fn() {}
async function asyncFn() {}
function* generator() {}
async function* asyncGenerator() {}
class Klass {}

// Check declarations
check(fn);
check(asyncFn);
check(generator);
check(asyncGenerator);
check(Klass)

// Check expressions
check(function() {});
check(async function() {});
check(function*() {});
check(async function*() {});
check(() => 42);
check(async () => 42);
check(class {});

true;
