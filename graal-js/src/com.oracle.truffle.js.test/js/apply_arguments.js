/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Regression test for f.apply(_, arguments) optimization.
 */
load("assert.js");

function verifyArguments(args) {
    assertSame(1, args.length);
    assertSame(42, args[0]);
    assertSame('[object Arguments]', Object.prototype.toString.call(args));
    assertFalse(Array.isArray(args));
}

(function test1() {
    var arguments;
    var oh = {
        apply(thiz, args) {
            verifyArguments(args);
            return args;
        }
    };
    oh.apply(undefined, arguments);
})(42);

(function test2() {
    var oh = {
        apply(thiz, args) {
            verifyArguments(args);
            return args;
        }
    };
    oh.apply(null, arguments);
    var arguments = [42];
})(42);

(function test3() {
    var arguments = [43];
    var oh = {
        apply(thiz, args) {
            assertSame(1, args.length);
            assertSame(43, args[0]);
            assertSame('[object Array]', Object.prototype.toString.call(args));
            assertTrue(Array.isArray(args));
            return args;
        }
    };
    oh.apply(null, arguments);
})(42);
