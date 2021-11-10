/*
 * Copyright (c) 2021, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Tests recursive bound function and proxy cache specializations.
 *
 * @option js.debug-builtin=true
 */

function assertInstanceof(e, ErrorType) {
    if (!(e instanceof ErrorType)) {
        throw e;
    }
}

function fail(msg) {
    throw new Error(msg);
}

function countNodes(fn) {
    let count = 0;
    let srcattr = Debug.srcattr(fn);
    srcattr = srcattr.replace(/.*AST source attribution:/s, '');
    for (let match of srcattr.matchAll(/\((\w+)\)/g)) {
        count++;
    }
    return count;
}

function assertFunctionNodeCount(fn, min=5, max=200) {
    let count = countNodes(fn);
    if (!(min <= count && count <= max)) {
        fail(`Node count not between ${min} and ${max}: ${count}`);
    }
}

(function testProxyGet() {
    var handler = {};
    var p = new Proxy({}, handler);
    handler.__proto__ = p;
    try {
        p.property;
        fail("RangeError expected");
    } catch(e) {
        assertInstanceof(e, RangeError);
    }

    assertFunctionNodeCount(arguments.callee);
})();

(function testProxyApply() {
    var handler = {};
    var proxy = new Proxy(() => {}, handler);
    handler.apply = proxy;
    try {
        proxy();
        fail("RangeError expected");
    } catch(e) {
        assertInstanceof(e, RangeError);
    }

    assertFunctionNodeCount(arguments.callee);
})();

(function testBoundFunctionInstanceof() {
    var f = function() { return {}; }

    for (var i = 0; i < 500; ++i) {
        f = f.bind();
        Object.defineProperty(f, Symbol.hasInstance, {value: undefined, configurable: true});
    }

    ({}) instanceof f;

    assertFunctionNodeCount(arguments.callee);
})();

(function testBoundFunctionCall() {
    for (var j = 0; j < 5; j++) {
        var f = function() { return {}; }

        for (var i = 0; i < 1000; ++i) {
            f = f.bind();
        }

        f();
        new f();
        Reflect.construct(f, [], f);

        assertFunctionNodeCount(arguments.callee);
    }
})();
