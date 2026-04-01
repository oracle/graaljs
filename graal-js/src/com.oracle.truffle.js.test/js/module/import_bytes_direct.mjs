/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Tests that `with { type: 'bytes' }` honors the `direct-byte-buffer` option.
 *
 * @option import-attributes=true
 * @option import-bytes=true
 * @option direct-byte-buffer=true
 * @option debug-builtin=true
 */

import staticJsonBytes from './fixtures/dummy.json' with { type: 'bytes' };
import staticWasmBytes from './fixtures/dummy.wasm' with { type: 'bytes' };

const expectedJsonBytes = stringToBytes('{}');
const expectedWasmBytes = Uint8Array.of(0x00, 0x61, 0x73, 0x6d, 0x01, 0x00, 0x00, 0x00);

function stringToBytes(text) {
    return Uint8Array.from(text, (char) => char.charCodeAt(0));
}

function assertArrayBufferClass(view) {
    if (!(view instanceof Uint8Array)) {
        throw new Error(`expected Uint8Array`);
    }
    if (!(view.buffer instanceof ArrayBuffer)) {
        throw new Error(`expected ArrayBuffer`);
    }
    const actualBufferClass = Debug.class(view.buffer);
    const expectedBufferClass = Debug.class(new ArrayBuffer(1));
    if (actualBufferClass !== expectedBufferClass) {
        throw new Error(`unexpected ArrayBuffer class: ${actualBufferClass}, expected: ${expectedBufferClass}`);
    }
}

function assertBytes(label, actualBytes, expectedBytes) {
    assertArrayBufferClass(actualBytes);
    if (actualBytes.length !== expectedBytes.length) {
        throw new Error(`unexpected ${label} length: ${actualBytes.length}`);
    }
    for (let i = 0; i < actualBytes.length; i++) {
        if (actualBytes[i] !== expectedBytes[i]) {
            throw new Error(`unexpected ${label}[${i}]: ${actualBytes[i]}`);
        }
    }
}

assertBytes('static .json import', staticJsonBytes, expectedJsonBytes);
assertBytes('static .wasm import', staticWasmBytes, expectedWasmBytes);

const dynamicJsonBytes = await import('./fixtures/dummy.json', { with: { type: 'bytes' } });
assertBytes('dynamic .json import', dynamicJsonBytes.default, expectedJsonBytes);

const dynamicWasmBytes = await import('./fixtures/dummy.wasm', { with: { type: 'bytes' } });
assertBytes('dynamic .wasm import', dynamicWasmBytes.default, expectedWasmBytes);

const dataUrlBytes = await import('data:;base64,AP+AQQ==', { with: { type: 'bytes' } });
assertBytes('data URL import without mime type', dataUrlBytes.default, Uint8Array.of(0x00, 0xff, 0x80, 0x41));

const dataUrlBytesWithMimeType = await import('data:application/octet-stream;base64,AP+AQQ==', { with: { type: 'bytes' } });
assertBytes('data URL import with mime type', dataUrlBytesWithMimeType.default, Uint8Array.of(0x00, 0xff, 0x80, 0x41));
