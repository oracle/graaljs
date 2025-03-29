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

assertSame("Deferred Module", defer[Symbol.toStringTag]);
assertSame(undefined, defer.then);
assertFalse(globalThis.evaluated);
assertSame(40, defer.forty);
assertTrue(globalThis.evaluated);

globalThis.evaluated = false;
import defer * as defer2 from "data:;charset=utf-8,globalThis.evaluated%20%3D%20true%3B";

assertSame("Deferred Module", defer2[Symbol.toStringTag]);
assertSame(undefined, defer2.then);
assertFalse(globalThis.evaluated);
assertSame(undefined, defer2.doesNotExist);
assertTrue(globalThis.evaluated);

globalThis.evaluated = false;
import defer * as defer3 from "data:;charset=utf-8,globalThis.evaluated%20%3D%20true%3B%0D%0Aexport%20let%20then%3B";

assertTrue(Symbol.toStringTag in defer3);
assertFalse('then' in defer3);
assertFalse(globalThis.evaluated);
assertFalse('doesNotExist' in defer3);
assertTrue(globalThis.evaluated);
