/*
 * Copyright (c) 2020, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

load("../assert.js");

assertSame(42, loadWithNewGlobal("loadWithNewGlobal_helper.js", 40, 2));
assertSame(42, loadWithNewGlobal({
    name: "testFn",
    script: "arguments[0] + arguments[1];"
}, 39, 3));
true;
