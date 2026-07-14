/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Tests Iterator.prototype.includes.
 *
 * @option ecmascript-version=staging
 */

load("../assert.js");

assertSame("function", typeof Iterator.prototype.includes);
assertSame("includes", Iterator.prototype.includes.name);
assertSame(1, Iterator.prototype.includes.length);
assertThrows(() => new Iterator.prototype.includes(), TypeError);

{
    const descriptor = Object.getOwnPropertyDescriptor(Iterator.prototype, "includes");
    assertTrue(descriptor.writable);
    assertFalse(descriptor.enumerable);
    assertTrue(descriptor.configurable);
}

function includes(values, searchElement, skippedElements) {
    const iterator = values.values();
    if (arguments.length < 3) {
        return iterator.includes(searchElement);
    }
    return iterator.includes(searchElement, skippedElements);
}

assertTrue(includes([1, 2, 3], 1));
assertTrue(includes([1, 2, 3], 3));
assertFalse(includes([1, 2, 3], 4));
assertFalse(includes([], undefined));
assertTrue(includes([undefined], undefined));

// SameValueZero comparison.
assertTrue(includes([NaN], NaN));
assertTrue(includes([+0], -0));
assertTrue(includes([-0], +0));
const object = {};
assertTrue(includes([object], object));
assertFalse(includes([{}], object));
const symbol = Symbol("test");
assertTrue(includes([symbol], symbol));
assertFalse(includes([Symbol("test")], symbol));

// skippedElements.
assertTrue(includes([1, 2, 3], 1, undefined));
assertTrue(includes([1, 2, 3], 1, +0));
assertTrue(includes([1, 2, 3], 1, -0));
assertFalse(includes([1, 2, 3], 1, 1));
assertTrue(includes([1, 2, 3], 2, 1));
assertFalse(includes([1, 2, 3], 2, 2));
assertFalse(includes([1, 2, 3], 3, Number.MAX_SAFE_INTEGER));
assertFalse(includes([1, 2, 3], 1, Infinity));

for (const skippedElements of [NaN, 0.1, -0.1, "0", null, true, 0n, {}, new Number(0)]) {
    assertThrows(() => includes([], 0, skippedElements), TypeError);
}
{
    let coerced = false;
    const skippedElements = {
        valueOf() {
            coerced = true;
            return 0;
        }
    };
    assertThrows(() => includes([], 0, skippedElements), TypeError);
    assertFalse(coerced);
}
for (const skippedElements of [-1, -Infinity]) {
    assertThrows(() => includes([], 0, skippedElements), RangeError);
}
for (const skippedElements of [Number.MAX_SAFE_INTEGER + 1, Number.MAX_SAFE_INTEGER + 3]) {
    assertThrows(() => includes([], 0, skippedElements), RangeError);
}

// The method is generic and gets the next method only once.
{
    let nextGets = 0;
    let nextCalls = 0;
    const iterator = {
        get next() {
            nextGets++;
            return function() {
                nextCalls++;
                return {value: nextCalls, done: nextCalls > 3};
            };
        }
    };
    assertTrue(Iterator.prototype.includes.call(iterator, 3));
    assertSame(1, nextGets);
    assertSame(3, nextCalls);
}

// A match closes the iterator, while natural exhaustion does not.
{
    let returnCalls = 0;
    const iterator = {
        next() {
            return {value: 42, done: false};
        },
        return() {
            returnCalls++;
            return {};
        }
    };
    assertTrue(Iterator.prototype.includes.call(iterator, 42));
    assertSame(1, returnCalls);
}
{
    let returnGets = 0;
    const iterator = {
        next() {
            return {done: true};
        },
        get return() {
            returnGets++;
            return function() { return {}; };
        }
    };
    assertFalse(Iterator.prototype.includes.call(iterator, 42));
    assertSame(0, returnGets);
}

// Errors while closing a matched iterator replace the successful result.
for (const [returnMethod, errorType] of [
    [42, TypeError],
    [function() { throw new SyntaxError("close"); }, SyntaxError],
    [function() { return 42; }, TypeError]
]) {
    const iterator = {
        next() { return {value: 42, done: false}; },
        return: returnMethod
    };
    assertThrows(() => Iterator.prototype.includes.call(iterator, 42), errorType);
}
{
    const iterator = {
        next() { return {value: 42, done: false}; },
        get return() { throw new SyntaxError("close"); }
    };
    assertThrows(() => Iterator.prototype.includes.call(iterator, 42), SyntaxError);
}

// Invalid skippedElements closes before the next method is read. Closing errors
// are suppressed in favor of the argument-validation error.
for (const [skippedElements, errorType] of [[0.5, TypeError], [-1, RangeError]]) {
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
    assertThrows(() => Iterator.prototype.includes.call(iterator, 0, skippedElements), errorType);
    assertSame(0, nextGets);
    assertSame(1, returnCalls);
}

// IteratorStepValue failures do not close the iterator.
for (const next of [
    function() { throw new SyntaxError("next"); },
    function() { return 42; },
    function() { return {get done() { throw new SyntaxError("done"); }}; },
    function() { return {done: false, get value() { throw new SyntaxError("value"); }}; }
]) {
    let returnCalls = 0;
    const iterator = {
        next,
        return() {
            returnCalls++;
            return {};
        }
    };
    assertThrows(() => Iterator.prototype.includes.call(iterator, 0));
    assertSame(0, returnCalls);
}

// The receiver is validated before skippedElements and is not boxed.
assertThrows(() => Iterator.prototype.includes.call(null, 0, 0.5), TypeError);
assertThrows(() => Iterator.prototype.includes.call(42, 0), TypeError);
