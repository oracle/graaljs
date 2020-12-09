/*
 * Copyright (c) 2020, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */
"use strict";

load("assert.js");

let fo1 = (...{x}) => x;
let fo2 = (x0, ...{x}) => { return x; };
let fo3 = (...{x = 42}) => x;
let fo4 = (x0, ...{x = 42}) => { return x; };
let fo5 = (...{length: x}) => x;
let fo6 = (x0, ...{length: x}) => { return x; };

let fa1 = (...[{x}]) => x;
let fa2 = (x0, ...[{x}]) => { return x; };
let fa3 = (...[x]) => x;
let fa4 = (x0, ...[x]) => { return x; };

assertSame(undefined, fo1());
assertSame(undefined, fo2());
assertSame(42, fo3());
assertSame(42, fo4());
assertSame(9, fo5(1,2,3,4,5,6,7,8,9));
assertSame(8, fo6(1,2,3,4,5,6,7,8,9));

assertSame(42, fa1({x: 42}));
assertSame(42, fa2(41, {x: 42}));
assertSame(42, fa3(42));
assertSame(42, fa4(41, 42, 43));
