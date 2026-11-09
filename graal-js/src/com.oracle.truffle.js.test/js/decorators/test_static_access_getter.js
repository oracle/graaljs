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

function foo(value, context) {
    getter = context.access.get;
}

var C1 = class {
    @foo static #m() {};
    static ['#m'] = 42;
}

assertSame('function', typeof getter(C1));
assertThrows(() => {
    getter({});
}, TypeError);


//---------------------------------------------//
var C2 = class {
    @foo static m() {};
}

assertSame('function', typeof getter(C2));
assertSame(undefined, getter({}));


//---------------------------------------------//
var C3 = class {
    @foo #m() {};
    ['#m'] = 42;
}

assertSame('function', typeof getter(new C3()));
assertThrows(() => {
    getter({});
}, TypeError);


//---------------------------------------------//
var C4 = class {
    @foo static get #m() { return 42; };
    static ['#m'] = 211;
}
assertSame(42, getter(C4));

assertThrows(() => {
    getter({});
}, TypeError);


//---------------------------------------------//
var C5 = class {
    @foo static get m() { return 42; }
}

assertSame(42, getter(C5));
assertSame(undefined, getter({}));


//---------------------------------------------//
function f(value, context) { globalThis.context = context; }

class C6 { @f static [Symbol('foo')] }

assertThrows(() => context.access.get('foo'), TypeError);
assertThrows(() => context.access.set('foo'), TypeError);


//---------------------------------------------//
class C7 { @f static [Symbol('foo')] = 42 }
assertSame(42, context.access.get(C7));

class C8 { @f static [Symbol('foo')] = 42 }
assertSame(undefined, context.access.set(C8, 211));

assertSame(undefined, context.access.set({}));
assertSame(undefined, context.access.get({}));
