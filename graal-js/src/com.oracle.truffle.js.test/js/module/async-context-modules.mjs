/*
 * Copyright (c) 2026, 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Tests that module evaluation uses an empty AsyncContext mapping.
 *
 * @option async-context
 */

const variable = new AsyncContext.Variable({defaultValue: 'default'});
globalThis.asyncContextModuleVariable = variable;

try {
    const synchronousModule = await variable.run('importer', () => import('./fixtures/async-context-module-sync.mjs'));
    if (synchronousModule.value !== 'default') {
        throw new Error(`synchronous module mapping: ${synchronousModule.value}`);
    }

    const topLevelAwaitModule = await variable.run('importer', () => import('./fixtures/async-context-module-tla.mjs'));
    if (topLevelAwaitModule.value !== 'default') {
        throw new Error(`top-level-await module mapping: ${topLevelAwaitModule.value}`);
    }
} finally {
    delete globalThis.asyncContextModuleVariable;
}
