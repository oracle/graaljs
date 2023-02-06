/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Symbols as WeakMap keys proposal.
 *
 * @option ecmascript-version=staging
 */


load('assert.js');
//Testing valid symbol as key
const weak = new WeakMap();


const key = Symbol('my ref');
const obj = {};

weak.set(key, obj);

assertSame(obj, weak.get(key));

//Testing for invalid, registered symbol
const key2 = Symbol.for('name');

assertThrows(() => weak.set(key2, obj), TypeError);


