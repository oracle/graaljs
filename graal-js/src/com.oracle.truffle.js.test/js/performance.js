/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/*
 * @option performance
 */

load("./assert.js")

assertSame('function', typeof performance.now);
assertSame(0, performance.now.length);

const perfNow = performance.now();
assertSame("number", typeof perfNow); // performance.now() result must be a number
assertTrue(perfNow >= 0); // performance.now() result must be >= 0

const timeOrigin = performance.timeOrigin;
assertSame("number", typeof timeOrigin); // performance.timeOrigin result must be a number
assertTrue(timeOrigin > 0); // performance.timeOrigin result must be > 0

const nowWall = perfNow + timeOrigin;
const dateNow = Date.now();

// Test: difference between performance.now()+performance.timeOrigin and Date.now() is plausible
assertTrue(Math.abs(nowWall - dateNow) < 60_000 /*ms*/); // performance.now() + performance.timeOrigin should be close to Date.now()
assertTrue(Math.abs(timeOrigin - dateNow) < 3_600_000 /*ms*/); // performance.timeOrigin should be recent

// Test: Performance constructor and prototype.
assertSame("function", typeof Performance);
assertSame(Performance.prototype, Object.getPrototypeOf(performance));
assertTrue(performance instanceof Performance);

for (const member of ['now', 'timeOrigin', 'toJSON', Symbol.toStringTag]) {
    if (Object.hasOwn(performance, member)) throw member;
    if (!Object.hasOwn(Performance.prototype, member)) throw member;
}

{
    let descriptor = Object.getOwnPropertyDescriptor(Performance.prototype, 'now');
    assertSame(true, descriptor.writable);
    assertSame(true, descriptor.enumerable);
    assertSame(true, descriptor.configurable);
    assertSame("function", typeof descriptor.value);

    assertSame('number', typeof performance.now());

    // Test: receiver must be a Performance object.
    assertThrows(() => descriptor.value(), TypeError);
    assertSame(0, descriptor.value.length);
}

{
    let descriptor = Object.getOwnPropertyDescriptor(Performance.prototype, 'timeOrigin');
    assertSame(true, descriptor.enumerable);
    assertSame(true, descriptor.configurable);
    assertSame("function", typeof descriptor.get);
    assertSame("undefined", typeof descriptor.set);

    assertSame('number', typeof performance.timeOrigin);

    // Test: performance.timeOrigin is readonly
    assertThrows(() => { 'use strict'; performance.timeOrigin = 123; }, TypeError);

    // Test: receiver must be a Performance object.
    assertThrows(() => descriptor.get(), TypeError);
    assertSame(0, descriptor.get.length);
}

{
    let descriptor = Object.getOwnPropertyDescriptor(Performance.prototype, 'toJSON');
    assertSame(true, descriptor.writable);
    assertSame(true, descriptor.enumerable);
    assertSame(true, descriptor.configurable);
    assertSame("function", typeof descriptor.value);

    let json = performance.toJSON();
    assertSame('object', typeof json);
    assertSame(performance.timeOrigin, json.timeOrigin);

    // Test: receiver must be a Performance object.
    assertThrows(() => descriptor.value(), TypeError);
    assertSame(0, descriptor.value.length);
}

{
    let descriptor = Object.getOwnPropertyDescriptor(Performance.prototype, Symbol.toStringTag);
    assertSame(false, descriptor.writable);
    assertSame(false, descriptor.enumerable);
    assertSame(true, descriptor.configurable);
    assertSame("string", typeof descriptor.value);

    assertSame("Performance", descriptor.value);
    assertSame("[object Performance]", String(performance));
}
