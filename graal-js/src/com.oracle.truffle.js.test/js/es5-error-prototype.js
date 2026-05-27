/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/*
 * @option ecmascript-version=5
 */

load("assert.js");

var errorPrototypes = [
    Error.prototype,
    EvalError.prototype,
    RangeError.prototype,
    ReferenceError.prototype,
    SyntaxError.prototype,
    TypeError.prototype,
    URIError.prototype
];

for (var i = 0; i < errorPrototypes.length; i++) {
    var prototype = errorPrototypes[i];
    assertSame("[object Error]", Object.prototype.toString.call(prototype));
    try {
        throw prototype;
    } catch (e) {
        assertSame(prototype, e);
    }
    assertThrows(function () {
        Number.prototype.valueOf.call(prototype);
    }, TypeError);
}
