/*
 * Copyright (c) 2026, 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * An auto-accessor named static must not be reinterpreted as a static class element modifier.
 *
 * @option ecmascript-version=staging
 */

load('../assert.js');

for (const body of [
    'accessor static x;',
    'accessor static accessor x;',
    'accessor static {}'
]) {
    assertThrows(() => Function('return class {' + body + '}'), SyntaxError);
}

for (const separator of [';', '\n', '\r\n', '/*\n*/']) {
    const C = Function('return class { accessor static' + separator + 'x = 42; }')();
    const instance = new C();
    assertSame(undefined, instance.static);
    instance.static = 21;
    assertSame(21, instance.static);
    assertSame(42, instance.x);
    assertSame(false, Object.hasOwn(C, 'x'));
    assertSame(false, Object.hasOwn(C, 'static'));
    const descriptor = Object.getOwnPropertyDescriptor(C.prototype, 'static');
    assertSame('function', typeof descriptor.get);
    assertSame('function', typeof descriptor.set);
}

class C {
    accessor static = 21;
    static accessor x = 42;
}
assertSame(21, new C().static);
assertSame(42, C.x);
assertSame(false, Object.hasOwn(C.prototype, 'x'));
