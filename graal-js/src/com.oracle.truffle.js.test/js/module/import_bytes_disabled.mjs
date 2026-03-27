/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Tests that `with { type: 'bytes' }` can be disabled using the import-bytes option.
 *
 * @option import-attributes=true
 * @option import-bytes=false
 */

try {
    await import('./fixtures/import_text_FIXTURE.json', { with: { type: 'bytes' } });
    throw new Error('should have thrown a TypeError');
} catch (e) {
    if (!(e instanceof TypeError)) {
        throw e;
    }
}
