/*
 * Copyright (c) 2020, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Exponentiation operator should not be available in ES 5 mode.
 * 
 * @option ecmascript-version=5
 */

load('assert.js');

assertThrows(function() {
    eval("2**10");
}, SyntaxError);

assertThrows(function() {
    eval("var a = 2; a **= 10;");
}, SyntaxError);

true;
