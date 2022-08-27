/*
 * Copyright (c) 2022, 2022, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

// Test of Date.prototype.toJSON applied on a foreign instant
// as reported by https://github.com/oracle/graaljs/issues/632

load('assert.js');

const timestamp = java.sql.Timestamp.valueOf('2022-08-13 19:57:00');

const isoString = timestamp.toISOString();
const toJSON = timestamp.toJSON();
assertSame(isoString, toJSON);

assertSame('"' + isoString + '"', JSON.stringify(timestamp));
