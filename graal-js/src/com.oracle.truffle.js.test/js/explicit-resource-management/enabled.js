/*
 * Copyright (c) 2026, 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/*
 * @option explicit-resource-management
 */

load('../assert.js');

(function testUsingScopePreservesNullAndUndefinedErrors() {
    function throwInBody(value) {
        try {
            using x = { [Symbol.dispose]() {} };
            throw value;
        } catch (e) {
            return e;
        }
    }

    function throwInDispose(value) {
        try {
            using x = { [Symbol.dispose]() { throw value; } };
        } catch (e) {
            return e;
        }
    }

    assertSame(null, throwInBody(null));
    assertSame(undefined, throwInBody(undefined));
    assertSame(null, throwInDispose(null));
    assertSame(undefined, throwInDispose(undefined));
})();

(function testForUsingOfRemainsValid() {
    var using = 0;
    for (using of [1]) {
    }
    assertSame(1, using);
})();

(function testForUsingOfOfRemainsIdentifierBased() {
    var using;
    var of = [[9], [8], [7]];
    var result = [];
    for (using of of [0, 1, 2]) {
        result.push(using);
    }
    assertSame(1, result.length);
    assertSame(7, result[0]);
})();
