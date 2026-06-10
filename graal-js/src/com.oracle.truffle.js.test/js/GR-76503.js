/*
 * Copyright (c) 2026, 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

load("assert.js");

var space = new String(" ");
space.toString = function() {
    throw new Error("space toString");
};

assertThrows(() => JSON.stringify("x", undefined, space), Error, "space toString");
