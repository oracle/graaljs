/*
 * Copyright (c) 2020, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

'use strict';

load('assert.js');

var globalThisPrototype = Object.getPrototypeOf(globalThis);

Object.defineProperty(
    globalThisPrototype,
    "foo",
    {
        value: 42,
        writable: true,
        configurable: true
    }
);
foo = 211; // no error here
assertSame(42, globalThisPrototype.foo);
assertSame(211, globalThis.foo);
assertSame(211, foo);

var valueOfBar = 42;
Object.defineProperty(
    globalThisPrototype,
    "bar",
    {
        set: function(value) {
            valueOfBar = value;
        },
        get: function() {
            return valueOfBar;
        }
    }
);
bar = 211;
assertSame(211, globalThisPrototype.bar);
assertSame(211, globalThis.bar);
assertSame(211, bar);
assertSame(211, valueOfBar);

true;
