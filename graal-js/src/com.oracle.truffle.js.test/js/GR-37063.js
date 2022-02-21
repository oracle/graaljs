/*
 * Copyright (c) 2022, 2022, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Regression test for the resumption state reset in ClassDefinitionNode.
 */

load('assert.js');

let g = function* () {
    for (var i = 0; i < 2; i++) {
        (class {
            [yield] = 42;
            [yield] = 'foo';
        });
    }
};

let iter = g();

assertSame(false, iter.next().done); // i == 0, first yield
assertSame(false, iter.next().done); // i == 0, second yield
assertSame(false, iter.next().done); // i == 1, first yield
assertSame(false, iter.next().done); // i == 1, second yield
assertSame(true, iter.next().done);  // done
