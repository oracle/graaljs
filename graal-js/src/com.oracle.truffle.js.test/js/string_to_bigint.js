/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

load("./assert.js");

// StringIntegerLiteral accepts ECMAScript whitespace and ASCII digits only.
const whitespaceCodePoints = [
    0x0009, 0x000a, 0x000b, 0x000c, 0x000d, 0x0020, 0x00a0, 0x1680,
    0x2000, 0x2001, 0x2002, 0x2003, 0x2004, 0x2005, 0x2006, 0x2007,
    0x2008, 0x2009, 0x200a, 0x2028, 0x2029, 0x202f, 0x205f, 0x3000, 0xfeff
];

for (const codePoint of whitespaceCodePoints) {
    const whitespace = String.fromCodePoint(codePoint);
    assertSame(1n, BigInt(whitespace + "1" + whitespace));
    // IsLooselyEqual invokes StringToBigInt directly.
    assertTrue(1n == (whitespace + "1" + whitespace));
}

assertSame(0n, BigInt(String.fromCodePoint(...whitespaceCodePoints)));

const validC0Whitespace = new Set([0x0009, 0x000a, 0x000b, 0x000c, 0x000d, 0x0020]);
for (let codePoint = 0; codePoint <= 0x0020; codePoint++) {
    if (!validC0Whitespace.has(codePoint)) {
        const control = String.fromCodePoint(codePoint);
        assertThrows(() => BigInt(control + "1"), SyntaxError);
        assertThrows(() => BigInt("1" + control), SyntaxError);
        // Unlike BigInt(), IsLooselyEqual reports a StringToBigInt parse failure as false.
        assertFalse(1n == (control + "1"));
        assertFalse(1n == ("1" + control));
    }
}

const nonASCIIDigits = ["\u0661", "\u0967", "\uff11"];
for (const digit of nonASCIIDigits) {
    assertThrows(() => BigInt(digit), SyntaxError);
    assertThrows(() => BigInt("+" + digit), SyntaxError);
    assertThrows(() => BigInt("-" + digit), SyntaxError);
    assertThrows(() => BigInt("0b" + digit), SyntaxError);
    assertThrows(() => BigInt("0o" + digit), SyntaxError);
    assertThrows(() => BigInt("0x" + digit), SyntaxError);
}
