/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Tests that `with { type: 'bytes' }` returns a Uint8Array of the imported source bytes, does not
 * parse the imported file as JavaScript, and exposes only a default export.
 *
 * @option import-attributes=true
 * @option import-bytes=true
 */

import json from './fixtures/dummy.json' with { type: 'bytes' };
import * as jsonNamespace from './fixtures/dummy.json' with { type: 'bytes' };
import wasm from './fixtures/dummy.wasm' with { type: 'bytes' };
import * as wasmNamespace from './fixtures/dummy.wasm' with { type: 'bytes' };
import js from './fixtures/import_text_FIXTURE.js' with { type: 'bytes' };
import * as jsNamespace from './fixtures/import_text_FIXTURE.js' with { type: 'bytes' };
import mjs from './fixtures/import_text_FIXTURE.mjs' with { type: 'bytes' };
import * as mjsNamespace from './fixtures/import_text_FIXTURE.mjs' with { type: 'bytes' };

const expectedJsonBytes = stringToBytes('{}');
const expectedWasmBytes = Uint8Array.of(0x00, 0x61, 0x73, 0x6d, 0x01, 0x00, 0x00, 0x00);

function stringToBytes(text) {
    return Uint8Array.from(text, (char) => char.charCodeAt(0));
}

function bytesToString(value) {
    return Array.from(value, (byte) => String.fromCharCode(byte)).join('');
}

function stripLicenseHeader(text) {
    if (text.startsWith('/*')) {
        const end = text.indexOf('*/');
        if (end >= 0) {
            return text.slice(end + 2).trim();
        }
    }
    return text;
}

function assertBytesValue(label, actualBytes, expectedBytes) {
    if (actualBytes.length !== expectedBytes.length) {
        throw new Error(`unexpected ${label} length: ${actualBytes.length}, expected: ${expectedBytes.length}`);
    }
    for (let i = 0; i < actualBytes.length; i++) {
        if (actualBytes[i] !== expectedBytes[i]) {
            throw new Error(`unexpected ${label}[${i}]: ${actualBytes[i]}`);
        }
    }
}

function assertBytes(label, actualBytes, expectedBytes, filter) {
    if (!(actualBytes instanceof Uint8Array)) {
        throw new Error(`expected Uint8Array for ${label}`);
    }
    if (!(actualBytes.buffer instanceof ArrayBuffer)) {
        throw new Error(`expected ArrayBuffer for ${label}`);
    }
    let filteredBytes = stringToBytes(stripLicenseHeader(bytesToString(actualBytes)));
    assertBytesValue(label, filter ? filter(actualBytes) : actualBytes, expectedBytes);
}

function assertBytesNamespace(label, namespace, expectedBytes, filter) {
    const keys = Object.getOwnPropertyNames(namespace).join(',');
    if (keys !== 'default') {
        throw new Error(`unexpected ${label} namespace keys: ${keys}`);
    }
    assertBytes(`${label} namespace default export`, namespace.default, expectedBytes, filter);
}

function assertStaticBytesModule(label, value, namespace, expectedBytes, filter) {
    assertBytes(`${label} import`, value, expectedBytes, filter);
    assertBytesNamespace(label, namespace, expectedBytes, filter);
}

async function assertDynamicBytesModule(label, specifier, expectedBytes, filter) {
    const namespace = await import(specifier, { with: { type: 'bytes' } });
    assertBytesNamespace(`${label} import()`, namespace, expectedBytes, filter);
}

function textFilter(bytes) {
    return stringToBytes(stripLicenseHeader(bytesToString(bytes)));
}

assertStaticBytesModule('.json', json, jsonNamespace, expectedJsonBytes);
assertStaticBytesModule('.wasm', wasm, wasmNamespace, expectedWasmBytes);
assertStaticBytesModule('.js', js, jsNamespace, stringToBytes('invalid { javascript'), textFilter);
assertStaticBytesModule('.mjs', mjs, mjsNamespace, stringToBytes('invalid { javascript module'), textFilter);

await assertDynamicBytesModule('.json', './fixtures/dummy.json', expectedJsonBytes);
await assertDynamicBytesModule('.wasm', './fixtures/dummy.wasm', expectedWasmBytes);
await assertDynamicBytesModule('.js', './fixtures/import_text_FIXTURE.js', stringToBytes('invalid { javascript'), textFilter);
await assertDynamicBytesModule('.mjs', './fixtures/import_text_FIXTURE.mjs', stringToBytes('invalid { javascript module'), textFilter);

await assertDynamicBytesModule('data: without mime type', 'data:;base64,AP+AQQ==', Uint8Array.of(0x00, 0xff, 0x80, 0x41));
await assertDynamicBytesModule('data: with mime type', 'data:application/octet-stream;base64,AP+AQQ==', Uint8Array.of(0x00, 0xff, 0x80, 0x41));
