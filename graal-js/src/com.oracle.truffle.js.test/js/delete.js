/*
 * Copyright (c) 2020, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

// Tests of delete operator

load('assert.js');

// Test-case from https://github.com/graalvm/graaljs/issues/324
assertThrows(function() {
    "use strict";
    delete 'x'[0];
}, TypeError);

assertThrows(function() {
    "use strict";
    delete 'x'.length;
}, TypeError);

function toPropertyKeyCheck(object) {
    var called = false;
    var key = {
        toString: function() {
            called = true;
            return 'someKey';
        }
    };
    delete object[key];
    assertTrue(called);
}

toPropertyKeyCheck(42);
toPropertyKeyCheck(Debug.createSafeInteger(43));
toPropertyKeyCheck(3.14);
toPropertyKeyCheck(211n);
toPropertyKeyCheck(true);
toPropertyKeyCheck('txt');
toPropertyKeyCheck(Symbol());

var javaObject = new java.lang.Object();
toPropertyKeyCheck(javaObject);

assertTrue(delete javaObject[Symbol()]);

true;
