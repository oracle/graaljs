/*
 * Copyright (c) 2022, 2022, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * @option ecmascript-version=staging
 */

load('../assert.js')

var ticks = 40;

function logged(value, context) {
    ticks++;
    assertSame('setter', context.kind)
    assertSame('x', context.name)
    assertSame(false, context.static)
    assertSame(false, context.private)
    const name = context.name;
    if (context.kind === "setter") {
        return function (...args) {
            assertSame('x', name);
            assertSame(1, args.length);
            assertSame(42, args[0]);
            ticks++;
            return value.call(this, ...args);
        };
    }
}

class C {
    @logged
    set x(arg) {}
}

new C().x = 42;
assertSame(42, ticks);
