/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Test constructing TextDecoder with unsupported encodings.
 *
 * @option js.text-encoding
 */

load("../assert.js");

/* From https://encoding.spec.whatwg.org/. */
const unsupportedLegacyEncodings = [
    // Legacy single-byte encodings
    "ibm866",
    "iso-8859-2",
    "iso-8859-3",
    "iso-8859-4",
    "iso-8859-5",
    "iso-8859-6",
    "iso-8859-7",
    "iso-8859-8",
    "iso-8859-8-i",
    "iso-8859-10",
    "iso-8859-13",
    "iso-8859-14",
    "iso-8859-15",
    "iso-8859-16",
    "koi8-r",
    "koi8-u",
    "macintosh",
    "windows-874",
    "windows-1250",
    "windows-1251",
    "windows-1252",
    "windows-1253",
    "windows-1254",
    "windows-1255",
    "windows-1256",
    "windows-1257",
    "windows-1258",
    "x-mac-cyrillic",
    // Legacy multi-byte Chinese (simplified) encodings
    "gbk",
    "gb18030",
    // Legacy multi-byte Chinese (traditional) encodings
    "big5",
    // Legacy multi-byte Japanese encodings
    "euc-jp",
    "iso-2022-jp",
    "shift_jis",
    // Legacy multi-byte Korean encodings
    "euc-kr",
    // Legacy miscellaneous encodings
    "replacement",
    // "utf-16be", // supported
    // "utf-16le", // supported
    "x-user-defined",
];

for (const encodingName of unsupportedLegacyEncodings) {
    assertThrows(() => new TextDecoder(encodingName), RangeError);
}
