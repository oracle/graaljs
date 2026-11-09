/*
 * Copyright (c) 2022, 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * @option ecmascript-version=staging
 */

load('../assert.js');

var ticks = 0;

function f() { ticks++; }

for (let i=0; i<10; i++) { (class { @f [i]() {} }); }
assertSame(10, ticks);


//---------------------------------------------//
for (let i=0; i<10; i++) { (class { @f [i] }) }
assertSame(20, ticks);


//---------------------------------------------//
for (let i=0; i<10; i++) { (class { @f get [i]() {} }) }
assertSame(30, ticks);


//---------------------------------------------//
for (let i=0; i<10; i++) { (class { @f set [i](value) {} }) }
assertSame(40, ticks);


//---------------------------------------------//
for (let i=0; i<10; i++) { (class { @f accessor [i] }) }
assertSame(50, ticks);


//---------------------------------------------//
function foo(value, context) {
    getter = context.access.get;
    ticks++;
}

for (let i=0; i<10; i++) {
    var C1 = class {
        @foo static #m() {};
        static ['#m'] = 42;
    }
    assertSame('function', typeof getter(C1));

    assertThrows(() => {
        getter({});
    }, TypeError);
}
assertSame(60, ticks);


//---------------------------------------------//
for (let i=0; i<10; i++) {
    var C2 = class {
        @foo #m() {};
        ['#m'] = 42;
    }
    assertSame('function', typeof getter(new C2()));

    assertThrows(() => {
        getter({});
    }, TypeError);
}
assertSame(70, ticks);
