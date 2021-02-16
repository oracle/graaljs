/*
 * Copyright (c) 2021, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * @option shell
 */

// Regular assertSame does not work in promise jobs
// (it just triggers an unhandled promise rejection that is ignored).
var assertSame = function (expected, actual) {
    if (expected !== actual) {
        console.log(expected + ' !== ' + actual);
        quit(1);
    }
};

var step = 0;
var promiseJob = function() {
    Promise.resolve().then(function() {
        assertSame(2, ++step);
    });
    java.util.Collections.singleton(42).forEach(function() {});
    assertSame(1, ++step);
};

Promise.resolve().then(promiseJob);
