/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

load("assert.js");

(function testConstructNumberCustomNewTargetOrder() {
    var log = [];
    var proto = {};
    var newTarget = new Proxy(function() {}, {
        get(target, key, receiver) {
            if (key === "prototype") {
                log.push("proto");
                return proto;
            }
            return Reflect.get(target, key, receiver);
        }
    });
    var value = {
        [Symbol.toPrimitive]() {
            log.push("value");
            return 42;
        }
    };

    var object = Reflect.construct(Number, [value], newTarget);

    assertSame("value,proto", log.join(","));
    assertSame(42, Number.prototype.valueOf.call(object));
    assertSame(proto, Object.getPrototypeOf(object));
})();

(function testConstructNumberCustomNewTargetErrorOrder() {
    var log = [];
    var newTarget = new Proxy(function() {}, {
        get(target, key, receiver) {
            if (key === "prototype") {
                log.push("proto");
                return {};
            }
            return Reflect.get(target, key, receiver);
        }
    });
    var value = {
        [Symbol.toPrimitive]() {
            log.push("value");
            throw new Error("test");
        }
    };

    assertThrows(function() {
        Reflect.construct(Number, [value], newTarget);
    }, Error, "test");
    assertSame("value", log.join(","));
})();
