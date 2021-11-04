/*
 * Copyright (c) 2021, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Tests a potential side-effect of JSRuntime.safeToString().
 */

load('assert.js');

try {
    Error.prepareStackTrace = function(e) { e.sideEffect = true; };
    var o = Object.create(null);
    Error.captureStackTrace(o);
    throw o;
} catch (e) {
    assertSame(undefined, e.sideEffect);
}
