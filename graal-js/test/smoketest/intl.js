/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

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


var result = Intl.getCanonicalLocales('EN-US')
assertTrue(result.length > 0)

var numberDe = new Intl.NumberFormat('de-DE').format(123456.789);
assertSame("123.456,789", numberDe);

