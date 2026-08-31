/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/* Tests that await-dictionary methods are not available before staging. */

load('./assert.js');

assertSame('undefined', typeof Promise.allKeyed);
assertSame('undefined', typeof Promise.allSettledKeyed);
