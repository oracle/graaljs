/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Tests that source phase imports can be disabled explicitly.
 *
 * @option webassembly=true
 * @option source-phase-imports=false
 * @option unhandled-rejections=throw
 */

try {
    await import("./source-phase-import.mjs");
    throw new Error("should have thrown a SyntaxError");
} catch (e) {
    if (!(e instanceof SyntaxError)) {
        throw e;
    }
}
try {
    await import("./source-phase-import-dynamic.mjs");
    throw new Error("should have thrown a SyntaxError");
} catch (e) {
    if (!(e instanceof SyntaxError)) {
        throw e;
    }
}
