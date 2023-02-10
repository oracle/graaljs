/*
 * Copyright (c) 2023, 2023, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

load("assert.js");

var array = [...[1,2,3,4]];
array.splice(1, 0, 5, 6);
assertSameContent([1,5,6,2,3,4], array);

array = [...[1,2,3,4],,];
array.splice(1, 0, 5, 6);
assertSameContent([1,5,6,2,3,4,,], array);
