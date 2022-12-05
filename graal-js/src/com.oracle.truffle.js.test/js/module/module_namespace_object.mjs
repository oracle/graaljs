/*
 * Copyright (c) 2022, 2022, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

load('../assert.js');

import * as ns from './module_namespace_object.mjs';

assertThrows(() => ns.answer, ReferenceError);

assertThrows(() => Object.seal(ns), ReferenceError);
assertThrows(() => Object.freeze(ns), ReferenceError);
assertThrows(() => Object.isSealed(ns), ReferenceError);
assertThrows(() => Object.isFrozen(ns), ReferenceError);

export const answer = 42;

assertSame(42, ns.answer);

Object.seal(ns);
assertThrows(() => Object.freeze(ns), TypeError);
assertTrue(Object.isSealed(ns));
assertFalse(Object.isFrozen(ns));

let o = Object.create(ns);
assertTrue('answer' in o);
assertSame(42, o.answer);
assertSame('Module', o[Symbol.toStringTag]);
