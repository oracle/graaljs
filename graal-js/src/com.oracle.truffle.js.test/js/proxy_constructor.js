/*
 * Copyright (c) 2026, 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.
 */

load("assert.js");

(function proxyConstructorDoesNotGetNewTargetPrototype() {
    var reads = 0;
    var newTarget = new Proxy(function() {}, {
        get(target, key, receiver) {
            if (key === "prototype") {
                reads++;
                throw 42;
            }
            return Reflect.get(target, key, receiver);
        }
    });

    var result = Reflect.construct(Proxy, [{}, {}], newTarget);

    assertSame(0, reads);
    assertSame(Object.prototype, Object.getPrototypeOf(result));
})();
