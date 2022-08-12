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

function foo(value, context) {
    getter = context.access.get;
}

var C1 = class {
    @foo static #m() {};
    static ['#m'] = 42;
}

assertSame('function', typeof getter.call(C1));
assertThrows(() => {
    getter.call({});
}, TypeError);


//---------------------------------------------//
var C2 = class {
    @foo static m() {};
}

assertSame('function', typeof getter.call(C2));
assertSame(undefined, getter.call({}));


//---------------------------------------------//
var C3 = class {
    @foo #m() {};
    ['#m'] = 42;
}

assertSame('function', typeof getter.call(C3));
assertThrows(() => {
    getter.call({});
}, TypeError);


//---------------------------------------------//
var C4 = class {
    @foo static get #m() { return 42; };
    static ['#m'] = 211;
}
assertSame(42, getter.call(C4));

assertThrows(() => {
    getter.call({});
}, TypeError);


//---------------------------------------------//
var C5 = class {
    @foo static get m() { return 42; }
}

assertSame(42, getter.call(C5));
assertSame(undefined, getter.call({}));


//---------------------------------------------//
function f(value, context) { globalThis.context = context; }

class C6 { @f static [Symbol('foo')] }

assertSame(undefined, context.access.get.call('foo'));
assertSame(undefined, context.access.set.call('foo'));


//---------------------------------------------//
class C7 { @f static [Symbol('foo')] = 42 }
assertSame(42, context.access.get.call(C7));

class C8 { @f static [Symbol('foo')] = 42 }
assertSame(211, context.access.set.call(C8, 211));

assertSame(undefined, context.access.set.call({}));
assertSame(undefined, context.access.get.call({}));
