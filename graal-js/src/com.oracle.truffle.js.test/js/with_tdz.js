/*
 * Copyright (c) 2022, 2022, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Tests that variables in temporal dead zone shadowed by a with statement can be accessed.
 */

load('assert.js')

with ({ok: 'OK!'}) {
    assertSame('OK!', ok);
}
with ({}) {
    assertThrows(() => ok, ReferenceError);
}
let ok;

(function() {
    with ({ok: 'OK!'}) {
        assertSame('OK!', ok);
    }
    with ({}) {
        assertThrows(() => ok, ReferenceError);
    }
    let ok;
})();

// Alternative version without closure.
(function() {
    with ({ok: 'OK!'}) {
        assertSame('OK!', ok);
    }
    with ({}) {
        try {
            throw new Error("should have thrown ReferenceError but was: " + ok)
        } catch (e) {
            if (!(e instanceof ReferenceError)) {
                throw e;
            }
        }
    }
    let ok;
})();
