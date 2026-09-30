/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Verify that parameters without a corresponding actual argument are not mapped to the arguments
 * object.
 */

load("assert.js");

(function (a, b, c) {
    assertSame(undefined, c);
    c = 3;
    assertSame(3, c);
    assertSame(2, arguments.length);
    assertFalse(Object.hasOwn(arguments, 2));

    arguments[2] = 4;
    assertSame(3, c);
    assertSame(4, arguments[2]);
})(1, 2);

(function (a, b, c) {
    c = 3;
    Object.defineProperty(arguments, "0", {value: 10, writable: false});
    assertSame(10, a);

    a = 11;
    assertSame(11, a);
    assertSame(10, arguments[0]);
    assertSame(3, c);
})(1, 2);

var closure = (function (a, b, c) {
    return {
        get: function () {
            return c;
        },
        set: function (value) {
            c = value;
        },
        arguments: arguments
    };
})(1, 2);
assertSame(undefined, closure.get());
closure.set(3);
assertSame(3, closure.get());
assertFalse(Object.hasOwn(closure.arguments, 2));
closure.arguments[2] = 4;
assertSame(3, closure.get());

(function (a, b, c) {
    eval("c = 3");
    assertSame(3, c);
    assertFalse(Object.hasOwn(arguments, 2));
})(1, 2);

function createThenDelete(a, b, c) {
    arguments[2] = 5;
    delete arguments[2];
    return c;
}
assertSame(undefined, createThenDelete(1, 2));

function defineNonWritable(a, b, c) {
    Object.defineProperty(arguments, "2", {value: 5, writable: false, configurable: true});
    assertSame(5, Object.getOwnPropertyDescriptor(arguments, "2").value);
    return c;
}
assertSame(undefined, defineNonWritable(1, 2));

function createThenMakeNonWritable(a, b, c) {
    arguments[2] = 5;
    Object.defineProperty(arguments, "2", {writable: false});
    return c;
}
assertSame(undefined, createThenMakeNonWritable(1, 2));

(function (a) {
    delete arguments[0];
    assertSame(1, a);
    a = 2;
    assertSame(2, a);
    assertFalse(Object.hasOwn(arguments, 0));
})(1);
