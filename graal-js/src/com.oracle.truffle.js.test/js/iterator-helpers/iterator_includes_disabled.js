/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Tests that Iterator.prototype.includes is not available before staging.
 *
 * @option ecmascript-version=2026
 */

load("../assert.js");

assertSame("undefined", typeof Iterator.prototype.includes);
