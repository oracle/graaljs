/*
 * Copyright (c) 2021, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Tests break statement (without a label) nested in a labelled statement.
 */

load('assert.js');

var fail = false;
switch (1) {
    case 1:
        lbl: break;
    default:
        fail = true;
}
assertFalse(fail);

fail = false;
for (var i = 0; i < 1; i++) {
    lbl: break;
    fail = true;
}
assertFalse(fail);
