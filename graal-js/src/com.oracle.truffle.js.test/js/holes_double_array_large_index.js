/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

load("./assert.js");

const array = [1.5, 2.5, 3.5];
delete array[1];

assertTrue(Reflect.set(array, "4294967294", 4.5));
assertSame(4294967295, array.length);
assertTrue(Object.hasOwn(array, "4294967294"));
assertSame(4.5, array[4294967294]);
