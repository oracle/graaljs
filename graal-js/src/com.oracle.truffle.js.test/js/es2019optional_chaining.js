/*
 * Copyright (c) 2020, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Optional chaining operator should not be available in ES 2019 mode.
 * 
 * @option ecmascript-version=2019
 */

load('assert.js');

assertThrows(function() {
    eval("Math?.PI");
}, SyntaxError);

true;
