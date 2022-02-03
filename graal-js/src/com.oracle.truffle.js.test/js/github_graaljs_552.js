/*
 * Copyright (c) 2022, 2022, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

// Tests the comparison involving long value coming from valueOf() method of the compared object
// as reported by https://github.com/oracle/graaljs/issues/552

load('assert.js');

var Long = java.lang.Long;
var big = {
    valueOf() {
        return Long.valueOf(10000000000);
    }
};
var bigPlusOne = {
    valueOf() {
        return Long.valueOf(10000000001);
    }
};
var negative = {
    valueOf() {
        return Long.valueOf(-10000000000);
    }    
};

assertTrue(big < bigPlusOne);
assertTrue(bigPlusOne > big);
assertTrue(negative <= 0);
assertTrue(0 >= negative);
