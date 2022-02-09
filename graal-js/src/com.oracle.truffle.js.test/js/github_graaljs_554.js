/*
 * Copyright (c) 2022, 2022, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

// Tests the redefinition of accessor property in an object literal
// as reported by https://github.com/oracle/graaljs/issues/554

load('assert.js');

assertSame(null, {set ['a'](x) {}, ['a']: null}.a);
assertSame(true, {get ['a']() {}, ['a']: true}.a);
assertSame(false, {set a(x) {}, ['a']: false}.a);
assertSame(42, {get a() {}, ['a']: 42}.a);

assertSame(null, {set ['a'](x) {}, a: null}.a);
assertSame(true, {get ['a']() {}, a: true}.a);
assertSame(false, {set a(x) {}, a: false}.a);
assertSame(42, {get a() {}, a: 42}.a);
