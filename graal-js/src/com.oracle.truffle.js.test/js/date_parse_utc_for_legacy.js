/*
 * Copyright (c) 2025, 2025, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/*
 * Test of the interpretation of parsed dates (with use-utc-for-legacy-dates=true).
 *
 * @option timezone=IST
 * @option use-utc-for-legacy-dates=true
 */

load('assert.js');
load('date_parse_common.js');

// use-utc-for-legacy-dates=true is the default mode
// so, it should be the same as date_parse.js

load('date_parse.js');
