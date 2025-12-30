/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * @option temporal=true
 */

load('assert.js');

var instant = '2001-09-09T01:46:40Z';
var rounded = Temporal.Instant.from(instant).round({roundingIncrement: 25, roundingMode: 'ceil', smallestUnit: 'microsecond'}).toString();
assertSame(instant, rounded);
