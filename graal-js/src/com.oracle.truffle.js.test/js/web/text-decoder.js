/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Basic tests for TextDecoder.
 *
 * @option js.text-encoding
 */

load("../assert.js");

// TextDecoder: UTF-8
for (const utf8decoder of [
    new TextDecoder(),
    new TextDecoder("utf-8", {fatal: true}),
    new TextDecoder("utf-8", {ignoreBOM: true}),
    new TextDecoder("utf-8", {fatal: true, ignoreBOM: true}),
]) {
    assertSame("utf-8", utf8decoder.encoding);

    function assertDecode(expected, ...inputArgs) {
        if (utf8decoder.fatal && (inputArgs?.[1]?.errorIfFatal ?? expected.includes("\uFFFD"))) {
            assertThrows(() => utf8decoder.decode(...inputArgs));
        } else {
            assertSame(expected, utf8decoder.decode(...inputArgs));
        }
    }

    const euro = new Uint8Array([0xe2, 0x82, 0xac]);
    assertDecode("€", euro);

    assertDecode("\uFFFD", euro.subarray(0, 2));
    assertDecode("", euro.subarray(0, 2), {stream: true});
    assertDecode("€", euro.subarray(2, 3));

    assertDecode("", euro.subarray(0, 2), {stream: true});
    assertDecode("", undefined, {stream: true});
    assertDecode("\uFFFD");

    assertDecode("$", new Uint8Array([0x24, 0xe2]), {stream: true});
    assertDecode("€", new Uint8Array([0x82, 0xac, 0xe2, 0x82]), {stream: true});
    assertDecode("€", new Uint8Array([0xac, 0xc2]), {stream: true});
    assertDecode("£", new Uint8Array([0xa3]));

    assertDecode("", euro.subarray(0, 2), {stream: true});
    assertDecode("\uFFFD", euro.subarray(0, 2), {stream: true});
    // If fatal, the previous error has cleared the pending bytes
    assertDecode("€", euro.subarray(2, 3), {errorIfFatal: true});

    // surrogate code points are always invalid
    assertDecode("\uFFFD\uFFFD\uFFFD", new Uint8Array([0xED, 0xA0, 0xBD])); // U+D83D
    assertDecode("\uFFFD\uFFFD\uFFFD", new Uint8Array([0xED, 0xB2, 0xA9])); // U+DCA9
    assertDecode("\uFFFD\uFFFD", new Uint8Array([0xED,0xB2]), {stream: true});
    assertDecode("\uFFFD", new Uint8Array([0xA9]), {stream: true});
    assertDecode("", new Uint8Array([0xED]), {stream: true});
    assertDecode("\uFFFD\uFFFD\uFFFD", new Uint8Array([0xB2,0xA9]));

    // unfinished sequences
    assertDecode("\uFFFD\uFFFD\uFFFD", new Uint8Array([0x82, 0xAC, 0xFF]), {stream: true});
    assertDecode("\uFFFD\uFFFDa", new Uint8Array([0x82, 0xAC, 0x61]), {stream: true});
    utf8decoder.decode(); // reset decoder

    if (utf8decoder.ignoreBOM) {
        assertDecode("\uFEFF\uFEFF", new Uint8Array([0xEF, 0xBB, 0xBF, 0xEF, 0xBB, 0xBF]));
        assertDecode("\uFEFF", new Uint8Array([0xEF, 0xBB, 0xBF]), {stream: true});
    } else {
        // only the first BOM is skipped
        assertDecode("\uFEFF", new Uint8Array([0xEF, 0xBB, 0xBF, 0xEF, 0xBB, 0xBF]));
        assertDecode("", new Uint8Array([0xEF, 0xBB, 0xBF]), {stream: true});
        assertDecode("\uFEFF", new Uint8Array([0xEF, 0xBB, 0xBF]));

        assertDecode("", new Uint8Array([0xEF]), {stream: true});
        assertDecode("", new Uint8Array([0xBB]), {stream: true});
        assertDecode("", new Uint8Array([0xBF]), {stream: true});
        assertDecode("", new Uint8Array([0xEF]), {stream: true});
        assertDecode("", new Uint8Array([0xBB]), {stream: true});
        assertDecode("\uFEFF", new Uint8Array([0xBF]), {stream: true});
        assertDecode("\uFFFD", new Uint8Array([0xBF]), {stream: true});
        assertDecode("\uFEFF", new Uint8Array([0xEF, 0xBB, 0xBF]));

        assertDecode("", new Uint8Array([0xC3]), {stream: true});
        assertDecode("ä", new Uint8Array([0xA4, 0xEF, 0xBB]), {stream: true});
        assertDecode("\uFEFF", new Uint8Array([0xBF]));
    }
}

function makeVariants(strings) {
    return [...strings, ...strings.map(s => s.toUpperCase()), ...strings.map(s => " " + s + " ")]
}
for (const label of [undefined, ...makeVariants(["utf-8", "utf8", "unicode-1-1-utf-8", "unicode11utf8", "unicode20utf8", "x-unicode20utf8"])]) {
    assertSame("utf-8", new TextDecoder(label).encoding);
}

// TextDecoder: UTF-16
function reverseBytes(bytes) {
    let result = new Uint8Array(bytes);
    let view = new DataView(result.buffer);
    for (let i = 0; i <= view.byteLength - 2; i += 2) {
        view.setInt16(i, view.getInt16(i, false), true);
    }
    return result;
}
for (const utf16decoder of [
        new TextDecoder("utf-16le"),
        new TextDecoder("utf-16le", {fatal: true}),
        new TextDecoder("utf-16le", {ignoreBOM: true}),
        new TextDecoder("utf-16le", {fatal: true, ignoreBOM: true}),
        new TextDecoder("utf-16be"),
        new TextDecoder("utf-16be", {fatal: true}),
        new TextDecoder("utf-16be", {ignoreBOM: true}),
        new TextDecoder("utf-16be", {fatal: true, ignoreBOM: true}),
    ]) {
    function assertDecode(expected, le, be = le && reverseBytes(le), opt) {
        let inputArgs = [
            ...(le ? [new Uint8Array(utf16decoder.encoding === "utf-16be" ? be : le)] : []),
            ...(opt ? [opt] : [])];
        if (utf16decoder.fatal && (opt?.errorIfFatal ?? expected.includes("\uFFFD"))) {
            assertThrows(() => utf16decoder.decode(...inputArgs));
        } else {
            assertSame(expected, utf16decoder.decode(...inputArgs));
        }
    }

    assertDecode("\uFFFD", [0x3D]);
    assertDecode("\uFFFD", [0x3D, 0xD8]);
    assertDecode("\uFFFD", [0xA9, 0xDC]);
    assertDecode("\uFFFD\uFFFD", [0xA9, 0xDC, 0x3D, 0xD8]);
    assertDecode("\uD83D\uDCA9", [0x3D, 0xD8, 0xA9, 0xDC]);

    // unpaired low surrogate: immediately invalid
    assertDecode("", [0xA9], [0xDC], {stream: true});
    assertDecode("\uFFFD", [0xDC], [0xA9], {stream: true});
    assertDecode("€", [0xAC, 0x20]);

    // incomplete surrogate pair
    assertDecode("", [0x3D], [0xD8], {stream: true});
    assertDecode("", [0xD8], [0x3D], {stream: true});
    assertDecode("\uD83D\uDCA9", [0xA9, 0xDC]);

    assertDecode("", [0xE4], [0x00], {stream: true});
    assertDecode("ä", [0x00, 0x3D, 0xD8, 0xA9], [0xE4, 0xD8, 0x3D, 0xDC], {stream: true});
    assertDecode("\uD83D\uDCA9", [0xDC], [0xA9]);

    // high surrogate followed by another high surrogate
    assertDecode("", [0x3D], [0xD8], {stream: true});
    assertDecode("", [0xD8], [0x3D], {stream: true});
    assertDecode("", [0x3D], [0xD8], {stream: true});
    assertDecode("\uFFFD", [0xD8], [0x3D], {stream: true});
    assertDecode("\uD83D\uDCA9", [0xA9, 0xDC], undefined, {errorIfFatal: true});

    assertDecode("\uFFFE\uFFFE", [0xFE, 0xFF, 0xFE, 0xFF]);
    if (utf16decoder.ignoreBOM) {
        assertDecode("\uFEFF\uFEFF", [0xFF, 0xFE, 0xFF, 0xFE]);
        assertDecode("\uFEFF", [0xFF, 0xFE], undefined, {stream: true});
    } else {
        // only the first BOM is skipped
        assertDecode("\uFEFF", [0xFF, 0xFE, 0xFF, 0xFE]);
        assertDecode("\uFEFF", [0xFF, 0xFE, 0xFF, 0xFE]);

        assertDecode("", [0xFF, 0xFE], undefined, {stream: true});
        assertDecode("\uFEFF", [0xFF, 0xFE], undefined);

        assertDecode("", [0xFF], [0xFE], {stream: true});
        assertDecode("", [0xFE], [0xFF], {stream: true});
        assertDecode("", [0xFF], [0xFE], {stream: true});
        assertDecode("\uFEFF", [0xFE], [0xFF], {stream: true});
        assertDecode("\uFFFD", [0xA9, 0xDC], undefined, {stream: true});
        assertDecode("\uFEFF", [0xFF, 0xFE]);
    }
}

for (const label of makeVariants(["utf-16le", "utf-16", "ucs-2", "iso-10646-ucs-2", "csunicode", "unicode", "unicodefeff"])) {
    assertSame("utf-16le", new TextDecoder(label).encoding);
}
for (const label of makeVariants(["utf-16be", "unicodefffe"])) {
    assertSame("utf-16be", new TextDecoder(label).encoding);
}

// other legacy encodings are not supported.
