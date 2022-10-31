/*
 * Copyright (c) 2022, 2022, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

// Checks the handling of an array literal with a spread and a hole
// as reported by https://github.com/oracle/graaljs/issues/668

load('assert.js');

var array = [ ...[] , , 42 ];
var keys = Reflect.ownKeys(array);
assertSameContent(['1', 'length'], keys);
