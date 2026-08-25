/*
 * Copyright (c) 2026, 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Regression test of the handling of large raw lengths in String.raw
 * as reported at https://github.com/oracle/graaljs/issues/1104
 */

load("assert.js");

var raw = {length: Infinity};
Object.defineProperty(raw, "0", {
    get() {
        throw new Error("reached index 0");
    }
});

assertThrows(() => String.raw.call(0, {raw}), Error, "reached index 0");
