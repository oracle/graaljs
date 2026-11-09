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

let access;
let C;
function d(value, context) {
    access = context.access;
};

// Static class elements

// Field
C = class { @d static x = 42 };
assertSame('function', typeof access.get);
assertSame('function', typeof access.set);
assertSame(42, access.get(C));
assertSame(undefined, access.set(C, 211));
assertSame(211, access.get(C));
assertSame(211, C.x);

// Private field
C = class { @d static #x = 43; static getX() { return this.#x; } };
assertSame('function', typeof access.get);
assertSame('function', typeof access.set);
assertSame(43, access.get(C));
assertSame(undefined, access.set(C, 212));
assertSame(212, access.get(C));
assertSame(212, C.getX());

// Method
C = class { @d static x() {} };
assertSame('function', typeof access.get);
assertSame('undefined', typeof access.set);
assertSame(C.x, access.get(C));

// Private method
C = class { @d static #x() {}; static getX() { return this.#x; } };
assertSame('function', typeof access.get);
assertSame('undefined', typeof access.set);
assertSame(C.getX(), access.get(C));

// Auto-accessor
C = class { @d static accessor x = 42 };
assertSame('function', typeof access.get);
assertSame('function', typeof access.set);
assertSame(42, access.get(C));
assertSame(undefined, access.set(C, 211));
assertSame(211, access.get(C));
assertSame(211, C.x);

// Private auto-accessor
C = class { @d static accessor #x = 43; };
assertSame('function', typeof access.get);
assertSame('function', typeof access.set);
assertSame(43, access.get(C));
assertSame(undefined, access.set(C, 212));
assertSame(212, access.get(C));
assertFalse('#x' in C);
assertFalse(Object.getOwnPropertyNames(C).includes('#x'));
assertThrows(() => access.get({}), TypeError);
assertThrows(() => access.set({}, 212), TypeError);

// Getter
C = class { @d static get x() { return 42; } };
assertSame('function', typeof access.get);
assertSame('undefined', typeof access.set);
assertSame(42, access.get(C));

// Private getter
C = class { @d static get #x() { return 43; } };
assertSame('function', typeof access.get);
assertSame('undefined', typeof access.set);
assertSame(43, access.get(C));

// Setter
let newValue = undefined;
C = class { @d static set x(v) { newValue = v; } };
assertSame('undefined', typeof access.get);
assertSame('function', typeof access.set);
assertSame(undefined, access.set(C, 42));
assertSame(42, newValue);

// Private setter
newValue = undefined;
C = class { @d static set #x(v) { newValue = v; } };
assertSame('undefined', typeof access.get);
assertSame('function', typeof access.set);
assertSame(undefined, access.set(C, 42));
assertSame(42, newValue);

// Instance class elements
var c;

// Field
C = class { @d x = 42 };
c = new C();
assertSame('function', typeof access.get);
assertSame('function', typeof access.set);
assertSame(42, access.get(c));
assertSame(undefined, access.set(c, 211));
assertSame(211, access.get(c));
assertSame(211, c.x);

// Private field
C = class { @d #x = 43; getX() { return this.#x; } };
c = new C();
assertSame('function', typeof access.get);
assertSame('function', typeof access.set);
assertSame(43, access.get(c));
assertSame(undefined, access.set(c, 212));
assertSame(212, access.get(c));
assertSame(212, c.getX());

// Method
C = class { @d x() {} };
c = new C();
assertSame('function', typeof access.get);
assertSame('undefined', typeof access.set);
assertSame(c.x, access.get(c));

// Private method
C = class { @d #x() {}; getX() { return this.#x; } };
c = new C();
assertSame('function', typeof access.get);
assertSame('undefined', typeof access.set);
assertSame(c.getX(), access.get(c));

// Auto-accessor
C = class { @d accessor x = 42 };
c = new C();
assertSame('function', typeof access.get);
assertSame('function', typeof access.set);
assertSame(42, access.get(c));
assertSame(undefined, access.set(c, 211));
assertSame(211, access.get(c));
assertSame(211, c.x);

// Private auto-accessor
C = class { @d accessor #x = 43; };
c = new C();
assertSame('function', typeof access.get);
assertSame('function', typeof access.set);
assertSame(43, access.get(c));
assertSame(undefined, access.set(c, 212));
assertSame(212, access.get(c));
assertFalse('#x' in C);
assertFalse(Object.getOwnPropertyNames(C).includes('#x'));
assertThrows(() => access.get({}), TypeError);
assertThrows(() => access.set({}, 212), TypeError);

// Getter
C = class { @d get x() { return 42; } };
c = new C();
assertSame('function', typeof access.get);
assertSame('undefined', typeof access.set);
assertSame(42, access.get(c));

// Private getter
C = class { @d get #x() { return 43; } };
c = new C();
assertSame('function', typeof access.get);
assertSame('undefined', typeof access.set);
assertSame(43, access.get(c));

// Setter
newValue = undefined;
C = class { @d set x(v) { newValue = v; } };
c = new C();
assertSame('undefined', typeof access.get);
assertSame('function', typeof access.set);
assertSame(undefined, access.set(c, 42));
assertSame(42, newValue);

// Private setter
newValue = undefined;
C = class { @d set #x(v) { newValue = v; } };
c = new C();
assertSame('undefined', typeof access.get);
assertSame('function', typeof access.set);
assertSame(undefined, access.set(c, 42));
assertSame(42, newValue);
