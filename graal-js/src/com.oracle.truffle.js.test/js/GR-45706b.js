/*
 * Copyright (c) 2023, 2023, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Checks that bigInt syntax is refused in ES5 mode correctly.
 * 
 * @option ecmascript-version=5
 */

load("assert.js");

assertThrows(function() { eval("42n") }, SyntaxError);
