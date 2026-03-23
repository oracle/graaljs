/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Verifies that import.defer does not observe Promise.prototype.then while waiting for
 * asynchronous transitive dependencies.
 *
 * @option ecmascript-version=staging
 * @option unhandled-rejections=throw
 */

load("../assert.js");

globalThis.importDeferThenLog = [];
const originalThen = Promise.prototype.then;
Promise.prototype.then = function () {
    throw new Error("Promise.prototype.then should not be called");
};

try {
    const ns = await import.defer("./fixtures/import_defer_promise_prototype_then_monkey_patch_root.mjs");

    assertSame("Deferred Module", ns[Symbol.toStringTag]);
    assertSame(2, globalThis.importDeferThenLog.length);
    assertSame("dep start", globalThis.importDeferThenLog[0]);
    assertSame("dep end", globalThis.importDeferThenLog[1]);

    assertSame(42, ns.value);
    assertSame(3, globalThis.importDeferThenLog.length);
    assertSame("root", globalThis.importDeferThenLog[2]);
} finally {
    Promise.prototype.then = originalThen;
    delete globalThis.importDeferThenLog;
}
