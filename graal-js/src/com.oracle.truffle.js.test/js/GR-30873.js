/*
 * Copyright (c) 2021, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Tests of some corner cases of Intl.Collator with case sensitivity.
 * 
 * @option intl-402
 */

load('assert.js');

// inspired by failures of tests of locale-index-of (npm package)
assertSame(1, new Intl.Collator('de', {sensitivity: "case", usage: "search"}).compare('ä', 'a'));
assertSame(0, new Intl.Collator('de', {sensitivity: "case"}).compare('ä', 'a'));
