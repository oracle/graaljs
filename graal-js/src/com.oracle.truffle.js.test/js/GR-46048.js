/*
 * Copyright (c) 2023, 2023, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Regression test for an internal error in Array.prototype.shift/splice.
 */

load("assert.js");

var array = new Array();
array.push("a", "b", "c");
array.shift();
array.shift();
array.splice(0, 0, "d", "e");
assertSameContent(["d", "e", "c"], array);
