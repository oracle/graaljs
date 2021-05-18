/*
 * Copyright (c) 2021, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Allocation of a large Memory should either succeed or throw RangeError
 * 
 * @option webassembly
 */

load('../js/assert.js');

var memories = [];
try {
    for (var i = 0; i < 20; i++) {
        memories.push(new WebAssembly.Memory({ initial: 32767 }));
    }
} catch (e) {
    assertTrue(e instanceof RangeError);
}
