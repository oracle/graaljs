/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Tests module imports with a mismatch between import attribute type and detected mime type.
 *
 * @option import-attributes=true
 * @option json-modules=true
 */

await import("./fixtures/dummy.json", {with: {type: "json"}});

try {
    await import("./fixtures/dummy.json");
    throw new Error(`should have thrown a TypeError`);
} catch (e) {
    if (!(e instanceof TypeError)) throw e;
}

await import("./import_call_assertion_error_imported.mjs");

try {
    await import("./import_call_assertion_error_imported.mjs", {with: {type: "json"}});
    throw new Error(`should have thrown a TypeError`);
} catch (e) {
    if (!(e instanceof TypeError)) throw e;
}
