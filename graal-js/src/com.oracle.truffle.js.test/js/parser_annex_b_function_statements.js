/*
 * Copyright (c) 2026, 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Labelled functions and functions in if statement clauses require Annex B extensions.
 *
 * @option annex-b=false
 */

load('assert.js');

for (const statement of [
    'label: function f() {}',
    'outer: inner: function f() {}',
    'if (true) function f() {}',
    'if (false) {} else function f() {}',
    'if (true) function f() {} else function g() {}'
]) {
    assertThrows(() => Function(statement), SyntaxError);
    assertThrows(() => Function('function outer() {' + statement + '}'), SyntaxError);
    assertThrows(() => Function('() => {' + statement + '}'), SyntaxError);
}

assertSame(42, Function('function f() { return 42; } return f();')());
assertSame(42, Function('{ function f() { return 42; } return f(); }')());
assertSame('undefined', Function('{ function f() {} } return typeof f;')());
