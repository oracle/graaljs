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

var ticks = 38;

function logged(value, context) {
    ticks++;
    const name = context.name;
    const kind = context.kind;
     assertSame('accessor', context.kind);
     assertSame('x', context.name);
     assertSame(false, context.static);
     assertSame(false, context.private);
     assertSame('object', typeof context.access);
     assertSame('function', typeof context.access.set);
     assertSame('function', typeof context.access.get);
    
    if (kind === "accessor") {
        let { get, set } = value;
        return {
            get() {
                ticks++;
                assertSame('x', name);
                return get.call(this);
            },
            set(val) {
                ticks++;
                assertSame('x', name);
                assertSame(123, val);
                return set.call(this, val);
            },
            init(initialValue) {
                ticks++;
                assertSame('x', name);
                assertSame(1, initialValue);
                return initialValue;
            }
        };
    }
}

class C {
    @logged accessor x = 1;
}

let c = new C();
assertSame(40, ticks);
c.x;
assertSame(41, ticks);
c.x = 123;
assertSame(42, ticks);


//---------------------------------------------//
assertThrows(() => {

    function f() { return 42 }
    var C = class { @f static accessor x }

}, TypeError, "Class decorator must return undefined or function");


//---------------------------------------------//
assertThrows(() => {

    function f() { return null }
    var C = class { @f static accessor x }

}, TypeError, "Class decorator must return undefined or function");


//---------------------------------------------//
ticks = 41;
var proxy = new Proxy(function() { }, {})
function f() { ticks++; return { get: proxy, set: proxy } }

var C1 = class { @f static accessor x; }
assertSame(42, ticks);


//---------------------------------------------//
function globalLeak(value, context) { ticks++; globalThis.context = context; }
var C2 = (class { @globalLeak static accessor #x = 42 })
context.access.get.call(C2);
context.access.set.call(C2, 211);
assertSame(43, ticks);


//---------------------------------------------//
var C3 = class { static accessor x }
Object.getOwnPropertyDescriptor(C, 'x');
