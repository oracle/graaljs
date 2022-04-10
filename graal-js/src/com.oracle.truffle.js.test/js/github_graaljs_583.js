/*
 * Copyright (c) 2022, 2022, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

// Tests the handling of function declarations in switch statement
// as reported by https://github.com/oracle/graaljs/issues/583

load('assert.js');

function z(x) {
    switch(x) {
        case 1:
            function f() { return 1; }
            break;
        case 2:
            function f() { return 2; }
            break;
    }
    return f();
}

assertSame(2, z(1));
assertSame(2, z(2));
