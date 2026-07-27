/*
 * Copyright (c) 2026, 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

load("assert.js");

const toPrimitive = java[Symbol.toPrimitive];

for (const hint of ["string", "default", "number"]) {
    assertThrows(() => toPrimitive.call({}, hint), TypeError);
}
