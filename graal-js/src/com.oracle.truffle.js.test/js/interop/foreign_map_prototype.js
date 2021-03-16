/*
 * Copyright (c) 2021, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/*
 * Tests Map.prototype methods on foreign maps.
 *
 * @option foreign-object-prototype
 */

load('../assert.js');

const HashMap = Java.type("java.util.LinkedHashMap");

let obj = {};
let unknownKey = {};

let foreignMap = new HashMap();
foreignMap.put("key", "value");

foreignMap.set(obj, 42);
foreignMap.set(13.37, 43);
foreignMap.set("removeMe", "please");

assertSame(4, foreignMap.size());

assertSame(42, foreignMap.get(obj));
assertTrue(undefined == foreignMap.get(unknownKey));
assertTrue(foreignMap.has("removeMe"));
assertTrue(foreignMap.delete("removeMe"));
assertFalse(foreignMap.has("removeMe"));
assertFalse(foreignMap.delete("removeMe"));

assertArrayEquals([["key", "value"], [obj, 42], [13.37, 43]], forOf(foreignMap.entries()));
assertArrayEquals(["key", obj, 13.37], forOf(foreignMap.keys()));
assertArrayEquals(["value", 42, 43], forOf(foreignMap.values()));

assertSame(42, Map.prototype.get.call(foreignMap, obj));
assertSame(undefined, Map.prototype.get.call(foreignMap, unknownKey));
assertSame(foreignMap, Map.prototype.set.call(foreignMap, obj, 42));
assertSame(true, Map.prototype.has.call(foreignMap, obj));
assertSame(false, Map.prototype.has.call(foreignMap, unknownKey));

for (let prototypeCall of [true, false]) {
    let size = 0;
    let entries = [];
    let callback = (k, v) => {
        size++;
        entries.push([k, v]);
    }
    if (prototypeCall) {
        Map.prototype.forEach.call(foreignMap, callback);
    } else {
        foreignMap.forEach(callback);
    }
    assertSame(3, size);
    assertArrayEquals([["key", "value"], [obj, 42], [13.37, 43]], forOf(foreignMap.entries()));
}

Map.prototype.clear.call(foreignMap);
assertSame(0, foreignMap.size());

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
