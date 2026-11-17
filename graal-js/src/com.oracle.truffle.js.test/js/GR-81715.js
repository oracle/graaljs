/*
 * Copyright (c) 2026, 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/*
 * Tests that array literals with holes and empty spreads preserve writes and holes
 * across splice operations, while retaining iterator side effects and handling nonempty spreads.
 */

load("./assert.js");

function assertHoles(array, length) {
    assertSame(length, array.length);
    for (let i = 0; i < length; i++) {
        assertSame(undefined, array[i]);
        assertFalse(Object.hasOwn(array, i));
    }
}

const cases = [
    [() => [, ...[]], 1],
    [() => [,,, ...[]], 3],
    [() => [...[], ,], 1],
    [() => [...[], ,,,], 3],
    [() => [, ...[], , ...[], ,], 3],
    [() => [, ...""], 1],
    [() => [...[]], 0],
    [() => [...[], ...[]], 0],
];

for (const [create, length] of cases) {
    assertHoles(create(), length);
    for (let index = 0; index <= length; index++) {
        const array = create();
        array[index] = 42;
        assertSame(42, array[index]);
        assertTrue(Object.hasOwn(array, index));
        assertSame(Math.max(length, index + 1), array.length);
        for (let i = 0; i < length; i++) {
            if (i !== index) {
                assertFalse(Object.hasOwn(array, i));
            }
        }
    }
    const array = create();
    const removed = array.splice(0, 1);
    assertHoles(removed, Math.min(length, 1));
    assertHoles(array, Math.max(length - 1, 0));
    array[0] = 42;
    assertSame(42, array[0]);
}

// Empty iterables must still be evaluated and consumed, in order.
const events = [];
function emptyIterable(id) {
    events.push('evaluate ' + id);
    return {
        [Symbol.iterator]() {
            events.push('iterator ' + id);
            return {
                next() {
                    events.push('next ' + id);
                    return {done: true};
                }
            };
        }
    };
}
const empty = [, ...emptyIterable(1), , ...emptyIterable(2), ,];
assertSameContent(['evaluate 1', 'iterator 1', 'next 1', 'evaluate 2', 'iterator 2', 'next 2'], events);
assertHoles(empty, 3);
empty[0] = 42;
assertSame(42, empty[0]);

// The same literal node may see both empty and nonempty spreads.
function withSpread(iterable) {
    return [, ...iterable, ,];
}
for (const iterable of [[], [undefined], [], [42], []]) {
    const array = withSpread(iterable);
    assertSame(iterable.length + 2, array.length);
    assertFalse(Object.hasOwn(array, 0));
    assertFalse(Object.hasOwn(array, array.length - 1));
    if (iterable.length !== 0) {
        assertTrue(Object.hasOwn(array, 1));
        assertSame(iterable[0], array[1]);
    }
    array[0] = 1;
    assertSame(1, array[0]);
}
