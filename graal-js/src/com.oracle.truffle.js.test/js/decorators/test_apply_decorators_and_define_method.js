/*
 * Copyright (c) 2023, 2023, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * @option ecmascript-version=staging
 */

'use strict';

load('../assert.js')

function saveName(f) {
    f.initialName = f.name;
    return f;
}

let decorationOrder = [];

function decorate(inner, context) {
    decorationOrder.push(context.name);
    // decorators are called with undefined this
    assertSame(undefined, this);
    switch (context.kind) {
        case 'method':
            return saveName(function newMethod() {
                return inner() * 9;
            });
        case 'accessor':
            let {get: innerGet, set: innerSet} = inner;
            return {
                get: saveName(function newGetter() {
                    return innerGet.call(this) * 2;
                }),
                set: saveName(function newSetter(v) {
                    return innerSet.call(this, v * 3);
                }),
                init: function newInit(v) {
                    // ensure correct this value
                    assertSame('C', this.constructor.name);
                    // accessor methods are already defined at this point
                    assertTrue(context.name in this);
                    // get and set do not work yet since the accessor's private state is not initialized yet
                    assertThrows(() => innerGet.call(this), TypeError);
                    assertThrows(() => innerSet.call(this, 'whatever'), TypeError);
                    assertThrows(() => context.access.get.call(this), TypeError);
                    assertThrows(() => context.access.set.call(this, 'whatever'), TypeError);
                    return v * 4;
                }
            };
        case 'getter':
            return saveName(function newGetter() {
                return inner.call(this) * 6;
            });
        case 'setter':
            return saveName(function newSetter(v) {
                return inner.call(this, v * 8);
            });
        case 'field':
            return saveName(function newInit(v) {
                if (!(v.name === 'fx' || v.name === 'newValue')) {
                    throw new Error(v.name);
                }
                return saveName(function newValue() {
                    return v() * 6;
                });
            });
        default:
            throw new Error(context.kind);
    }
}

class C {
    @decorate @decorate accessor ax = 4;
    @decorate @decorate get gx() { return 5; }
    @decorate @decorate set sx(v) { this.sxv = v; }
    @decorate @decorate mx() { return 6; }
    @decorate @decorate fx = function() { return 9; };
}

let c = new C();
// Check new vs old function names
assertSame('get gx',    Object.getOwnPropertyDescriptor(C.prototype, 'gx').get.name)
assertSame('newGetter', Object.getOwnPropertyDescriptor(C.prototype, 'gx').get.initialName)
assertSame('set sx',    Object.getOwnPropertyDescriptor(C.prototype, 'sx').set.name)
assertSame('newSetter', Object.getOwnPropertyDescriptor(C.prototype, 'sx').set.initialName)
assertSame('mx',        Object.getOwnPropertyDescriptor(C.prototype, 'mx').value.name)
assertSame('newMethod', Object.getOwnPropertyDescriptor(C.prototype, 'mx').value.initialName)
// No SetFunctionName for auto-accessors (?)
assertSame('newGetter', Object.getOwnPropertyDescriptor(C.prototype, 'ax').get.name);
assertSame('newGetter', Object.getOwnPropertyDescriptor(C.prototype, 'ax').get.initialName)
assertSame('newSetter', Object.getOwnPropertyDescriptor(C.prototype, 'ax').set.name)
assertSame('newSetter', Object.getOwnPropertyDescriptor(C.prototype, 'ax').set.initialName)
// No SetFunctionName for field value
assertSame('newValue',  Object.getOwnPropertyDescriptor(c, 'fx').value.name)
assertSame('newValue',  Object.getOwnPropertyDescriptor(c, 'fx').value.initialName)

// Check property order
assertSameContent(['constructor', 'ax', 'gx', 'sx', 'mx'], Object.getOwnPropertyNames(C.prototype));
// Check decorator invocation order
assertSameContent(['ax', 'gx', 'sx', 'mx', 'fx'].flatMap(n => [n, n]), decorationOrder);

// Check values
assertSame(180, c.gx);   // 5 * 6 * 6
assertSame(486, c.mx()); // 6 * 9 * 9
assertSame(256, c.ax);   // 4 * 4 * 4 * 2 * 2
c.ax = 6;
assertSame(216, c.ax);   // 6 * 3 * 3 * 2 * 2
c.sx = 7;
assertSame(448, c.sxv);  // 7 * 8 * 8
assertSame(324, c.fx()); // 9 * 6 * 6
