/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Tests Iterator.prototype.chunks and Iterator.prototype.windows.
 *
 * @option ecmascript-version=staging
 */

load("../assert.js");

function assertNestedContent(expected, actual) {
    assertSame(expected.length, actual.length);
    for (let i = 0; i < expected.length; i++) {
        assertSameContent(expected[i], actual[i]);
    }
}

for (const [name, length] of [["chunks", 1], ["windows", 1]]) {
    const method = Iterator.prototype[name];
    assertSame("function", typeof method);
    assertSame(name, method.name);
    assertSame(length, method.length);
    assertThrows(() => new method(1), TypeError);
    const descriptor = Object.getOwnPropertyDescriptor(Iterator.prototype, name);
    assertTrue(descriptor.writable);
    assertFalse(descriptor.enumerable);
    assertTrue(descriptor.configurable);
}

assertNestedContent([[0, 1], [2, 3], [4]], [0, 1, 2, 3, 4].values().chunks(2).toArray());
assertNestedContent([[0], [1], [2]], [0, 1, 2].values().chunks(1).toArray());
assertNestedContent([[0, 1, 2], [3, 4, 5]], [0, 1, 2, 3, 4, 5].values().chunks(3).toArray());
assertNestedContent([[0, 1, 2]], [0, 1, 2].values().chunks(100).toArray());
assertNestedContent([], [].values().chunks(2).toArray());

const windowsExpected = [[0, 1, 2], [1, 2, 3], [2, 3, 4], [3, 4, 5]];
assertNestedContent(windowsExpected, [0, 1, 2, 3, 4, 5].values().windows(3).toArray());
assertNestedContent(windowsExpected, [0, 1, 2, 3, 4, 5].values().windows(3, undefined).toArray());
assertNestedContent(windowsExpected, [0, 1, 2, 3, 4, 5].values().windows(3, "only-full").toArray());
assertNestedContent(windowsExpected, [0, 1, 2, 3, 4, 5].values().windows(3, "allow-partial").toArray());
assertNestedContent([[0], [1], [2]], [0, 1, 2].values().windows(1).toArray());
assertNestedContent([], [0, 1, 2].values().windows(4).toArray());
assertNestedContent([[0, 1, 2]], [0, 1, 2].values().windows(4, "allow-partial").toArray());
assertNestedContent([], [].values().windows(4, "allow-partial").toArray());

// Every yielded window is an independent, mutable Array.
{
    const helper = [0, 1, 2, 3].values().windows(2);
    const first = helper.next().value;
    const second = helper.next().value;
    first[1] = 42;
    assertSameContent([0, 42], first);
    assertSameContent([1, 2], second);
}

// The methods get next once when creating the helper, but consume lazily.
for (const name of ["chunks", "windows"]) {
    let nextGets = 0;
    let nextCalls = 0;
    const iterator = {
        get next() {
            nextGets++;
            return function() {
                nextCalls++;
                return nextCalls <= 3 ? {done: false, value: nextCalls} : {done: true};
            };
        }
    };
    const helper = Iterator.prototype[name].call(iterator, 2);
    assertSame(1, nextGets);
    assertSame(0, nextCalls);
    assertSameContent([1, 2], helper.next().value);
    assertSame(2, nextCalls);
}

// Sizes are Numbers and are not coerced.
for (const invalidSize of [undefined, NaN, 1.5, Infinity, -Infinity, "1", 1n, {}, new Number(1)]) {
    assertThrows(() => [].values().chunks(invalidSize), TypeError);
    assertThrows(() => [].values().windows(invalidSize), TypeError);
}
for (const invalidSize of [0, -0, -1, 2 ** 32]) {
    assertThrows(() => [].values().chunks(invalidSize), RangeError);
    assertThrows(() => [].values().windows(invalidSize), RangeError);
}
// The maximum size is accepted without eagerly allocating a huge buffer.
assertNestedContent([[1]], [1].values().chunks(2 ** 32 - 1).toArray());
assertNestedContent([[1]], [1].values().windows(2 ** 32 - 1, "allow-partial").toArray());

for (const undersized of ["", "allow_partial", null, false, {}, new String("only-full")]) {
    assertThrows(() => [].values().windows(1, undersized), TypeError);
}

// Validation failures close the raw receiver before next is read. Size
// validation precedes undersized validation.
for (const [name, args, errorType] of [
    ["chunks", [1.5], TypeError],
    ["chunks", [0], RangeError],
    ["windows", [1.5, "invalid"], TypeError],
    ["windows", [0, "invalid"], RangeError],
    ["windows", [1, "invalid"], TypeError]
]) {
    let nextGets = 0;
    let returnCalls = 0;
    const iterator = {
        get next() {
            nextGets++;
            throw new Error("next must not be read");
        },
        return() {
            returnCalls++;
            throw new Error("suppressed close error");
        }
    };
    assertThrows(() => Iterator.prototype[name].call(iterator, ...args), errorType);
    assertSame(0, nextGets);
    assertSame(1, returnCalls);
}

// Returning a helper closes a live underlying iterator.
for (const name of ["chunks", "windows"]) {
    let returnCalls = 0;
    const iterator = {
        next() { return {done: false, value: 1}; },
        return() {
            returnCalls++;
            return {};
        }
    };
    const helper = Iterator.prototype[name].call(iterator, 2);
    helper.next();
    helper.return();
    assertSame(1, returnCalls);
    assertSame(true, helper.next().done);
}

// A final undersized result is yielded after the underlying iterator is done;
// returning at that point must not close the already exhausted iterator.
for (const name of ["chunks", "windows"]) {
    let nextCalls = 0;
    let returnCalls = 0;
    const iterator = {
        next() {
            nextCalls++;
            return nextCalls === 1 ? {done: false, value: 1} : {done: true};
        },
        return() {
            returnCalls++;
            return {};
        }
    };
    const helper = name === "chunks"
                    ? Iterator.prototype.chunks.call(iterator, 2)
                    : Iterator.prototype.windows.call(iterator, 2, "allow-partial");
    assertSameContent([1], helper.next().value);
    helper.return();
    assertSame(0, returnCalls);
}

// Iterator protocol failures are propagated without closing the iterator.
for (const name of ["chunks", "windows"]) {
    let returnGets = 0;
    const iterator = {
        next() { throw new SyntaxError("next"); },
        get return() {
            returnGets++;
            return function() { return {}; };
        }
    };
    const helper = Iterator.prototype[name].call(iterator, 2);
    assertThrows(() => helper.next(), SyntaxError);
    assertSame(0, returnGets);
}

for (const receiver of [undefined, null, false, 0, 0n, "", Symbol()]) {
    assertThrows(() => Iterator.prototype.chunks.call(receiver, 1), TypeError);
    assertThrows(() => Iterator.prototype.windows.call(receiver, 1), TypeError);
}
