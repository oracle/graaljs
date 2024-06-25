/*
 * Copyright (c) 2020, 2023, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Tests of CallSite built-ins.
 * 
 * @option v8-compat
 */

load('assert.js');

function receiverChecks(callSite) {
    for (var illegalReceiver of [42, null, undefined, {}]) {
        assertThrows(() => callSite.isToplevel.call(illegalReceiver), TypeError);
        assertThrows(() => callSite.getFileName.call(illegalReceiver), TypeError);
        assertThrows(() => callSite.getLineNumber.call(illegalReceiver), TypeError);
    }
}

function check() {
    var oldPrepareStackTrace = Error.prepareStackTrace;
    Error.prepareStackTrace = (e, stack) => e.stack = stack;
    var stack = new Error().stack;
    Error.prepareStackTrace = oldPrepareStackTrace;

    var callSite = stack[0];
    assertTrue(callSite.isToplevel());
    assertSame(globalThis, callSite.getThis());
    assertSame('global', callSite.getTypeName());
    receiverChecks(callSite);
}

function checkStrict(thiz) {
    "use strict";
    var oldPrepareStackTrace = Error.prepareStackTrace;
    Error.prepareStackTrace = (e, stack) => e.stack = stack;
    var stack = new Error().stack;
    Error.prepareStackTrace = oldPrepareStackTrace;

    var callSite = stack[0];
    assertTrue(callSite.isToplevel());
    assertSame(undefined, callSite.getThis());
    assertSame((thiz === globalThis) ? 'global' : null, callSite.getTypeName());
    receiverChecks(callSite);
}

for (let thiz of [undefined, null, globalThis]) {
    check.call(thiz);
    checkStrict.call(thiz, thiz);
}
