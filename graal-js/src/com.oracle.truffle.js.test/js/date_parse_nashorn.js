/*
 * Copyright (c) 2025, 2025, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/*
 * Test of the interpretation of parsed dates (in nashorn-compat mode).
 *
 * @option timezone=IST
 * @option nashorn-compat
 */

load('assert.js');
load('date_parse_common.js');

assertSame(dateOnlyUTC, zDateOnlyEcma.toString());
assertSame(dateOnlyUTC, zDateOnlyLegacy.toString());

assertSame(dateTimeUTC, zDateTimeEcma.toString());
assertSame(dateTimeUTC, zDateTimeLegacy.toString());

assertSame(dateOnlyUTC, dateOnlyEcma.toString());
assertSame(dateOnlyLocal, dateOnlyLegacy.toString());

assertSame(dateTimeUTC, dateTimeEcma.toString());
assertSame(dateTimeLocal, dateTimeLegacy.toString());
