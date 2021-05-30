/*
 * Copyright (c) 2021, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Regression test inspired by the pattern used in pino-transmit-http (npm package).
 */

load('assert.js');

function verify(o) {
    var args = o.arguments;
    assertSame(1, args.length);
    assertSame(42, args[0]);
    assertTrue(typeof args === 'object');
}

(function() {
    verify({ arguments });
})(42);

(function() {
    verify({ arguments });
    parseInt.apply(undefined, arguments);
})(42);
