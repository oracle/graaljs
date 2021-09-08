/*
 * Copyright (c) 2021, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Tests of length property of bound functions.
 */

load('assert.js');

var fn = function() {};

Object.defineProperty(fn, "length", {value: Infinity});
assertSame(Infinity, fn.bind().length);
assertSame(Infinity, new Proxy(fn, {}).bind().length);

Object.defineProperty(fn, "length", {value: -Infinity});
assertSame(0, fn.bind().length);
assertSame(0, new Proxy(fn, {}).bind().length);

Object.defineProperty(fn, "length", {value: -(2**100)});
assertSame(0, fn.bind(undefined, undefined).length);
assertSame(0, new Proxy(fn, {}).bind(undefined, undefined).length);

Object.defineProperty(fn, "length", {value: -Infinity});
assertSame(0, fn.bind(undefined, undefined).length);
assertSame(0, new Proxy(fn, {}).bind(undefined, undefined).length);

Object.defineProperty(fn, "length", {value: -2147483647});
assertSame(0, fn.bind(undefined, undefined, undefined).length);
assertSame(0, new Proxy(fn, {}).bind(undefined, undefined, undefined).length);
