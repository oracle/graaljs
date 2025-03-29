/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Tests deferred module namespace object prototype property lookup.
 *
 * @option ecmascript-version=staging
 */

load("../assert.js");

globalThis.evaluated = false;
import defer * as defer from "./fixtures/deferred_module.mjs";
const obj1 = {__proto__: defer, two: 2};

assertSame(2, obj1.two);
assertSame("Deferred Module", obj1[Symbol.toStringTag]);
assertSame(undefined, obj1.then);
assertFalse(globalThis.evaluated);
assertSame(40, obj1.forty);
assertTrue(globalThis.evaluated);

globalThis.evaluated = false;
import defer * as defer2 from "data:;charset=utf-8,globalThis.evaluated%20%3D%20true%3B";
const obj2 = {__proto__: defer2, four: 4};

assertSame(4, obj2.four);
assertSame("Deferred Module", obj2[Symbol.toStringTag]);
assertSame(undefined, obj2.then);
assertFalse(globalThis.evaluated);
assertSame(undefined, obj2.doesNotExist);
assertTrue(globalThis.evaluated);

globalThis.evaluated = false;
import defer * as defer3 from "data:;charset=utf-8,globalThis.evaluated%20%3D%20true%3B%0D%0Aexport%20let%20then%3B";
const obj3 = {__proto__: defer3, six: 6};

assertSame(6, obj3.six);
assertTrue(Symbol.toStringTag in obj3);
assertFalse('then' in obj3);
assertFalse(globalThis.evaluated);
assertFalse('doesNotExist' in obj3);
assertTrue(globalThis.evaluated);
