/*
 * Copyright (c) 2022, 2022, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * @option intl-402
 */

// Ensure that no internal error is thrown

["en", "ja"].forEach(function(locale) {
    // minimal test-case
    var df = new Intl.DateTimeFormat(locale, { year: 'numeric', hour: 'numeric', calendar: 'chinese' });
    df.formatRange(1000000000,2000000000);

    // realistic test-case
    df = new Intl.DateTimeFormat(locale, {
        year: 'numeric',
        month: 'numeric',
        day: 'numeric',
        hour: 'numeric',
        minute: 'numeric',
        calendar: 'chinese'
    });
    df.formatRange(1000000000,2000000000); 
});
