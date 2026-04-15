/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Regression test for Date.UTC processing arguments beyond its seven formal parameters.
 */

load("assert.js");

var log = [];
var result = Date.UTC(
                { valueOf() { log.push("y"); return 2000; } },
                { valueOf() { log.push("m"); return 0; } },
                { valueOf() { log.push("d"); return 1; } },
                { valueOf() { log.push("h"); return 0; } },
                { valueOf() { log.push("min"); return 0; } },
                { valueOf() { log.push("s"); return 0; } },
                { valueOf() { log.push("ms"); return 0; } },
                { valueOf() { log.push("extra"); throw new Error("boom"); } });

assertSame(946684800000, result);
assertSameContent(["y", "m", "d", "h", "min", "s", "ms"], log);

assertSame(946684800000, Date.UTC(2000, 0, 1, 0, 0, 0, 0, NaN));
