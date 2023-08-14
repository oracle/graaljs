/*
 * Copyright (c) 2020, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Tests of logical assignment operators.
 * 
 * @option ecmascript-version=2021
 */

load('assert.js');

/**
 * Test of keys variable in context object
 */
const keysIinput = '{ "foo": [{ "bar": "baz" }] }';
const expectedKeys = ['', 'foo', 0, 'bar'];
let spiedKeys;
JSON.parse(keysIinput, (key, val, {keys}) => (spiedKeys = spiedKeys || keys, val));

assertEqual(expectedKeys.length, spiedKeys.length);

assertTrue(expectedKeys.every((key, i) => spiedKeys[i] === key));

/**
 * Test of input and index variables in context object
 */
const contextVariableInput = '\n\t"use\\u0020strict"';
let spied;
const parsed = JSON.parse(contextVariableInput, (key, val, context) => (spied = context, val));

assertEqual(parsed, 'use strict');

assertEqual(spied.source, '"use\\u0020strict"');

assertEqual(spied.index, 2);

assertEqual(spied.input, contextVariableInput);

assertEqual(spied.input, contextVariableInput);

/**
 * Test JSON stringify with raw JSON
 */
const digitsToBigInt = (key, val, {source}) => /^[0-9]+$/.test(source) ? BigInt(source) : val;

const bigIntToRawJSON = (key, val) => typeof val === "bigint" ? JSON.rawJSON(String(val)) : val;

const tooBigForNumber = BigInt(Number.MAX_SAFE_INTEGER) + 2n;
const parsedToBigForNumber = JSON.parse(String(tooBigForNumber), digitsToBigInt);

assertEqual(parsedToBigForNumber, tooBigForNumber);

const wayTooBig = BigInt("1" + "0".repeat(1000));
const parsedWayTooBig = JSON.parse(String(wayTooBig), digitsToBigInt);

assertEqual(parsedWayTooBig, wayTooBig);
JSON.parse(String(wayTooBig), digitsToBigInt) === wayTooBig;

const embedded = JSON.stringify({ tooBigForNumber }, bigIntToRawJSON);
assertEqual(embedded, '{"tooBigForNumber":9007199254740993}');

/**
 * Test index of precision loss
 */
const lossIndex = JSON.parse(`{ "big": 9999999999999999 }`,
  (key, val, {index, input}) => {
    if (typeof val == "number") {
      if (Math.abs(val) > Number.MAX_SAFE_INTEGER) {
        return index;
      }
    }
    return val;
})
assertEqual(lossIndex.big, 9);

/**
 * Test source access
 */
const bigIntSource = BigInt(" 9999999999999999");
const parsedBigInt = JSON.parse(" 9999999999999999", (key, val, {source}) => BigInt(source));
assertEqual(bigIntSource, parsedBigInt);

const parsedBigIntTwo = JSON.parse('{"test": {"small": 123, "big": ["teststring", 9999999999999999]}}', function(key, val, {keys, source}){
    if (keys.length === 4 && keys.join(".") === ".test.big.1") {
        return BigInt(source);
    }
    return val;
});
assertEqual(parsedBigIntTwo.test.big[1], bigIntSource);