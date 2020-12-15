/*
 * Copyright (c) 2020, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * This test is an extension of
 * https://github.com/tc39/test262/blob/main/test/built-ins/Date/parse/without-utc-offset.js
 * This extension checks also the desired behaviour for legacy dates
 * (that are not covered by the specification).
 */

load('assert.js');

const timezoneOffsetMS = new Date(0).getTimezoneOffset() * 60000;

function check(string, expected) {
    assertSame(expected, Date.parse(string));
    assertSame(expected, new Date(string).getTime());
}

check('1970-01-01T00:00:00', timezoneOffsetMS);
check('1970-01-01', 0);
check('1970-01', 0);
check('1970', 0);

// Legacy format
check('1-1-1970', 0);
check('1-1-1970 00:00:00', timezoneOffsetMS);
check('1/1/1970', 0);
check('1/1/1970 00:00:00', timezoneOffsetMS);
check('1970-01-1', 0);
check('1970-1-01', 0);
check('1970-1-1', 0);
check('1970-1', 0);

true;
