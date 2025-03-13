/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Iterator helpers should only close the receiver if it is an Object.
 *
 * @option iterator-helpers
 */

load("./assert.js")

let returnCalled = 0;
Number.prototype.return = function() { returnCalled++ };

for (const iteratorMethod of [
    Iterator.prototype.drop,
    Iterator.prototype.take,
]) {
    returnCalled = 0;
    assertThrows(() => iteratorMethod.call(42), TypeError, "not an Object");
    assertSame(0, returnCalled);
    assertThrows(() => iteratorMethod.call(new Number(42)), RangeError);
    assertSame(1, returnCalled);
}

for (const iteratorMethod of [
    Iterator.prototype.filter,
    Iterator.prototype.map,
    Iterator.prototype.flatMap,
    Iterator.prototype.reduce,
    Iterator.prototype.find,
    Iterator.prototype.some,
    Iterator.prototype.every,
    Iterator.prototype.forEach,
]) {
    returnCalled = 0;
    assertThrows(() => iteratorMethod.call(42, function() {}), TypeError, "not an Object");
    assertSame(0, returnCalled);
    assertThrows(() => iteratorMethod.call(42), TypeError, "not an Object");
    assertSame(0, returnCalled);
    assertThrows(() => iteratorMethod.call(new Number(42)), TypeError, "not a function");
    assertSame(1, returnCalled);
}
