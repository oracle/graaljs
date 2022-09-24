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
    ticks++;
    const kind = context.kind;
    const name = context.name;
    assertSame('field', context.kind);
    assertSame('x', context.name);
    assertSame(false, context.static);
    assertSame(false, context.private);
    if (kind === "field") {
        return function (initialValue) {
            assertSame(name, 'x');
            assertSame(42, initialValue);
            ticks++;
            return initialValue;
        };
    }
}

class C1 {
    @logged x = 42;
}
new C1();
assertSame(42, ticks);


//---------------------------------------------//
assertThrows(() => {

    function f(value, context) { set = context.access.set }
    var C = class { @f static x }
    Object.freeze(C)
    set.call(C, 42)

}, TypeError, 'Cannot assign to read only property');
