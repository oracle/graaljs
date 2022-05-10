/*
 * Copyright (c) 2022, 2022, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

load("assert.js");

// +000000 is valid
assertSame(Date.parse("0000-01-01T00:00:00Z"), Date.parse("+000000-01-01T00:00:00Z"));

// -000000 is invalid
assertTrue(isNaN(Date.parse("-000000-01-01T00:00:00Z")));

// 000000 is invalid (missing sign)
assertTrue(isNaN(Date.parse("000000-01-01T00:00:00Z")));
