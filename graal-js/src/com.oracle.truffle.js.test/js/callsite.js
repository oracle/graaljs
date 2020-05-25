/*
 * Copyright (c) 2020, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Tests of CallSite built-ins.
 */

load('assert.js');

function check() {
    var oldPrepareStackTrace = Error.prepareStackTrace;
    Error.prepareStackTrace = (e, stack) => e.stack = stack;
    var stack = new Error().stack;
    Error.prepareStackTrace = oldPrepareStackTrace;

    assertTrue(stack[0].isToplevel());
    assertSame(globalThis, stack[0].getThis());
    assertSame('global', stack[0].getTypeName());
}

function checkStrict(thiz) {
    "use strict";
    var oldPrepareStackTrace = Error.prepareStackTrace;
    Error.prepareStackTrace = (e, stack) => e.stack = stack;
    var stack = new Error().stack;
    Error.prepareStackTrace = oldPrepareStackTrace;

    assertTrue(stack[0].isToplevel());
    assertSame(undefined, stack[0].getThis());
    assertSame((thiz === globalThis) ? 'global' : null, stack[0].getTypeName());
}

for (let thiz of [undefined, null, globalThis]) {
    check.call(thiz);
    checkStrict.call(thiz, thiz);
}

true;
