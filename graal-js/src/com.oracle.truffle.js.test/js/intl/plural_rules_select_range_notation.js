/*
 * Copyright (c) 2025, 2025, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Checks that Intl.PluralRules.prototype.selectRange respects the value of notation option.
 */

load('../assert.js');

assertSame('one', new Intl.PluralRules('ru', { notation: 'standard' }).selectRange(1, 1000001));
assertSame('many', new Intl.PluralRules('ru', { notation: 'compact' }).selectRange(1, 1000001));
