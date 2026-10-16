/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

load("./assert.js");

for (const setIntegrityLevel of [Object.preventExtensions, Object.seal, Object.freeze]) {
    const indices = /(a)(b)?/d.exec("a").indices;
    setIntegrityLevel(indices);

    assertSameContent([0, 1], Object.getOwnPropertyDescriptor(indices, "0").value);
    assertSameContent([0, 1], Object.getOwnPropertyDescriptor(indices, "1").value);
    assertSame(undefined, Object.getOwnPropertyDescriptor(indices, "2").value);
}
