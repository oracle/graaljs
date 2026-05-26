/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * @option temporal=true
 */

load("assert.js");

var dtf = new Intl.DateTimeFormat("en-CA", {
    timeZone: "UTC",
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
    second: "2-digit",
    fractionalSecondDigits: 3,
    hourCycle: "h23",
});
var parts = Object.fromEntries(dtf.formatToParts(new Temporal.Instant(-1n)).filter(part => part.type !== "literal").map(part => [part.type, part.value]));

assertSame("1969", parts.year);
assertSame("12", parts.month);
assertSame("31", parts.day);
assertSame("23", parts.hour);
assertSame("59", parts.minute);
assertSame("59", parts.second);
assertSame("999", parts.fractionalSecond);
