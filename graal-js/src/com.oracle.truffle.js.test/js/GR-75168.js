/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

load("assert.js");

(function dateConstructorCapturesTimeBeforeNewTargetPrototype() {
    var marker;
    var newTarget = new Proxy(function() {}, {
        get(target, key, receiver) {
            if (key === "prototype") {
                marker = Date.now();
                while (Date.now() - marker < 25) {
                }
                return Date.prototype;
            }
            return Reflect.get(target, key, receiver);
        }
    });

    var date = Reflect.construct(Date, [], newTarget);

    assertTrue(date.getTime() <= marker);
    assertSame(Date.prototype, Object.getPrototypeOf(date));
})();
