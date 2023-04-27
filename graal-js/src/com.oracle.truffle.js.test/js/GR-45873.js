/*
 * Copyright (c) 2023, 2023, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Tests of this in getters of foreign object prototype (invoked via a foreign object).
 */

load("assert.js");

let foreignObject = new java.lang.Object();
let fop = Object.getPrototypeOf(foreignObject);
let symbol = Symbol('bar');

Object.defineProperty(fop, 'foo', {
    get() {
        return this;
    }
});
Object.defineProperty(fop, symbol, {
    get() {
        return this;
    }
});

assertSame(foreignObject, foreignObject.foo);
assertSame(foreignObject, foreignObject['foo']);
assertSame(foreignObject, foreignObject[symbol]);

let receiver = {};
assertSame(foreignObject, Reflect.get(foreignObject, 'foo'));
assertSame(foreignObject, Reflect.get(foreignObject, symbol));
assertSame(receiver, Reflect.get(foreignObject, 'foo', receiver));
assertSame(receiver, Reflect.get(foreignObject, symbol, receiver));

let point = new java.awt.Point(42,211);

Object.defineProperty(fop, 'valueOf', {
    get() {
        if ('y' in this) {
            return (function() { return this.y*this.y });
        } else {
            return Object.getPrototypeOf(fop).valueOf;
        }
    }
});
assertSame(44521, point.valueOf());
assertSame(44521, Number(point));

Object.defineProperty(fop, Symbol.toPrimitive, {
    get() {
        if ('x' in this) {
            return (function() { return this.x });
        } else {
            return Object.getPrototypeOf(fop)[Symbol.toPrimitive];
        }
    }
});
assertSame('42', String(point));
