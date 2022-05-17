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


//---------------------------------------------//
var ticks = 41;
function dec(v, c) {
    assertSame('function', typeof v);
    assertSame('class', c.kind);
    assertSame('C1', c.name);
    ticks++;
}

@dec
class C1 {}
assertSame(42, ticks);


//---------------------------------------------//
ticks = 40;
function logged(value, { kind, name }) {
    ticks++;
    if (kind === "class") {
        return class extends value {
            constructor(...args) {
                super(...args);
                assertSame('C2', name);
                assertSame(1, args.length);
                assertSame(42, args[0]);
                ticks++;
            }
        }
    }
}

@logged
class C2 {}
new C2(42)
assertSame(42, ticks);


//---------------------------------------------//
ticks = 41;
function log(value, context) {
    assertSame('class', context.kind);
    assertSame('C3', context.name);
    assertSame(undefined, context.static);
    assertSame(undefined, context.private);
    ticks++;
}

@log
class C3 {}
new C3(1);
assertSame(42, ticks);


//---------------------------------------------//
ticks = 41;

@(function foo() { ticks++; }) class C4 {}
assertSame(42, ticks);


//---------------------------------------------//
ticks = 41;
var proxy = new Proxy(function() { ticks++; }, {})
@proxy class C5 {}
assertSame(42, ticks);


//---------------------------------------------//
assertThrows(() => {
    (class { @(42) x });
}, TypeError);


//---------------------------------------------//
ticks = 41;
(class { @(new Proxy(function() { ticks++; }, {})) x });
assertSame(42, ticks);


//---------------------------------------------//
ticks = 41;

function f(value, context) {
    String(context.name)
    assertSame(context.name, '');
    ticks++;
}
(@f class {})
assertSame(42, ticks);
