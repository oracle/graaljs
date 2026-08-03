/*
 * Copyright (c) 2026, 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.
 */

load("assert.js");

// [[Set]] returns false when adding an indexed property to a non-extensible
// array. Strict assignment turns that false result into a TypeError.
const sealedArray = Object.seal([]);
assertFalse(Reflect.set(sealedArray, 0, undefined));
assertFalse(Object.hasOwn(sealedArray, 0));
assertThrows(function() {
    "use strict";
    sealedArray[0] = undefined;
}, TypeError);

// A non-writable inherited indexed property likewise makes [[Set]] fail.
const prototype = Object.defineProperty({}, "0", {
    value: undefined,
    writable: false
});
const arrayWithReadOnlyPrototypeElement = [];
Object.setPrototypeOf(arrayWithReadOnlyPrototypeElement, prototype);
assertFalse(Reflect.set(arrayWithReadOnlyPrototypeElement, 0, undefined));
assertFalse(Object.hasOwn(arrayWithReadOnlyPrototypeElement, 0));

const accessorPrototype = Object.defineProperty({}, "0", {
    get() {
        return undefined;
    }
});
const arrayWithGetterOnlyPrototypeElement = [];
Object.setPrototypeOf(arrayWithGetterOnlyPrototypeElement, accessorPrototype);
assertFalse(Reflect.set(arrayWithGetterOnlyPrototypeElement, 0, undefined));
assertFalse(Object.hasOwn(arrayWithGetterOnlyPrototypeElement, 0));

const proxyPrototype = new Proxy({}, {
    set() {
        return false;
    }
});
const arrayWithProxyPrototype = [];
Object.setPrototypeOf(arrayWithProxyPrototype, proxyPrototype);
assertFalse(Reflect.set(arrayWithProxyPrototype, 0, undefined));
assertThrows(function() {
    "use strict";
    arrayWithProxyPrototype[0] = undefined;
}, TypeError);
assertFalse(Object.hasOwn(arrayWithProxyPrototype, 0));

const arrayWithTypedArrayPrototype = [];
Object.setPrototypeOf(arrayWithTypedArrayPrototype, new Uint8Array(1));
Object.preventExtensions(arrayWithTypedArrayPrototype);
assertFalse(Reflect.set(arrayWithTypedArrayPrototype, 0, undefined));
assertThrows(function() {
    "use strict";
    arrayWithTypedArrayPrototype[0] = undefined;
}, TypeError);
assertFalse(Object.hasOwn(arrayWithTypedArrayPrototype, 0));
