/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Tests that import arguments are evaluated once and in order across await and yield.
 *
 * @option import-attributes=true
 * @option unhandled-rejections=throw
 */

load("../assert.js");

let log = [];
function specifier() {
    log.push("specifier");
    return {
        toString() {
            log.push("toString");
            return "data:text/javascript,export default 42;";
        }
    };
}
function options() {
    log.push("options");
    return {};
}
const expectedLog = ["specifier", "options", "toString"];

// Top-level await in the options argument must preserve the evaluated specifier.
assertSame(42, (await import(specifier(), await Promise.resolve(options()))).default);
assertSameContent(expectedLog, log);

async function testAwait() {
    log = [];
    assertSame(42, (await import(specifier(), await Promise.resolve(options()))).default);
    assertSameContent(expectedLog, log);

    // Resuming the second await must not restart the first await.
    log = [];
    assertSame(42, (await import(await Promise.resolve(specifier()), await Promise.resolve(options()))).default);
    assertSameContent(expectedLog, log);
}
await testAwait();

function* yieldOptions() {
    return import(specifier(), yield options());
}
log = [];
let iterator = yieldOptions();
let step = iterator.next();
assertFalse(step.done);
step = iterator.next(step.value);
assertTrue(step.done);
assertSame(42, (await step.value).default);
assertSameContent(expectedLog, log);

function* yieldBoth() {
    return import(yield specifier(), yield options());
}
log = [];
iterator = yieldBoth();
step = iterator.next();
assertFalse(step.done);
step = iterator.next(step.value);
assertFalse(step.done);
step = iterator.next(step.value);
assertTrue(step.done);
assertSame(42, (await step.value).default);
assertSameContent(expectedLog, log);
