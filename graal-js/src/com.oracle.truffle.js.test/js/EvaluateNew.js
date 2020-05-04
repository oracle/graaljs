/*
 * Copyright (c) 2020, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Tests of the evaluation of 'new' operator.
 * 
 * @option nashorn-compat
 */

load('assert.js');

(function missingClassArgumentsEvaluated() {
    var constructor = java.ThisClassDoesNotExist;

    assertThrows(function() {
        new constructor(constructor = Object);
    }, TypeError);

    assertSame(Object, constructor);
})();

(function jsAdapterArgumentsEvaluated() {
    var constructor = new JSAdapter({});

    try {
        new constructor(constructor = Object);
    } catch (e) {
        // GraalJS does not throw but Nashorn throws java.lang.invoke.WrongMethodTypeException: Parameter counts differ.
        // This test checks the evaluation of arguments which should be performed no matter if this kind of error is thrown or not
        // => this test does not care whether the error is thrown.
    }

    assertSame(Object, constructor);
})();

true;
