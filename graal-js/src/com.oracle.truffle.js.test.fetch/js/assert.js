/*
 * Copyright (c) 2020, 2023, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

function assertThrows(fn, errorType, msg) {
    try {
        fn();
    } catch (ex) {
        if (errorType) {
            if (!(ex instanceof errorType)) {
                throw new Error('Expected ' + errorType.name + ', actual: ' + ex);
            }
        }
        if (msg) {
            if (ex.message.indexOf(msg) === -1) {
                throw new Error('Expected error message "' + ex.message + '" to contain "' + msg + '"');
            }
        }
        return;
    }
    throw Error('error expected for method: ' + fn);
}

function assertEqual(expected, actual) {
    if (expected != actual) {
        var error = 'Objects not equal - '
                + 'expected: [' + expected + '] vs. '
                + 'actual: [' + actual +']';
        throw new Error(error);
    }
}

function assertSame(expected, actual) {
    if (expected !== actual) {
        var error = 'Objects not same - '
                + 'expected: [' + expected + '] vs. '
                + 'actual: [' + actual +']';
        throw new Error(error);
    }
}

function assertSameContent(expected, actual) {
    assertTrue(expected.length >= 0);
    assertSame(expected.length, actual.length);
    for (var i = 0; i < expected.length; i++) {
        assertSame(expected[i], actual[i]);
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
