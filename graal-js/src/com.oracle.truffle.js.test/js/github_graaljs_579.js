/*
 * Copyright (c) 2022, 2022, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

// Tests the order of operations during assignment
// as reported by https://github.com/oracle/graaljs/issues/579

load('assert.js');

function test() {
    "use strict";
    var output = 'start';
    try {
        e = (output += ',rhs');
    } catch (err) {
        output += ',err';
    }
    return output;
};

assertSame('start,rhs,err', test());
