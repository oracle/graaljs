/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Tests that `with { type: 'text' }` returns source text, does not parse the imported file as
 * JavaScript, and exposes only a default export, for .json, .js, .mjs, .wasm, and .wat sources.
 *
 * @option import-attributes=true
 * @option import-text=true
 */

import json from './fixtures/import_text_FIXTURE.json' with { type: 'text' };
import * as jsonNamespace from './fixtures/import_text_FIXTURE.json' with { type: 'text' };
import js from './fixtures/import_text_FIXTURE.js' with { type: 'text' };
import * as jsNamespace from './fixtures/import_text_FIXTURE.js' with { type: 'text' };
import mjs from './fixtures/import_text_FIXTURE.mjs' with { type: 'text' };
import * as mjsNamespace from './fixtures/import_text_FIXTURE.mjs' with { type: 'text' };
import wasm from './fixtures/import_text_FIXTURE.wasm' with { type: 'text' };
import * as wasmNamespace from './fixtures/import_text_FIXTURE.wasm' with { type: 'text' };
import wat from './fixtures/import_text_FIXTURE.wat' with { type: 'text' };
import * as watNamespace from './fixtures/import_text_FIXTURE.wat' with { type: 'text' };

function stripLicenseHeader(text) {
    if (text.startsWith('/*')) {
        const end = text.indexOf('*/');
        if (end >= 0) {
            return text.slice(end + 2).trim();
        }
    }
    if (text.startsWith(';;')) {
        let start = 0;
        while (text.startsWith(';;', start)) {
            const lineEnd = text.indexOf('\n', start);
            if (lineEnd < 0) {
                break;
            }
            start = lineEnd + 1;
        }
        return text.slice(start).trim();
    }
    return text;
}

function assertTextValue(label, actual, expected) {
    const stripped = stripLicenseHeader(actual);
    if (stripped !== expected) {
        throw new Error(`unexpected ${label}: ${JSON.stringify(stripped)}`);
    }
}

function assertTextNamespace(label, namespace, expected) {
    const keys = Object.getOwnPropertyNames(namespace).join(',');
    if (keys !== 'default') {
        throw new Error(`unexpected ${label} namespace keys: ${keys}`);
    }
    assertTextValue(`${label} namespace default export`, namespace.default, expected);
}

function assertStaticTextModule(label, value, namespace, expected) {
    assertTextValue(`${label} import`, value, expected);
    assertTextNamespace(label, namespace, expected);
}

async function assertDynamicTextModule(label, specifier, expected) {
    const namespace = await import(specifier, { with: { type: 'text' } });
    assertTextNamespace(`${label} import()`, namespace, expected);
}

assertStaticTextModule('.json', json, jsonNamespace, 'a string value');
assertStaticTextModule('.js', js, jsNamespace, 'invalid { javascript');
assertStaticTextModule('.mjs', mjs, mjsNamespace, 'invalid { javascript module');
assertStaticTextModule('.wasm', wasm, wasmNamespace, 'wasm string value');
assertStaticTextModule('.wat', wat, watNamespace, 'wat string value');

await assertDynamicTextModule('.json', './fixtures/import_text_FIXTURE.json', 'a string value');
await assertDynamicTextModule('.js', './fixtures/import_text_FIXTURE.js', 'invalid { javascript');
await assertDynamicTextModule('.mjs', './fixtures/import_text_FIXTURE.mjs', 'invalid { javascript module');
await assertDynamicTextModule('.wasm', './fixtures/import_text_FIXTURE.wasm', 'wasm string value');
await assertDynamicTextModule('.wat', './fixtures/import_text_FIXTURE.wat', 'wat string value');
