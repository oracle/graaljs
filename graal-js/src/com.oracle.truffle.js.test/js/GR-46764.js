/*
 * Copyright (c) 2023, 2023, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

load("assert.js");

assertSame(0, 0 ?? (1 || 2));
assertSame(1, undefined ?? (1 || 2));
assertSame(1, null ?? (1 || 2));

assertSame(0, 0 ?? ((1) || 2));
assertSame(1, undefined ?? ((1) || 2));
assertSame(1, null ?? ((1) || 2));
