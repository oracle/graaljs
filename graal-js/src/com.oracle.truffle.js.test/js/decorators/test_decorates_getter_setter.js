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

var tick = 38;

function dec1(v, c) {
    assertSame('function', typeof v);
    assertSame('getter', c.kind)
    assertSame('foo', c.name)
    assertSame(false, c.static)
    tick++;
}

class A {
    @dec1
    get foo() { };
}


//---------------------------------------------//
function dec2(v, c) {
    assertSame('function', typeof v);
    assertSame('setter', c.kind)
    assertSame('foo', c.name)
    assertSame(false, c.static)
    tick++;
}

class B {
    @dec2
    set foo(v) { };
}

class C {
    @dec1
    get foo() { };

    @dec2
    set foo(v) { };
}

assertSame(42, tick);


//---------------------------------------------//
function replacement() {}

function f() { return replacement }

var D = class {
    @f static get x() {}
}

assertSame('get x', Object.getOwnPropertyDescriptor(D, 'x').get.name);


//---------------------------------------------//
var E = class {
    @f static set x(v) {}
}

assertSame('set x', Object.getOwnPropertyDescriptor(E, 'x').set.name);


//---------------------------------------------//
var names = [];
function foo(context) { names.push(context.name); return replacement };
var aSymbol = Symbol();

(class { @foo [aSymbol]() {} });
(class { @foo get [aSymbol]() {} });
(class { @foo set [aSymbol](x) {} });
assertSameContent(['', 'get ', 'set '], names);


//---------------------------------------------//
var leakGetDecorator = function(value, context) { leakGetter = context.access.get };

let C1 = class { @leakGetDecorator static x; }

delete C1.x;

assertSame(undefined, leakGetter.call(C1));
