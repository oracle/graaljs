/*
 * Copyright (c) 2021, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

// Tests writing of hash/map entries. It used to throw TypeError
// in strict mode, see https://github.com/oracle/graaljs/issues/522

load('assert.js');

assertSame(42, (function() {
    var map = new java.util.HashMap();
    map.x = 42;
    return map.x;
})());

assertSame(43, (function() {
    var map = new java.util.HashMap();
    map['x'] = 43;
    return map['x'];
})());

assertSame(44, (function() {
    "use strict";
    var map = new java.util.HashMap();
    map.x = 44;
    return map.x;
})());

assertSame(45, (function() {
    "use strict";
    var map = new java.util.HashMap();
    map['x'] = 45;
    return map['x'];
})());
