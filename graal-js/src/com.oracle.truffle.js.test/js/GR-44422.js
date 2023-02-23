/*
 * Copyright (c) 2023, 2023, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Verifies that the option of Intl built-ins can be a foreign object.
 */

load("assert.js");

var foreignObject = new java.lang.Object();

assertSame(0, ''.localeCompare('', 'en', foreignObject));

// Should not throw
new Intl.Collator('en', foreignObject);
new Intl.NumberFormat('en', foreignObject);
new Intl.PluralRules('en', foreignObject);
new Intl.ListFormat('en', foreignObject);
new Intl.RelativeTimeFormat('en', foreignObject);
new Intl.Segmenter('en', foreignObject);
new Intl.Locale('en', foreignObject);

var foreignObjectWithType = new java.util.HashMap();
foreignObjectWithType.put('type', 'language');
new Intl.DisplayNames('en', foreignObjectWithType);

// Intl.DateTimeFormat constructor creates an object with the options object
// as the prototype, but we do not support foreign prototypes.
assertThrows(() => new Intl.DateTimeFormat('en', foreignObject), TypeError);
