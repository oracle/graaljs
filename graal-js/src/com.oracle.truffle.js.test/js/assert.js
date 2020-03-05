/*
 * Copyright (c) 2020, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

function assertFail(fn, msg) {
    try {
        fn();
    } catch (ex) {
        if (msg) {
            if (ex.message.indexOf(msg) == -1) {
                throw new Error('Expected error message "' + ex.message + '" to contain "' + msg + '"');
            }
        } else if (!(ex instanceof TypeError)) {
            throw new Error('Expected TypeError, actual: ' + ex);
        }
        return true;
    }
    throw TypeError('error expected for method: ' + fn);
}

function assertSame(expected, actual) {
    if (expected !== actual) {
        var error = 'Objects not same - '
                + 'expected: [' + expected + '] vs. '
                + 'actual: [' + actual +']';
        throw new Error(error);
    }
}

function assertTrue(condition) {
    assertSame(true, condition);
}

function assertFalse(condition) {
    assertSame(false, condition);
}

function fail(msg) {
    throw Error('FAILED: ' + msg);
}

true;
