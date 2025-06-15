/*
 * Copyright (c) 2025, 2025, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

load('../assert.js');

assertSame(-1, new Intl.Collator('en-u-kn-true').compare('2', '10'));
assertSame(1, new Intl.Collator('en-u-kn-false').compare('2', '10')); 
assertSame(-1, new Intl.Collator('en', { numeric: true }).compare('2', '10'));
assertSame(1, new Intl.Collator('en', { numeric: false }).compare('2', '10'));

assertSame(1, new Intl.Collator('en-u-kf-upper').compare('a', 'A'));
assertSame(-1, new Intl.Collator('en-u-kf-lower').compare('a', 'A'));
assertSame(1, new Intl.Collator('en', { caseFirst: 'upper' }).compare('a', 'A'));
assertSame(-1, new Intl.Collator('en', { caseFirst: 'lower' }).compare('a', 'A'));
