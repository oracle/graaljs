/*
 * Copyright (c) 2021, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Regression test inspired by the pattern used in observable-variable (npm package).
 */

[42, 3.14, {}, new java.lang.Object()].forEach(function (element) {
    var array = [];
    array[0] = element;
    array.length = 10;
    array.splice(9, 1); // Used to throw ArrayIndexOutOfBoundsException
});
