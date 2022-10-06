/*
 * Copyright (c) 2022, 2022, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

// Checks that String.prototype.normalize refuses an empty string
// as reported by https://github.com/oracle/graaljs/issues/654

load('assert.js');

assertThrows(() => "abc".normalize(""), RangeError);
