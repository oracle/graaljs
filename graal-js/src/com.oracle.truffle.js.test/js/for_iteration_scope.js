/*
 * Copyright (c) 2021, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Check that for loops with let declarations have per-iteration scopes.
 */

load('assert.js');

const N = 5;

function verifyClosures(closures) {
    assertSame(N, closures.length);
    for (let i = 0; i < N; i++) {
        let closure = closures[i];
        assertSame(i + 2, closure());
        assertSame(i + 4, closure());
        assertSame(i + 6, closure());
    }
    for (let i = 0; i < N; i++) {
        let closure = closures[i];
        assertSame(i + 8, closure());
    }
}

(function testWithClosure() {
    let closures = [];
    for (let i = 0; i < N; i++) {
        let j = i;
        closures.push(() => i += 2);
    }
    verifyClosures(closures);
})();

(function testWithEval() {
    let closures = [];
    for (let i = 0; i < N; i++) {
        closures.push(eval("() => i += 2"));
    }
    verifyClosures(closures);
})();
