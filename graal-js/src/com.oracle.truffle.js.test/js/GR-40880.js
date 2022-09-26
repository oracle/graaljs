/*
 * Copyright (c) 2022, 2022, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

load("assert.js");

/**
 * Tests that a property descriptor that is neither a data nor an accessor property descriptor is handled correctly by dictionary object.
 *
 * @option debug-builtin
 */

function replacer(key, value) {
    if (typeof value === 'undefined' || typeof value === 'function') {
        return typeof value;
    } else {
        return value;
    }
}

function verifyFullyPopulatedPropertyDescriptor(desc) {
    let hasOwnProperty = key => Object.hasOwn(desc, key);
    if (['get', 'set', 'enumerable', 'configurable'].every(hasOwnProperty)) {
        assertFalse(['value', 'writable'].some(hasOwnProperty));
        return true;
    } else if (['value', 'writable', 'enumerable', 'configurable'].every(hasOwnProperty)) {
        assertFalse(['get', 'set'].some(hasOwnProperty));
        return true;
    } else {
        throw new Error(`Not a fully populated property descriptor: ${JSON.stringify(desc, replacer)}`);
    }
}

const obj = {};
obj["0"] = "h";
obj["1"] = "e";
obj["2"] = "l";
obj["3"] = "l";
obj["4"] = "o";
obj["unexpected"] = 404;

if (typeof Debug != "undefined" && Debug.shape(obj).includes("unexpected")) {
    throw new Error(`obj should be a dictionary object but has an unexpected shape: ${Debug.shape(obj)}`);
}

Object.defineProperty(obj, "call", ({}));
verifyFullyPopulatedPropertyDescriptor(Object.getOwnPropertyDescriptor(obj, "call"));
assertSame(undefined, obj["call"]);

Object.defineProperty(obj, "call1", {configurable: true});
verifyFullyPopulatedPropertyDescriptor(Object.getOwnPropertyDescriptor(obj, "call1"));
assertSame(undefined, obj["call1"]);
Object.defineProperty(obj, "call2", {configurable: true, enumerable: false});
verifyFullyPopulatedPropertyDescriptor(Object.getOwnPropertyDescriptor(obj, "call2"));
assertSame(undefined, obj["call2"]);
Object.defineProperty(obj, "call3", {configurable: true, enumerable: false, writable: true});
verifyFullyPopulatedPropertyDescriptor(Object.getOwnPropertyDescriptor(obj, "call3"));
assertSame(undefined, obj["call3"]);

Object.defineProperty(obj, "accessor", {get: function(){ return 42; }});
verifyFullyPopulatedPropertyDescriptor(Object.getOwnPropertyDescriptor(obj, "accessor"));
Object.defineProperty(obj, "accessor1", {get: function(){ return 42; }, configurable: true});
verifyFullyPopulatedPropertyDescriptor(Object.getOwnPropertyDescriptor(obj, "accessor1"));
Object.defineProperty(obj, "accessor2", {get: function(){ return 42; }, configurable: true, enumerable: false});
verifyFullyPopulatedPropertyDescriptor(Object.getOwnPropertyDescriptor(obj, "accessor2"));

Object.defineProperty(obj, "call3", {get: function(){ return 42; }, configurable: true, enumerable: false});
verifyFullyPopulatedPropertyDescriptor(Object.getOwnPropertyDescriptor(obj, "call3"));
assertSame(42, obj["call3"]);
Object.defineProperty(obj, "call3", {value: 43, configurable: true, enumerable: false});
verifyFullyPopulatedPropertyDescriptor(Object.getOwnPropertyDescriptor(obj, "call3"));
assertSame(43, obj["call3"]);
