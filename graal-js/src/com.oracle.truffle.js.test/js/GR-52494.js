/*
 * Copyright (c) 2024, 2024, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Regression test checking String.prototype.split in nashorn-compat mode
 *
 * @option nashorn-compat
 */

load("assert.js");

var array = "ab".split(/(?:ab)*/);

assertSameContent(['', ''], array);

