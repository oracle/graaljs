/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

load("./assert.js");

assertSame("[1]", JSON.stringify([[[1]]].flat({ valueOf() { return Infinity; } })));
assertSame("[1]", JSON.stringify([[[1]]].flat({ valueOf() { return 2 ** 40; } })));
assertSame("[[1]]", JSON.stringify([[1]].flat({ valueOf() { return -4294967295; } })));

assertSame("", "abc".charAt({ valueOf() { return Infinity; } }));
assertFalse("abc".startsWith("b", { valueOf() { return -4294967295; } }));
assertSame(undefined, "abc".at({ valueOf() { return Infinity; } }));

assertThrows(() => 1..toFixed({ valueOf() { return Infinity; } }), RangeError);
assertThrows(() => 10..toString({ valueOf() { return 4294967298; } }), RangeError);
assertThrows(() => (10n).toString({ valueOf() { return 4294967298; } }), RangeError);

const segments = new Intl.Segmenter("en", { granularity: "grapheme" }).segment("abc");
assertSame(undefined, segments.containing({ valueOf() { return Infinity; } }));

const re = /a/g;
let execCalls = 0;
re.exec = function() {
    execCalls++;
    if (execCalls === 1) {
        return {
            0: "a",
            length: 1,
            index: { valueOf() { return Infinity; } },
            groups: undefined
        };
    }
    return null;
};
assertSame("bax", re[Symbol.replace]("ba", "x"));
