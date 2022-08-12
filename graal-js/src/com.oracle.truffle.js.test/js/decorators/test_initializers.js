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

var ticks = 39;

function bound(value, { name, addInitializer }) {
    ticks++;
    addInitializer(function () {
        ticks++;
        this[name] = this[name].bind(this);
    });
}

class C {
    message = 'hello!';

    @bound
    m() {
        ticks++;
        assertSame('hello!', this.message);
    }
}

let { m } = new C();
m();

assertSame(42, ticks);


//---------------------------------------------//
ticks = 40;

function initializer() { ticks++ }
function f() { return initializer; }

var C1 = class { @f static x = 42 };
var C2 = class { @f static x };

assertSame(42, ticks);


//---------------------------------------------//
ticks = 40;
var proxyInitializer = new Proxy(function() { ticks++ }, {})

function f2() { ticks++; return proxyInitializer; }
var C3 = class { @f2 static x }

assertSame(42, ticks);


//---------------------------------------------//
ticks = 41;
var decorator = function(value, context) { context.addInitializer(initializer); };
// should call the initializer here:
class C4 { @decorator static x; y; }
assertSame(42, ticks);
// should not call the initializer here
new C4();
assertSame(42, ticks);


//---------------------------------------------//
ticks = 41;
// should not call the initializer
class C5 { @decorator x; y; }
assertSame(41, ticks);
// should call the initializer here
new C5();
assertSame(42, ticks);
