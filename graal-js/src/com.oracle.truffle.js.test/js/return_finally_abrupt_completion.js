/*
 * Copyright (c) 2026, 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

load('assert.js');

function testReturnFinally() {
    try {
        return 42;
    } finally {
        do try {
            return 43;
        } finally {
            break;
        } while (false);
    }
}

assertSame(42, testReturnFinally());

function* testGeneratorReturnFinally() {
    try {
        return 42;
    } finally {
        do try {
            return 43;
        } finally {
            break;
        } while (false);
    }
}

const result = testGeneratorReturnFinally().next();
assertSame(true, result.done);
assertSame(42, result.value);
