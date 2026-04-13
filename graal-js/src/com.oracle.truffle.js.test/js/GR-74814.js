/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * RegExp.prototype[@@split] must handle limit = 0 correctly in both the
 * pristine fast path and the non-pristine/spec path.
 */

load("assert.js");

function assertPristineSplitLimitZero(re, string) {
    assertSameContent([], re[Symbol.split](string, 0));
}

assertPristineSplitLimitZero(/,/, "a,b");
assertPristineSplitLimitZero(/,/, "ab");
assertPristineSplitLimitZero(/,/y, "a,b");

{
    const sideEffects = [];
    const re = /,/;

    Object.defineProperty(re, "constructor", {
        get() {
            sideEffects.push("constructor");
            return RegExp;
        }
    });

    Object.defineProperty(re, "flags", {
        get() {
            sideEffects.push("flags");
            return "";
        }
    });

    const result = re[Symbol.split]("a,b", 0);
    assertSameContent([], result);
    assertSameContent(["constructor", "flags"], sideEffects);
}
