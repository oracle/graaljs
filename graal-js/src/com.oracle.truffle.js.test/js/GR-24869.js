/*
 * Copyright (c) 2020, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

load('assert.js');

assertSame(true, Object.isSealed(Object.seal(new Proxy([42], {}))));
assertSame(true, Object.isFrozen(Object.freeze(new Proxy([42], {}))));

var array = [1];
assertSame(true, Object.isSealed(Object.seal(array)));
// Property re-definition is refused
assertThrows(function () {
    Object.defineProperty(array, '0', {
        configurable: true
    });
}, TypeError);
// Array should remain sealed
assertSame(true, Object.isSealed(array));

true;
