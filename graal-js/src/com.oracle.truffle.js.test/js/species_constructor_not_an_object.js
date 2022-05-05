/*
 * Copyright (c) 2022, 2022, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/*
 * SpeciesConstructor (O, defaultConstructor):
 * Let C be ? Get(O, "constructor").
 * If C is undefined, return defaultConstructor.
 * If Type(C) is not Object, throw a TypeError exception.
 */

try {
    let a = new Int8Array(8);
    a.constructor = null;
    a.subarray(4); // uses SpeciesConstructor
    throw new Error("should have thrown a TypeError");
} catch (e) {
    if (!(e instanceof TypeError
        && !e.message.includes("Cannot read") // Cannot read property 'Symbol(Symbol.species)' of null
        && e.message.includes("is not a") // constructor property / null is not an object
    )) {
        throw e;
    }
}

/*
 * ArraySpeciesCreate (originalArray, length):
 * Let C be ? Get(originalArray, "constructor").
 * If IsConstructor(C) is true [...]
 * If Type(C) is Object [...]
 * If C is undefined [...]
 * If IsConstructor(C) is false, throw a TypeError exception.
 */

try {
    let a = new Array(8);
    a.constructor = null;
    a.filter(x => x); // uses ArraySpeciesCreate
    throw new Error("should have thrown a TypeError");
} catch (e) {
    if (!(e instanceof TypeError
        && !e.message.includes("Cannot read") // Cannot read property 'Symbol(Symbol.species)' of null
        && e.message.includes("is not a") // constructor property / null is not a constructor
    )) {
        throw e;
    }
}
