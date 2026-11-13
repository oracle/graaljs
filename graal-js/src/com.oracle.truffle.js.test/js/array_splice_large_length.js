/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * splice insertion near the signed-int length limit must preserve elements across storage transitions
 * and shifted backing-store offsets without overflowing logical or physical insertion indices.
 */

load("./assert.js");

for (const values of [[11, 22], [1.5, 2.5], ["a", "b"], [{}, {}]]) {
    for (const layout of ["zeroBased", "contiguous", "holes", "shifted", "shiftedFull", "shiftedFullHoles"]) {
        for (const length of [0x7ffffffe, 0x7fffffff]) {
            for (const offset of [0, 2, 8, length - 2, length]) {
                for (const items of [[values[1]], [values[1], "replacement"]]) {
                    const array = [];
                    if (layout === "zeroBased") {
                        array.push(values[0]);
                    } else if (layout.startsWith("shifted")) {
                        for (let i = 0; i < (layout === "shifted" ? 3 : 8); i++) {
                            array.push(values[i % values.length]);
                        }
                        array.shift();
                        array.shift();
                        if (layout === "shiftedFullHoles") {
                            delete array[1];
                        }
                    } else {
                        array[2] = values[0];
                        array[layout === "holes" ? 4 : 3] = values[1];
                    }
                    const entries = Object.keys(array).map(index => ({index: Number(index), value: array[index]}));
                    array.length = length;
                    const removed = array.splice(offset, 0, ...items);

                    assertSame(0, removed.length);
                    assertSame(length + items.length, array.length);
                    const expected = entries.map(entry => ({index: entry.index < offset ? entry.index : entry.index + items.length, value: entry.value}));
                    items.forEach((value, index) => expected.push({index: offset + index, value}));
                    expected.sort((a, b) => a.index - b.index);
                    assertSameContent(expected.map(entry => String(entry.index)), Object.keys(array));
                    for (const entry of expected) {
                        assertSame(entry.value, array[entry.index]);
                    }
                }
            }
        }
    }
}
