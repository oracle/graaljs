/*
 * Copyright (c) 2021, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Tests the formatting/currency digits of historial currencies in JDK.
 *
 * @option intl-402
 */

load('assert.js');


function checkCurrency(currency) {
    // Inspired by currency-digits.js test from Test262 test-suite.
    var options = Intl.NumberFormat([], {style: "currency", currency: currency}).resolvedOptions();
    assertSame(2, options.minimumFractionDigits);
    assertSame(2, options.maximumFractionDigits);
}

// Check currency digits of historial currencies in JDK
["ADP", "BEF", "BYB", "BYR", "ESP", "GRD", "ITL", "LUF", "MGF", "PTE", "ROL", "TPE", "TRL"].forEach(checkCurrency);

// Sanity check of the formatting of the currency from the original bug report.
var result = Math.PI.toLocaleString('ro-RO', { style: 'currency', currency: 'ROL' });
assertSame('3,14 ROL', result);
