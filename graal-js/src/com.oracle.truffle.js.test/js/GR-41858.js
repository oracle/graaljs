/*
 * Copyright (c) 2022, 2022, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Verify that a method/getter/setter can be replaced by a callable proxy
 * or a foreign executable (by a decorator).
 * 
 * @option ecmascript-version=staging
 */

load("assert.js");

function testDecorator(decorator, replacement, testName) {
    var C1 = class { @decorator static m() {}; }
    assertSame(replacement, C1.m);
    if (testName) {
        assertSame("m", C1.m.name);
    }

    var C2 = class { @decorator static get m() {}; }
    assertSame(replacement, Object.getOwnPropertyDescriptor(C2, 'm').get);
    if (testName) {
        assertSame("get m", replacement.name);
    }

    var C3 = class { @decorator static set m(x) {}; }
    assertSame(replacement, Object.getOwnPropertyDescriptor(C3, 'm').set);
    if (testName) {
        assertSame("set m", replacement.name);    
    }
}

var proxy = new Proxy(function() {}, {});
var d_proxy = function() { return proxy; };
testDecorator(d_proxy, proxy);

var foreign = java.lang.System.getProperties;
var d_foreign = function() { return foreign; };
testDecorator(d_foreign, foreign);
