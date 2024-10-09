/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Basic tests for TextEncoder.
 *
 * @option js.text-encoding
 */

load("../assert.js");

const utf8encoder = new TextEncoder();
assertSame("utf-8", utf8encoder.encoding);

// TextEncoder: encode()
assertSame(0, utf8encoder.encode.length);
assertSameContent([0x39, 0xc3,0xa4, 0xe2,0x82,0xac], utf8encoder.encode("9ä€"));
assertSameContent([0xf0,0x9f,0x92,0xa9], utf8encoder.encode("\u{1F4A9}"));
// lone surrogates are replaced with U+FFFD
assertSameContent([0xef,0xbf,0xbd, 0xef,0xbf,0xbd], utf8encoder.encode("\uDCA9\uD83D"));
// BOM and noncharacters
assertSameContent([0xef,0xbb,0xbf, 0xef,0xbf,0xbe, 0xef,0xbf,0xbf], utf8encoder.encode("\uFEFF\uFFFE\uFFFF"));

// ToString conversion
assertSameContent([], utf8encoder.encode(undefined));
assertSameContent([..."null"].map(c => c.charCodeAt(0)), utf8encoder.encode(null));
assertSameContent([..."[object Object]"].map(c => c.charCodeAt(0)), utf8encoder.encode({}));

// TextEncoder: encodeInto()
assertSame(2, utf8encoder.encodeInto.length);
let dest = new Uint8Array(18);
let result = utf8encoder.encodeInto("Hï\u3010\u{1D541}\u{1D54A}\u3011", dest);
assertSameContent([0x48, 0xc3,0xaf, 0xe3,0x80,0x90, 0xf0,0x9d,0x95,0x81, 0xf0,0x9d,0x95,0x8a, 0xe3,0x80,0x91, 0], dest);
assertSame(8, result.read);
assertSame(17, result.written);
result = utf8encoder.encodeInto("Hï\u3010\u{1D541}\u{1D54A}\u3011", dest.subarray(0, 9));
assertSame(3, result.read);
assertSame(6, result.written);

// ToString conversion
for (let [input, output] of [
    [undefined, "undefined"],
    [null, "null"],
    [{}, "[object Object]"],
]) {
    let dst = new Uint8Array(output.length);
    let res = utf8encoder.encodeInto(input, dst);
    assertSameContent([...output].map(c => c.charCodeAt(0)), dst);
    assertSame(output.length, res.read);
}

// destination must be a Uint8Array
assertThrows(() => utf8encoder.encodeInto("", new ArrayBuffer(8)));
assertThrows(() => utf8encoder.encodeInto("", new DataView(new ArrayBuffer(8))));
assertThrows(() => utf8encoder.encodeInto("", new Int8Array(8)));
assertThrows(() => utf8encoder.encodeInto("", new Uint8ClampedArray(8)));
