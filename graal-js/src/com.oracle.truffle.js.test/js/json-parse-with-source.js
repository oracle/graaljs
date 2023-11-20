/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Tests for the JSON.parse source text access proposal
 * (https://github.com/tc39/proposal-json-parse-with-source).
 *
 * @option ecmascript-version=staging
 */

load('assert.js');

/*
 * Test JSON.parse context object.
 */
let spied;
const parsed = JSON.parse('\n\t"use\\u0020strict"', (key, val, context) => (spied = context, val));
assertSame(parsed, 'use strict');
assertSame(spied.source, '"use\\u0020strict"');
assertSame(String.raw`{"source":"\"use\\u0020strict\""}`, JSON.stringify(spied));

/*
 * Test JSON.parse with conversion to BigInt.
 */
const digitsToBigInt = (key, val, {source}) => /^[0-9]+$/.test(source) ? BigInt(source) : val;

const tooBigForNumber = BigInt(Number.MAX_SAFE_INTEGER) + 2n;
const parsedToBigForNumber = JSON.parse(String(tooBigForNumber), digitsToBigInt);

assertSame(parsedToBigForNumber, tooBigForNumber);

const wayTooBig = BigInt("1" + "0".repeat(1000));
const parsedWayTooBig = JSON.parse(String(wayTooBig), digitsToBigInt);

assertSame(parsedWayTooBig, wayTooBig);

const bigIntValue = BigInt(" 9999999999999999");
const parsedBigInt = JSON.parse(" 9999999999999999", (key, val, {source}) => (spied = source, BigInt(source)));
assertSame(bigIntValue, parsedBigInt);
assertSame("9999999999999999", spied);

const parsedBigInt2 = JSON.parse('{"test": {"small": 123, "big": ["teststring", 9999999999999999]}}', function(key, val, context){
    if (typeof val == "number" && Math.abs(val) > Number.MAX_SAFE_INTEGER) {
        spied = context;
        return BigInt(context.source);
    }
    return val;
});

assertSame(bigIntValue, parsedBigInt2.test.big[1]);
assertSame("9999999999999999", spied.source);

/*
 * Test JSON.stringify of a BigInt value using JSON.rawJSON.
 */
const bigIntToRawJSON = (key, val) => typeof val === "bigint" ? JSON.rawJSON(String(val)) : val;

const embedded = JSON.stringify({ tooBigForNumber }, bigIntToRawJSON);
assertSame(embedded, '{"tooBigForNumber":9007199254740993}');

const bare = JSON.stringify(tooBigForNumber, bigIntToRawJSON);
assertSame(bare, '9007199254740993');

/*
 * Test illegal rawJSON strings.
 */
assertThrows(() => JSON.rawJSON(""), SyntaxError);
assertThrows(() => JSON.rawJSON("{}"), SyntaxError);
assertThrows(() => JSON.rawJSON("[]"), SyntaxError);
assertThrows(() => JSON.rawJSON('\t""'), SyntaxError);
assertThrows(() => JSON.rawJSON('\r""'), SyntaxError);
assertThrows(() => JSON.rawJSON('\n""'), SyntaxError);
assertThrows(() => JSON.rawJSON(' ""'), SyntaxError);
assertThrows(() => JSON.rawJSON('""\t'), SyntaxError);
assertThrows(() => JSON.rawJSON('""\r'), SyntaxError);
assertThrows(() => JSON.rawJSON('""\n'), SyntaxError);
assertThrows(() => JSON.rawJSON('"" '), SyntaxError);

/*
 * Test rawJSON object.
 */
assertTrue(Object.isFrozen(JSON.rawJSON("42")));
assertSame("42", JSON.rawJSON("42").rawJSON);
const desc = Object.getOwnPropertyDescriptor(JSON.rawJSON("42"), "rawJSON");
assertSame("42", desc.value);
assertSame(true, desc.enumerable);
