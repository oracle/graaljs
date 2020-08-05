/*
 * Copyright (c) 2020, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Tests of switch statement.
 */

load('assert.js');

(function emptyCaseAtTheEnd() {
    var input = 1;
    var output;
    switch (input) {
        default:
            output = "default";
            break;
        case 1:
    }
    assertSame(undefined, output);
})();

(function fallThroughToDefaultNotAtTheEnd() {
    var input = 1;
    var output;
    switch (input) {
        case 1:
        default:
            output = "default or 1";
            break;
        case 2:
            output = "case 2";
            break;
    }
    assertSame("default or 1", output);
})();

(function fallThroughConditionNotEvaluated() {
    var called = 0;
    function f(x) {
      called++;
      return x;
    }
    var input = 2;
    var output;
    var answer;
    switch (input) {
        case f(2):
        case f(4):
            output = "2 or 4";
            break;
        case f(5):
            // non-empty fallthrough case to prevent if-else cascade transformation.
            answer = 42;
        case f(6):
            output = "5 or 6";
            break;
    }
    assertSame("2 or 4", output);
    assertSame(1, called);
})();

true;
