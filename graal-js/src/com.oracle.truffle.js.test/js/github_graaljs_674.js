/*
 * Copyright (c) 2022, 2022, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Tests the order of operations of Object.defineProperty() as reported by
 * https://github.com/oracle/graaljs/issues/674
 */

load('assert.js');

// Original test-case
assertThrows(function() {
    Object.defineProperty({} , { [Symbol.toPrimitive]: () => REF_ERR }, undefined);
}, ReferenceError);

var toPropertyKeyInvoked = false;
var toPropertyKeyReporter = {
    toString() {
        toPropertyKeyInvoked = true;
        return 'key';
    }
};

// Object check should be performed before ToPropertyKey()
assertThrows(function() {
    Object.defineProperty(undefined, toPropertyKeyReporter, undefined);
}, TypeError);
assertFalse(toPropertyKeyInvoked);

// ToPropertyKey() should be performed before ToPropertyDescriptor()
assertThrows(function() {
    Object.defineProperty({}, toPropertyKeyReporter, undefined);
}, TypeError);
assertTrue(toPropertyKeyInvoked);
