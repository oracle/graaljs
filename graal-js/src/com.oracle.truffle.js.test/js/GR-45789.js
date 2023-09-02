/*
 * Copyright (c) 2023, 2023, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Check that we not run into an internal error
 * in some Promise-related corner cases.
 */

load("assert.js");

let objectToReturn;
class MyPromise extends Promise {
    constructor(executor) {
        if (objectToReturn) {
            let dummyResolve = () => {};
            let dummyReject = () => {};
            executor(dummyResolve, dummyReject);
            return objectToReturn;
        }
        super(executor);
    }
}

// Foreign objects should not be returned by promise constructors
let promise = MyPromise.resolve();
objectToReturn = new java.lang.Object();
assertThrows(() => promise.then(), TypeError);

// Custom object returned from promise constructor should not break
// the capturing of a stack-trace
objectToReturn = undefined;
promise = MyPromise.resolve();
objectToReturn = {};
promise.then(() => new Error());
