/*
 * Copyright (c) 2022, 2022, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Tests catch parameter temporal dead zone.
 */

load('assert.js');

(function testCatchParameterTDZ() {
    function fwdref(a) {
        try {
            throw a;
        } catch ([x = y, y]) {
            assertSame(1, x);
            assertSame(2, y);
        }
    }
    fwdref([1, 2]);
    assertThrows(() => fwdref([]), ReferenceError);
})();

(function testCatchParameterTDZClosure() {
    function fwdref(a) {
        try {
            throw a;
        } catch ([x = (() => y)(), y]) {
            assertSame(1, x);
            assertSame(2, y);
        }
    }
    fwdref([1, 2]);
    assertThrows(() => fwdref([]), ReferenceError);
})();

(function testCatchParameterNoBindings() {
    function catchEmptyArray(a) {
        try {
            throw a;
        } catch ([]) {
        }
    }
    catchEmptyArray([1, 2]);
    catchEmptyArray([]);
})();
