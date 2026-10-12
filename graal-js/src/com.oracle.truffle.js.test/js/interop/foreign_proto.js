/*
 * Copyright (c) 2026, 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

load("../assert.js");

let foreign = Polyglot.createForeignDynamicObject();

// Test the __proto__ getter and setter functions with foreign objects.
let {get, set} = Object.getOwnPropertyDescriptor(Object.prototype, '__proto__');
assertSame(Object.getPrototypeOf(foreign), get.call(foreign));
assertSame(undefined, set.call(foreign, {}));
assertSame(undefined, set.call({}, foreign));

assertTrue(Object.getPrototypeOf(foreign) instanceof Object);
assertTrue(foreign instanceof Object);
assertThrows(() => Object.setPrototypeOf(foreign, {}), TypeError);
assertThrows(() => Object.setPrototypeOf({}, foreign), TypeError);

assertSame(Object.getPrototypeOf(foreign), Reflect.getPrototypeOf(foreign));
assertThrows(() => Reflect.setPrototypeOf(foreign, {}), TypeError);
assertThrows(() => Reflect.setPrototypeOf({}, foreign), TypeError);
