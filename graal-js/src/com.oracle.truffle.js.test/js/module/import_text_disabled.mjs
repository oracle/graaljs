/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Tests that `with { type: 'text' }` requires the import-text option.
 *
 * @option import-attributes=true
 */

try {
    await import('./fixtures/import_text_FIXTURE.json', { with: { type: 'text' } });
    throw new Error('should have thrown a TypeError');
} catch (e) {
    if (!(e instanceof TypeError)) {
        throw e;
    }
}
