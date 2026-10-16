/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Tests splice removal across the index offset after the backing store grows to the left.
 * Covers contiguous and holes arrays and checks that subsequent writes do not expose stale elements.
 */

load("./assert.js");

function checkElements(array, entries) {
    assertSameContent(entries.map(entry => String(entry.index)), Object.keys(array));
    for (const entry of entries) {
        assertSame(entry.value, array[entry.index]);
    }
}

const base = 1000000;
for (const values of [[11, 22, 33], [1.5, 2.5, 3.5], ["a", "b", "c"], [{}, {}, {}]]) {
    for (const gap of [1, 2]) {
        for (const start of [0, base - 6]) {
            for (let end = base - 5; end <= base + gap + 1; end++) {
                const array = [];
                array[base] = values[0];
                array[base + gap] = values[1];
                // Prepending grows the backing store to the left, giving both offsets nonzero values.
                array[base - 1] = values[2];
                const entries = [{index: base - 1, value: values[2]}, {index: base, value: values[0]}, {index: base + gap, value: values[1]}];
                const length = array.length;
                const count = end - start;
                const removed = array.splice(start, count);

                assertSame(count, removed.length);
                checkElements(removed, entries.filter(entry => start <= entry.index && entry.index < end).map(entry => ({index: entry.index - start, value: entry.value})));
                assertSame(length - count, array.length);
                const remaining = entries.filter(entry => entry.index < start || entry.index >= end).map(entry => ({index: entry.index < start ? entry.index : entry.index - count, value: entry.value}));
                checkElements(array, remaining);

                // Extending the used range must not expose stale copies of relocated elements.
                const nextIndex = array.length + 5;
                array[nextIndex] = values[0];
                remaining.push({index: nextIndex, value: values[0]});
                checkElements(array, remaining);
            }
        }
    }
}
