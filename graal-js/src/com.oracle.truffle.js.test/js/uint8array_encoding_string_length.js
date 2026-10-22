/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Tests that Uint8Array string encodings enforce the string length limit.
 *
 * @option ecmascript-version=staging
 * @option string-length-limit=22
 */

load("./assert.js");

assertSame("", new Uint8Array(0).toHex());
assertSame("00".repeat(11), new Uint8Array(11).toHex());
assertThrows(() => new Uint8Array(12).toHex(), RangeError);

assertSame("", new Uint8Array(0).toBase64());
assertSame("A".repeat(20), new Uint8Array(15).toBase64());
assertThrows(() => new Uint8Array(16).toBase64(), RangeError);

for (const alphabet of ["base64", "base64url"]) {
    for (const omitPadding of [false, true]) {
        const options = {alphabet, omitPadding};
        for (let length = 0; length <= 18; length++) {
            const bytes = new Uint8Array(length);
            const resultLength = omitPadding ? Math.ceil(length * 4 / 3) : Math.ceil(length / 3) * 4;
            if (resultLength > 22) {
                assertThrows(() => bytes.toBase64(options), RangeError);
            } else {
                const unpaddedLength = Math.ceil(length * 4 / 3);
                const expected = "A".repeat(unpaddedLength) + "=".repeat(resultLength - unpaddedLength);
                assertSame(expected, bytes.toBase64(options));
            }
        }
    }
}

const buffer = new Uint8Array(30).fill(255).buffer;
assertSame("ff".repeat(11), new Uint8Array(buffer, 7, 11).toHex());
assertSame("/".repeat(21) + "w", new Uint8Array(buffer, 7, 16).toBase64({omitPadding: true}));
assertSame("_".repeat(21) + "w", new Uint8Array(buffer, 7, 16).toBase64({alphabet: "base64url", omitPadding: true}));

const oversized = new Uint8Array(17);
assertThrows(() => oversized.toBase64({alphabet: "invalid"}), TypeError);
oversized.buffer.transfer();
assertThrows(() => oversized.toHex(), TypeError);
assertThrows(() => oversized.toBase64(), TypeError);
