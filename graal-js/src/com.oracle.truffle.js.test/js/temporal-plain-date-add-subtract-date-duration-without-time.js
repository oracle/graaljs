/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Tests the usage of ToDateDurationRecordWithoutTime() operation in Temporal.PlainDate.prototype.add/subtract.
 * 
 * @option temporal=true
 */

load('assert.js');

var date = new Temporal.PlainDate(2020, 1, 1);

assertSame('2020-02-02', date.add({months: 1, hours: 24}).toString());
assertSame('2020-02-03', date.add({months: 1, days: 1, hours: 24}).toString());
assertSame('2020-01-09', date.add({weeks: 1, hours: 24}).toString());
assertSame('2020-02-01', date.add({months: 1, hours: 23}).toString());

assertSame('2020-01-31', new Temporal.PlainDate(2020, 3, 1).subtract({months: 1, hours: 24}).toString());
