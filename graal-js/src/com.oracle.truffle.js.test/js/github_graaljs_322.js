/*
 * Copyright (c) 2020, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

// Tests for the fixes of ++ and -- operators motivated by https://github.com/graalvm/graaljs/issues/322

load('assert.js');

assertTrue(isNaN(++undefined));
assertTrue(isNaN(--undefined));
assertTrue(isNaN(undefined++));
assertTrue(isNaN(undefined--));

true;
