/*
 * Copyright (c) 2026, 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Optional global calls preserve Nashorn's property fallback behavior.
 *
 * @option nashorn-compat=true
 * @option ecmascript-version=2020
 */

load('assert.js');

var calls = [];
globalThis.__noSuchProperty__ = function(name) {
    calls.push('property:' + name);
    return undefined;
};
globalThis.__noSuchMethod__ = function(name) {
    calls.push('method:' + name);
    return 42;
};

// A var declaration preserves an existing global property's configurable attribute.
globalThis.known = function() {};
load({name: 'optional-global-call.js', script: `
    var known;
    assertTrue(delete globalThis.known);
    var argumentEvaluated = false;
    assertSame(undefined, known?.(argumentEvaluated = true));
    assertFalse(argumentEvaluated);
    assertSameContent(['property:known'], calls);

    calls.length = 0;
    assertSame(42, known());
    assertSameContent(['method:known'], calls);
`});
