/*
 * Copyright (c) 2025, 2025, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/*
 * Common definitions for tests of the interpretation of parsed dates.
 * 
 * @option timezone IST
 */

var zDateOnlyEcma = new Date("2023-09-28Z");
var zDateOnlyLegacy = new Date("2023 09 28Z");

var zDateTimeEcma = new Date("2023-09-28T00:31:25Z");
var zDateTimeLegacy = new Date("2023 09 28 00:31:25Z");

var dateOnlyEcma = new Date("2023-09-28");
var dateOnlyLegacy = new Date("2023 09 28");

var dateTimeEcma = new Date("2023-09-28T00:31:25");
var dateTimeLegacy = new Date("2023 09 28 00:31:25");

// Possible results
var dateOnlyUTC = "Thu Sep 28 2023 05:30:00 GMT+0530 (IST)";
var dateTimeUTC = "Thu Sep 28 2023 06:01:25 GMT+0530 (IST)";
var dateOnlyLocal = "Thu Sep 28 2023 00:00:00 GMT+0530 (IST)";
var dateTimeLocal = "Thu Sep 28 2023 00:31:25 GMT+0530 (IST)";
