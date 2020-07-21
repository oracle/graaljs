/*
 * Copyright (c) 2020, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

load('assert.js');

function testReturn() {
    if (true)
        return
    ; else {}
    return 42;
}

assertSame(undefined, testReturn());

var ok = true;
for (var i=0; i<42; i++) {
    if (true)
        continue
    ; else {}
    ok = false;
}

assertSame(true, ok);

ok = true;
while (true) {
    if (true)
        break
    ; else {}
    ok = false;
}

assertSame(true, ok);

true;
