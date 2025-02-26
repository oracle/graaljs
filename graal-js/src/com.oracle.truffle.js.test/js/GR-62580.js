/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Regression test of the parsing of milliseconds in dates.
 */

load("./assert.js");

["1979-04-25T01:01:01", "1979-04-25 01:01:01"].forEach(function(baseString) {
    var baseTime = new Date(baseString).getTime();
    assertSame(baseTime + 100, new Date(baseString + ".1").getTime());
    assertSame(baseTime + 20, new Date(baseString + ".02").getTime());
    assertSame(baseTime + 3, new Date(baseString + ".003").getTime());
});
