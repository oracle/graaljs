/*
 * Copyright (c) 2022, 2022, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

// Tests the order of operations when SuperPropertyReference is deleted,
// as reported by https://github.com/oracle/graaljs/issues/671

load('assert.js');

// Original test-case
assertThrows(function() {
    class C { static x = delete super[0()]; }
}, ReferenceError);

var toPropertyKeyInvoked = false;
var toPropertyKeyReporter = {
    toString() {
        toPropertyKeyInvoked = true;
        return 'key';
    }
};

// ToPropertyKey(key) should not be evaluated before ReferenceError is thrown
assertThrows(function() {
    class C { static x = delete super[toPropertyKeyReporter]; }
}, ReferenceError);
assertFalse(toPropertyKeyInvoked);
