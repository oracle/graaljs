/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Tests Iterator.prototype.join.
 *
 * @option ecmascript-version=staging
 */

load("../assert.js");

assertSame("function", typeof Iterator.prototype.join);
assertSame("join", Iterator.prototype.join.name);
assertSame(1, Iterator.prototype.join.length);
assertThrows(() => new Iterator.prototype.join(), TypeError);

{
    const descriptor = Object.getOwnPropertyDescriptor(Iterator.prototype, "join");
    assertTrue(descriptor.writable);
    assertFalse(descriptor.enumerable);
    assertTrue(descriptor.configurable);
}

assertSame("", [].values().join());
assertSame("one", ["one"].values().join());
assertSame("one,two,three", ["one", "two", "three"].values().join());
assertSame("one&&two&&three", ["one", "two", "three"].values().join("&&"));
assertSame("onetwothree", ["one", "two", "three"].values().join(""));
assertSame("one,,two,,three", ["one", null, "two", undefined, "three"].values().join());
assertSame("0,true,false", [0, true, false].values().join());
assertSame("onenulltwo", ["one", "two"].values().join(null));

// The separator and non-nullish values are each converted exactly once.
{
    let separatorConversions = 0;
    let valueConversions = 0;
    const separator = {
        toString() {
            separatorConversions++;
            return " - ";
        }
    };
    const value = {
        toString() {
            valueConversions++;
            return "value";
        }
    };
    assertSame("value - value", [value, value].values().join(separator));
    assertSame(1, separatorConversions);
    assertSame(2, valueConversions);
}

// The method is generic and caches the next method.
{
    let nextGets = 0;
    let nextCalls = 0;
    const iterator = {
        get next() {
            nextGets++;
            return function() {
                nextCalls++;
                return nextCalls <= 2 ? {value: nextCalls, done: false} : {done: true};
            };
        }
    };
    assertSame("1,2", Iterator.prototype.join.call(iterator));
    assertSame(1, nextGets);
    assertSame(3, nextCalls);
}

// Separator conversion happens before the next method is read. A conversion
// failure closes the raw receiver and preserves the conversion error.
{
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
    const separator = {
        toString() {
            throw new SyntaxError("separator");
        }
    };
    assertThrows(() => Iterator.prototype.join.call(iterator, separator), SyntaxError);
    assertSame(0, nextGets);
    assertSame(1, returnCalls);
}

// A value conversion failure closes the iterator and preserves the conversion error.
{
    let nextCalls = 0;
    let returnCalls = 0;
    const iterator = {
        next() {
            nextCalls++;
            return {done: false, value: {toString() { throw new SyntaxError("value"); }}};
        },
        return() {
            returnCalls++;
            throw new Error("suppressed close error");
        }
    };
    assertThrows(() => Iterator.prototype.join.call(iterator), SyntaxError);
    assertSame(1, nextCalls);
    assertSame(1, returnCalls);
}

// Iterator protocol failures do not close the iterator.
for (const next of [
    function() { throw new SyntaxError("next"); },
    function() { return 42; },
    function() { return {get done() { throw new SyntaxError("done"); }}; },
    function() { return {done: false, get value() { throw new SyntaxError("value"); }}; }
]) {
    let returnGets = 0;
    const iterator = {
        next,
        get return() {
            returnGets++;
            return function() { return {}; };
        }
    };
    assertThrows(() => Iterator.prototype.join.call(iterator));
    assertSame(0, returnGets);
}

// Natural exhaustion does not close the iterator.
{
    let returnGets = 0;
    const iterator = {
        next() { return {done: true}; },
        get return() {
            returnGets++;
            return function() { return {}; };
        }
    };
    assertSame("", Iterator.prototype.join.call(iterator));
    assertSame(0, returnGets);
}

for (const receiver of [undefined, null, false, 0, 0n, "", Symbol()]) {
    assertThrows(() => Iterator.prototype.join.call(receiver), TypeError);
}
