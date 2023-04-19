/*
 * Copyright (c) 2023, 2023, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Checks that ES6+ features are refused in nashorn-compat mode
 * (with the default ES version) correctly.
 * 
 * @option nashorn-compat
 */

load("assert.js");

assertThrows(function() { eval("async function f() {}") }, SyntaxError);
assertThrows(function() { eval("function* f() {}") }, SyntaxError);
assertThrows(function() { eval("42n") }, SyntaxError);
