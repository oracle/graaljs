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
var tick = 37;

function dec1(v, c) {
    assertSame('undefined', typeof v);
    assertSame('field', c.kind)
    assertSame('foo', c.name)
    assertSame(false, c.static)
    tick++;
}

class C1 {
    @dec1
    foo = 42;
}
assertSame(38, tick);


//---------------------------------------------//
function dec2(v, c) {
    assertSame('undefined', typeof v);
    assertSame('field', c.kind)
    assertSame('foo', c.name)
    assertSame(true, c.static)
    tick++;
}

class C2 {
    @dec2
    static foo = 42;
}
assertSame(39, tick);


//---------------------------------------------//
function dec3(v, x) {
    assertSame(v, 'miao');
    assertSame(undefined, x);
    tick++
    return function (v, c) {
        assertSame('undefined', typeof v)
        assertSame('field', c.kind)
        assertSame('foo', c.name)
        tick++
    }
}

class C3 {
    @dec3('miao') foo = 42;
}
assertSame(41, tick);


//---------------------------------------------//
function dec4(value, context) {
    assertSame(false, context.private);
    tick++;
}
class C4 {
    @dec4 ['#']
}
assertSame(42, tick);
