/*
 * Copyright (c) 2021, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

load('../assert.js');

const HashMap = Java.type("java.util.LinkedHashMap");

let obj = {};

let foreignMap = new HashMap();
foreignMap.put("key", "value");
foreignMap.put(obj, 42);
foreignMap.put(13.37, 3.14);

assertArrayEquals(["key", obj, 13.37], forIn(foreignMap));
assertArrayEquals([["key", "value"], [obj, 42], [13.37, 3.14]], forOf(foreignMap));
assertArrayEquals([["key", "value"], [obj, 42], [13.37, 3.14]], Array.from(foreignMap));

// keys() returns member keys (only)!
let keys = Object.keys(foreignMap);
assertFalse(keys.includes("key"));

let jsMap = new Map(foreignMap);
assertSame("value", jsMap.get("key"));
assertSame(42, jsMap.get(obj));
assertSame(3.14, jsMap.get(13.37));

let objFromMap = Object.fromEntries(foreignMap);
assertSame("value", objFromMap.key);
assertSame(42, objFromMap[obj]); // key is '[object Object]'
assertSame(3.14, objFromMap[13.37]);


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

function assertArrayEquals(expected, actual) {
    if (!Array.isArray(actual)) {
        throw new Error(`Not an array: ${actual}`);
    }
    if (expected.length != actual.length) {
        throw new Error(`Expected length: ${expected.length}, actual length: ${actual.length}`);
    }

    for (let i = 0; i < expected.length; i++) {
        let e = expected[i];
        let a = actual[i];
        if (Array.isArray(e)) {
            assertArrayEquals(e, a);
        } else {
            assertSame(e, a);
        }
    }
}
