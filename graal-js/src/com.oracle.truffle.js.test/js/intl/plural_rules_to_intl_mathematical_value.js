/*
 * Copyright (c) 2026, 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Checks that Intl.PluralRules.prototype.select[Range] uses ToIntlMathematicalValue.
 */

load('../assert.js');

assertSame('one', new Intl.PluralRules('en').select(1n));
assertSame('one', new Intl.PluralRules('ru').select('1000000000000000000000001'));
assertSame('other', new Intl.PluralRules('en').selectRange(1n, 2n));
