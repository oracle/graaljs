/*
 * Copyright (c) 2022, 2022, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

// Regression test for a bug in the compound index assignment
// as reported by https://github.com/oracle/graaljs/issues/586

load('assert.js');

var foo = function () {
    var o = {
        b: 1
    };
    var a = o;
    a[a = 'b'] += 1;
    return o['b'];
};

assertSame(2, foo());
