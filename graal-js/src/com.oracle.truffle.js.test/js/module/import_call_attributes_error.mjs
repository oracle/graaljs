/*
 * Copyright (c) 2022, 2024, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Tests that import calls with invalid options/attributes throw a TypeError.
 *
 * @option import-attributes=true
 */

function shouldHaveThrown(err) {
    throw new Error(`should have thrown ${err}`);
}

const from = './import_call_assertion_error_imported.mjs';

try {
    // The second argument to import() must be an object
    await import(from, 42);
    shouldHaveThrown(TypeError);
} catch (e) {
    if (!(e instanceof TypeError)) throw e;
}

try {
    // The 'with' option must be an object
    await import(from, {with: 42});
    shouldHaveThrown(TypeError);
} catch (e) {
    if (!(e instanceof TypeError)) throw e;
}

try {
    // Import attribute value must be a string
    await import(from, {with: {attr: 42}});
    shouldHaveThrown(TypeError);
} catch (e) {
    if (!(e instanceof TypeError)) throw e;
}

// Valid
await import(from, {});
await import(from, {with: {}});
await import(from, {with: {attr: 'value'}});
