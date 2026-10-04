/*
 * Copyright (c) 2026, 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.
 */

load("assert.js");

var count = 0;
var proxy = new Proxy(function() {}, {
    getPrototypeOf(target) {
        count++;
        return Reflect.getPrototypeOf(target);
    }
});

assertFalse(proxy instanceof Array);
assertSame(1, count);
