/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * WebAssembly.Global.prototype.value setter: abrupt completions of ToWebAssemblyValue
 * must propagate unchanged (spec compliance, Issue #1137).
 *
 * @option webassembly
 */

load('../js/assert.js');

const g32 = new WebAssembly.Global({ value: "i32", mutable: true }, 0);
const g64 = new WebAssembly.Global({ value: "i64", mutable: true }, 0n);

// 1. Control: WebAssembly.Global constructor propagates error from valueOf
assertThrows(() => new WebAssembly.Global({ value: "i32", mutable: true }, {
    valueOf() { throw new RangeError("from valueOf"); }
}), RangeError, "from valueOf");

// 2. Setting value when valueOf throws RangeError must propagate RangeError (not TypeError)
assertThrows(() => {
    g32.value = { valueOf() { throw new RangeError("from valueOf"); } };
}, RangeError, "from valueOf");

// 3. Setting value when @@toPrimitive throws URIError must propagate URIError
assertThrows(() => {
    g32.value = { [Symbol.toPrimitive]() { throw new URIError("from toPrimitive"); } };
}, URIError, "from toPrimitive");

// 4. Setting i64 value with invalid string must throw SyntaxError from ToBigInt64
assertThrows(() => {
    g64.value = "abc";
}, SyntaxError);

// 5. Normal valid values update correctly
g32.value = 42;
assertSame(42, g32.value);

g64.value = 100n;
assertSame(100n, g64.value);
