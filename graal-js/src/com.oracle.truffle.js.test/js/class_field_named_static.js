/*
 * Copyright (c) 2020, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Tests of class field named 'static'.
 * 
 * @option ecmascript-version=2021
 */

load('assert.js');

class C { static }

assertFalse(C.hasOwnProperty('static'));
assertTrue(new C().hasOwnProperty('static'));

class D {
    static
    static
    static
}

assertTrue(D.hasOwnProperty('static'));
assertTrue(new D().hasOwnProperty('static'));

true;
