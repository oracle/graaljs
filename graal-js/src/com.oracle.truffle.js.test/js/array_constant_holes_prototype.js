/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

load("./assert.js");

for (const prototype of [["p", , "q"], [1, , 2, ,], [1.5, , 2.5], [{}, , {}], [,,,]]) {
    for (const length of [0, 1, 3, 10]) {
        const array = [];
        array.length = length;
        Object.setPrototypeOf(array, prototype);

        const result = [].concat(array);
        const expectedKeys = Object.keys(prototype).filter(key => Number(key) < length);
        assertSame(length, result.length);
        assertSameContent(expectedKeys, Object.keys(result));
        for (const key of expectedKeys) {
            assertSame(prototype[key], result[key]);
        }
    }
}
