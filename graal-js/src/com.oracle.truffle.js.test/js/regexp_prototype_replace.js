/*
 * Copyright (c) 2022, 2022, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Test RegExp.prototype.[@@replace] argument conversion order.
 */

load("assert.js");

const re = /asdf/;
assertSame("asdfasdf", re[Symbol.replace]("asdf", (x) => x + x));

const sideEffects = [];
assertSame("qwer", re[Symbol.replace](
    {toString() { sideEffects.push('searchString'); return "asdf"; }},
    {toString() { sideEffects.push('replaceValue'); return "qwer"; }}));
assertSame(2, sideEffects.length);
assertSame('searchString', sideEffects[0]);
assertSame('replaceValue', sideEffects[1]);
