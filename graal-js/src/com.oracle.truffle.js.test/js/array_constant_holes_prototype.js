/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * concat must copy inherited elements from holey prototypes without materializing absent elements,
 * even when the receiver has no own elements or is shorter than its prototype.
 */

load("./assert.js");

for (const prototype of [["p", , "q"], [, "p", ,], [, , "q"], [1, , 2, ,], [, 1, , 2], [1.5, , 2.5], [, 1.5], [{}, , {}], [, {}], [,,,]]) {
    for (const layout of ["empty", "deleted", "sparse"]) {
        for (const length of [0, 1, 3, 10]) {
            const array = [];
            if (layout === "deleted") {
                array[0] = "discard";
                delete array[0];
            } else if (layout === "sparse") {
                array.length = 0x80000000;
            }
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
}
