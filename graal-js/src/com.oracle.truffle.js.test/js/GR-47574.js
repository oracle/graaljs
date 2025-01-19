/*
 * Copyright (c) 2023, 2025, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * @option temporal=true
 * @option debug-builtin=true
 */

load("assert.js");

/*
 * Test intrinsic default proto of Temporal constructors.
 */
let newTargetWithNullPrototype = function(){};
newTargetWithNullPrototype.prototype = null;

for (let [constructor, args] of [
    [Temporal.Duration, []],
    [Temporal.PlainDate, [2019, 4, 11]],
    [Temporal.PlainDateTime, [2020, 3, 14, 13, 37]],
    [Temporal.PlainTime, [13, 37]],
    [Temporal.PlainMonthDay, [3, 14]],
    [Temporal.PlainYearMonth, [2019, 4]],
    [Temporal.Instant, [0n]],
    [Temporal.ZonedDateTime, [0n, 'UTC', 'iso8601']],
]) {
    try {
        assertSame(
            Object.getPrototypeOf(Reflect.construct(constructor, args, newTargetWithNullPrototype)),
            constructor.prototype
        );
    } catch (e) {
        e.message += ` (${constructor.name})`;
        throw e;
    } 
}

/*
 * Get(newTarget, "prototype") may detach the buffer.
 */
let detachBufferOnGetPrototype = new Proxy(function(){}, {
    get(target, key, receiver) {
        if (key == 'prototype') {
            Debug.typedArrayDetachBuffer(buffer);
            return DataView.prototype;            
        } else {
            return Reflect.get(target, key, receiver);
        }
    }
});

let buffer = new ArrayBuffer(8);

assertThrows(() => {
    Reflect.construct(DataView, [buffer], detachBufferOnGetPrototype);
}, TypeError); // TypeError: Detached buffer.
