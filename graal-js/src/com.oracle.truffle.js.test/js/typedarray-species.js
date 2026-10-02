/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * TypedArraySpeciesCreate must preserve the Number or BigInt content type.
 */

load("assert.js");

const constructors = [Float64Array, BigInt64Array, BigUint64Array];

for (const Source of constructors) {
    for (const Target of constructors) {
        for (const length of [0, 2]) {
            const source = new Source(length);
            source.fill(Source.name.startsWith("Big") ? 42n : 42);
            source.constructor = Target;
            const sameContentType = Source.name.startsWith("Big") === Target.name.startsWith("Big");

            if (sameContentType) {
                const results = [
                    source.slice(0),
                    source.map(value => value),
                    source.filter(() => true),
                    source.subarray(0)
                ];
                for (const result of results) {
                    assertSame(Target, result.constructor);
                    assertSameContent(source, result);
                }
                assertSame(0, source.slice(0, 0).length);
                assertSame(0, source.filter(() => false).length);
            } else {
                assertThrows(() => source.slice(0), TypeError);
                assertThrows(() => source.slice(0, 0), TypeError);
                assertThrows(() => source.map(() => fail("map callback must not be called")), TypeError);
                assertThrows(() => source.filter(() => true), TypeError);
                assertThrows(() => source.filter(() => false), TypeError);
                assertThrows(() => source.subarray(0), TypeError);
                assertThrows(() => source.subarray(0, 0), TypeError);
            }
        }
    }
}

// Different element types within the Number content type remain valid species results.
{
    const source = new Float64Array([1, 2]);
    source.constructor = Uint8Array;
    for (const result of [source.slice(0), source.map(value => value), source.filter(() => true)]) {
        assertSame(Uint8Array, result.constructor);
        assertSameContent([1, 2], result);
    }
    assertSame(Uint8Array, source.subarray(0).constructor);
}
