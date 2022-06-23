/*
 * Copyright (c) 2022, 2022, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

function copyPropsRenamed(src, dest, prefix) {
}

function copyPrototype(src, dest, prefix) {
}

var primordials = [];

const {Map} = primordials;

function foo() {

    [
      'JSON',
      'Math',
      'Proxy',
      'Reflect',
    ].forEach(function lambda1(name) {
      copyPropsRenamed(globalThis[name], primordials, name);
    });

    [
      'AggregateError',
      'Array',
      'ArrayBuffer',
      'BigInt',
      'BigInt64Array',
      'BigUint64Array',
      'Boolean',
      'DataView',
      'Date',
      'Error',
      'EvalError',
      'FinalizationRegistry',
      'Float32Array',
      'Float64Array',
      'Function',
      'Int16Array',
      'Int32Array',
      'Int8Array',
      'Map',
      'Number',
      'Object',
      'RangeError',
      'ReferenceError',
      'RegExp',
      'Set',
      'String',
      'Symbol',
      'SyntaxError',
      'TypeError',
      'URIError',
      'Uint16Array',
      'Uint32Array',
      'Uint8Array',
      'Uint8ClampedArray',
      'WeakMap',
      'WeakRef',
      'WeakSet',
    ].forEach(function lambda2(name) {
      const original = globalThis[name];
      primordials[name] = original;
      copyPropsRenamed(original, primordials, name);
      copyPrototype(original.prototype, primordials, `${name}Prototype`);
    });
}

for (var i = 0; i < 100000; i++) {
    foo();
}
