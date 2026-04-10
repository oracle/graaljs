/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

load("./assert.js");

function assertSplitReadsFlag(rx, flagName, string, expected) {
    const sideEffects = [];
    Object.defineProperty(rx, flagName, {
        get() {
            sideEffects.push(flagName);
            return true;
        }
    });
    const split = rx[Symbol.split](string);
    assertSameContent(expected, split);
    assertSameContent([flagName], sideEffects);
}

function assertReplaceReadsFlag(rx, flagName, string, replacement, expectedResult) {
    const sideEffects = [];
    Object.defineProperty(rx, flagName, {
        get() {
            sideEffects.push(flagName);
            return true;
        }
    });
    assertSame(expectedResult, rx[Symbol.replace](string, replacement));
    assertSameContent([flagName], sideEffects);
}

assertSplitReadsFlag(/a/, "ignoreCase", "A", ["", ""]);
assertReplaceReadsFlag(/a/, "ignoreCase", "a", "x", "x");

assertSplitReadsFlag(/^b/, "multiline", "a\nb", ["a\n", ""]);
assertReplaceReadsFlag(/a/, "multiline", "a", "x", "x");

if ("dotAll" in /a/) {
    assertSplitReadsFlag(/a.b/, "dotAll", "a\nb", ["", ""]);
    assertReplaceReadsFlag(/a/, "dotAll", "a", "x", "x");
}

if ("hasIndices" in /a/) {
    assertSplitReadsFlag(/a/, "hasIndices", "a", ["", ""]);
    assertReplaceReadsFlag(/a/, "hasIndices", "a", "x", "x");
}

if ("unicodeSets" in /(?:)/g) {
    assertSplitReadsFlag(/(?:)/g, "unicodeSets", "\uD83D\uDE00", ["\uD83D\uDE00"]);
    assertReplaceReadsFlag(/(?:)/g, "unicodeSets", "\uD83D\uDE00", "-", "-\uD83D\uDE00-");
}
