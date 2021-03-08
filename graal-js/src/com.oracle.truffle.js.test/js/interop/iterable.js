/*
 * Copyright (c) 2021, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/*
 * Tests interop between Java and JS Iterable/Iterator objects.
 *
 * @option foreign-object-prototype
 */

"use strict";
load('../assert.js');

const Arrays = Java.type('java.util.Arrays');

// java iterable -> js

let expected = ["perform", "or", "utter", "repeatedly"];
let iterable = Arrays.asList(expected);
assertSame(expected.length, iterable.length);

function forIn(iterable) {
    let result = [];
	for (let i in iterable) {
	    result.push(i);
	}
	return result;
}

function forOf(iterable) {
    let result = [];
	for (let i of iterable) {
	    result.push(i);
	}
	return result;
}

function assertIterableEquals(expected, actual) {
    let iterator = actual[Symbol.iterator]();
    let next;
	for (let expectedValue of expected) {
	    next = iterator.next();
	    assertFalse(next.done);
		assertSame(expectedValue, next.value);
	}
	next = iterator.next();
	assertTrue(next.done);
	assertSame(undefined, next.value);
}

assertIterableEquals(expected, iterable);
assertIterableEquals(expected, [...iterable]);
assertIterableEquals(expected, forOf(iterable));
assertIterableEquals(expected.map((v, i) => i), forIn(iterable));
assertIterableEquals(expected, Array.from(iterable));
assertIterableEquals(expected, new Set(iterable));

// js iterable -> java

function* makeIterator(iterable) {
	yield* iterable;
}

let xyz = ["x", "y", "z"];
assertSame("x-y-z", java.lang.String.join("-", xyz));
assertSame("x-y-z", java.lang.String['join(java.lang.CharSequence,java.lang.Iterable)']("-", xyz));
// iterators are iterables, too.
assertSame("x-y-z", java.lang.String.join("-", xyz[Symbol.iterator]()));
assertSame("x-y-z", java.lang.String.join("-", makeIterator(xyz)));
