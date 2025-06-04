/*
 * Copyright (c) 2024, 2025, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Regression test of the handling of SafeInteger by PluralRules.
 * 
 * @option locale=en
 */

load('assert.js');

assertSame('other', new Intl.PluralRules().select(2147483648));
