/*
 * Copyright (c) 2024, 2024, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/*
 * Tests that large long values are converted to smaller integer types consistently.
 */

const longValues = [
    java.lang.Long.parseLong("18014398509481984"),  //  (2**54)
    java.lang.Long.parseLong("18014398509481985"),  //  (2**54 + 1)
    java.lang.Long.parseLong("18014398509481986"),  //  (2**54 + 2)
    java.lang.Long.parseLong("18014398509481987"),  //  (2**54 + 3)
    java.lang.Long.parseLong("18014398509481988"),  //  (2**54 + 4)
    java.lang.Long.parseLong("-18014398509481984"), // -(2**54)
    java.lang.Long.parseLong("-18014398509481985"), // -(2**54 + 1)
    java.lang.Long.parseLong("-18014398509481986"), // -(2**54 + 2)
    java.lang.Long.parseLong("-18014398509481987"), // -(2**54 + 3)
    java.lang.Long.parseLong("-18014398509481988"), // -(2**54 + 4)

    java.lang.Long.parseLong("9007199254740992"),  //  (2**53)     == MAX_SAFE_INTEGER + 1
    java.lang.Long.parseLong("9007199254740991"),  //  (2**53 - 1) == MAX_SAFE_INTEGER
    java.lang.Long.parseLong("9007199254740990"),  //  (2**53 - 2) == MAX_SAFE_INTEGER - 1
    java.lang.Long.parseLong("9007199254740989"),  //  (2**53 - 3) == MAX_SAFE_INTEGER - 2
    java.lang.Long.parseLong("-9007199254740992"), // -(2**53)     == MIN_SAFE_INTEGER - 1
    java.lang.Long.parseLong("-9007199254740991"), // -(2**53 - 1) == MIN_SAFE_INTEGER
    java.lang.Long.parseLong("-9007199254740990"), // -(2**53 - 2) == MIN_SAFE_INTEGER + 1
    java.lang.Long.parseLong("-9007199254740989"), // -(2**53 - 3) == MIN_SAFE_INTEGER + 2
];

const intTypes = [
    "Int32",
    "Uint32",
    "Int16",
    "Uint16",
    "Int8",
    "Uint8"
];
const intTypeConversions = new Map(intTypes.map(k => [k, []]));

function toInt32(value) {
    return value | 0;
}
function toUint32(value) {
    return value >>> 0;
}
function toInt16(value) {
    return value << 16 >> 16;
}
function toUint16(value) {
    return String.fromCharCode(value).charCodeAt(0);
}
function toInt8(value) {
    return value << 24 >> 24;
}
function toUint8(value) {
    return value & 0xff;
}

for (const type of intTypes) {
    globalThis.eval(`
function to${type}UsingTypedArray(value) {
    const ta = new ${type}Array(1);
    ta[0] = value;
    return ta[0];
}
function to${type}UsingDataView(value) {
    const dv = new DataView(new ArrayBuffer(4));
    dv.set${type}(0, value, true);
    return dv.get${type}(0, true);
}
function to${type}UsingCompareExchange(value) {
    const ta = new ${type}Array(new SharedArrayBuffer(4));
    Atomics.compareExchange(ta, 0, 0, value);
    return Atomics.compareExchange(ta, 0, 0, 0);
}
    `);
    intTypeConversions.get(type).push(
        globalThis[`to${type}`],
        globalThis[`to${type}UsingTypedArray`],
        globalThis[`to${type}UsingDataView`],
        globalThis[`to${type}UsingCompareExchange`],
    );
}

for (const longValue of longValues) {
    let allResults = [];
    for (const intType of intTypes) {
        let results = [];
        for (const conversion of intTypeConversions.get(intType)) {
            const actualResult = conversion(longValue);

            const resultTuple = [actualResult, conversion.name, longValue];
            results.push(resultTuple);
            allResults.push(resultTuple);
        }
        // for the same int type, different conversion methods should yield the same result
        checkConsistentResults(results);
    }
    // check consistency across different int types
    checkConsistentResults(allResults);
}

function checkConsistentResults(results) {
    const [expectedResult, _, longValue] = results[0];
    if (!results.every(([actualResult, conversionName]) => {
        let bits;
        if (expectedResult < 0 && (bits = /toUint(\d{1,2})/.exec(conversionName)?.[1])) {
            return actualResult === (2 ** bits) + expectedResult;
        }
        return actualResult === expectedResult;
    })) {
        throw new Error("Inconsistent results\n" +
            `  where long value = ${longValue}\n` +
            results.map(([actualResult, conversionName]) => `    ${conversionName.padEnd(30)}= ${actualResult}`).join("\n"));
    }
}
