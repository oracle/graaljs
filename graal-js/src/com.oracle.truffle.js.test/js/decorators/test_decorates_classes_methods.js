/*
 * Copyright (c) 2022, 2022, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * @option ecmascript-version=staging
 */

load('../assert.js');

var ticks = 40;

function logged(value, context) {
    assertSame('method', context.kind);
    assertSame('m', context.name);
    assertSame(false, context.static);
    assertSame(false, context.private);
    const kind = context.kind;
    const name = context.name;
    if (kind === "method") {
        ticks++;
        return function (...args) {
            assertSame('m', name)
            assertSame(1, args.length)
            const ret = value.call(this, ...args);
            ticks++;
            return ret;
        };
    } else {
        throw 'wrong!';
    }
}

class C {
  @logged
  m(arg) {}
}

new C().m(1);
assertSame(42, ticks);
