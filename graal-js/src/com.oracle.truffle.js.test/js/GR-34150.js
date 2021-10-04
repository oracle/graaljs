/*
 * Copyright (c) 2021, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Tests of parsing of async arrow functions with a rest parameter.
 */

load('assert.js');

function assertSyntaxError(code) {
    try {
        eval(code);
        fail('SyntaxError expected');
    } catch (e) {
        assertTrue(e instanceof SyntaxError);
    }
}

assertSyntaxError('async (...x,) => x');
assertSyntaxError('async (...[x],) => x');
assertSyntaxError('async (...{length},) => length');

async (...x) => x;
async (...[x]) => x;
async (...{length}) => length;

// occurs in various npm packages
async (...x) =>
        x;

// comments and new-lines should not change the outcome
assertSyntaxError('async (...x,\n/**/) /**/ => \n/**/ x');
assertSyntaxError('async (...[x],\n/**/) /**/ => \n/**/ x');
assertSyntaxError('async (...{length},\n/**/) /**/ => \n/**/ length');
async (...x
        /**/) /**/ =>
        /**/x;
async (...[x]
        /**/) /**/ =>
        /**/x;
async (...{length}
        /**/) /**/ =>
        /**/length;
