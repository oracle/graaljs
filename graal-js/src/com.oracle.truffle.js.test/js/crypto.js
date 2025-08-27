/*
 * Copyright (c) 2025, 2025, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Tests for Web Crypto API.
 *
 * @option crypto
 */

load("assert.js");

function assertArrayNotEquals(a, b) {
    for (let i = 0; i < a.length; i++) {
        if (a[i] !== b[i]) return;
    }
    throw new Error('Arrays are equal')
}

function assertStartMiddleEndNotAllZeros(a) {
    if (a[0] === 0 && a[1] === 0 && a[2] === 0 && a[3] === 0 && a[4] === 0 && a[5] === 0 && a[6] === 0 && a[7] === 0) throw new Error('Start should not be all zeros');
    const l = a.length;
    const m = l / 2;
    if (a[m - 8] === 0 && a[m - 7] === 0 && a[m - 6] === 0 && a[m - 5] === 0 && a[m - 4] === 0 && a[m - 3] === 0 && a[m - 2] === 0 && a[m - 1] === 0) throw new Error('Middle should not be all zeros');
    if (a[l - 8] === 0 && a[l - 7] === 0 && a[l - 6] === 0 && a[l - 5] === 0 && a[l - 4] === 0 && a[l - 3] === 0 && a[l - 2] === 0 && a[l - 1] === 0) throw new Error('End should not be all zeros');
}

// All integer-type TypedArrays (excludes Float(16|32|64)Array)
const intTypedArrayClasses = [Int8Array, Uint8Array, Uint8ClampedArray, Int16Array, Uint16Array, Int32Array, Uint32Array, BigInt64Array, BigUint64Array];

// Test Crypto interface
{
    if (typeof crypto !== 'object') throw new Error('crypto should be of type object');
    if (typeof Crypto !== 'function') throw new Error('Crypto should be of type function');
    if (Object.getPrototypeOf(crypto) !== Crypto.prototype) throw new Error('crypto has wrong prototype');
    if (crypto[Symbol.toStringTag] !== 'Crypto') throw new Error('crypto has wrong toStringTag');
    if (Crypto.prototype[Symbol.toStringTag] !== 'Crypto') throw new Error('Crypto.prototype has wrong toStringTag');
    if (crypto.__proto__.constructor !== Crypto) throw new Error('crypto has wrong proto constructor');
    if (crypto.getRandomValues.length !== 1) throw new Error('getRandomValues has wrong length');
    if (Object.hasOwn(crypto, Symbol.toStringTag)) throw new Error('crypto should not have toStringTag as own property');
    if (Object.hasOwn(crypto, 'getRandomValues')) throw new Error('crypto should not have getRandomValues as own property');
    if (crypto.randomUUID.length !== 0) throw new Error('randomUUID has wrong length');
    if (Object.hasOwn(crypto, 'randomUUID')) throw new Error('crypto should not have randomUUID as own property');
    assertThrows(function () {
        Crypto();
    }, TypeError);
    assertThrows(function () {
        new Crypto();
    }, TypeError);
}

// Test getRandomValues(): fills TypedArrays
{
    const size = 1024;
    for (const typedArrayClass of intTypedArrayClasses) {
        const a = crypto.getRandomValues(new typedArrayClass(size));
        const b = crypto.getRandomValues(new typedArrayClass(size));
        if (size !== a.length || size !== b.length) throw new Error('Unexpected size');
        assertArrayNotEquals(a, b);
        assertStartMiddleEndNotAllZeros(a);
        assertStartMiddleEndNotAllZeros(b);
    }
}

// Test getRandomValues(): handles Uint8ClampedArray
{
    const buffer = new ArrayBuffer(8);
    const array = new Uint8ClampedArray(buffer, 1, 4);
    crypto.getRandomValues(array);
    const view = new DataView(buffer);
    if (view.getInt8(0) !== 0 || view.getInt8(5) !== 0 || view.getInt8(6) !== 0 || view.getInt8(7) !== 0) throw new Error('Unclamped area changed');
    if (view.getInt8(1) === 0 && view.getInt8(2) === 0 && view.getInt8(3) === 0 && view.getInt8(4) === 0) throw new Error('Clamped area not changed');
}

// Test getRandomValues(): handles detached buffers
{
    const buffer = new ArrayBuffer(8);
    const array = new Uint8Array(buffer);
    const newBuffer = buffer.transfer();
    if (!buffer.detached || newBuffer.detached) throw new Error('Failed to detach buffer');
    crypto.getRandomValues(array);
}

// Test getRandomValues(): handles interop
{
    const byteBuffer = Java.type('java.nio.ByteBuffer');
    const a = byteBuffer.allocateDirect(16);
    const b = byteBuffer.allocateDirect(16);
    crypto.getRandomValues(new Uint8Array(a));
    crypto.getRandomValues(new Uint8Array(b));
    if (a.equals(b)) throw new Error('Buffers should have different contents');
}

// Test getRandomValues(): throws errors
{
    const failedMessagePrefix = 'Failed to execute \'getRandomValues\' on \'Crypto\': ';
    for (const invalidObject of [null, undefined, true, 42, {}, [0, 0, 0, 0]]) {
        assertThrows(function () {
            crypto.getRandomValues(invalidObject);
        }, TypeError, `${failedMessagePrefix}parameter 1 is not of type 'ArrayBufferView'.`);
    }
    for (const invalidObject of [new Float16Array(16), new Float32Array(32), new Float64Array(64)]) {
        assertThrows(function () {
            crypto.getRandomValues(invalidObject);
        }, TypeError, `${failedMessagePrefix}The provided ArrayBufferView is of type '${invalidObject.constructor.name}', which is not an integer array type.`);
    }
    const byteLengthLimit = 1 << 16;
    for (const typedArrayClass of intTypedArrayClasses) {
        const maxLength = byteLengthLimit / typedArrayClass.BYTES_PER_ELEMENT;
        crypto.getRandomValues(new typedArrayClass(maxLength));
        const tooBigLength = maxLength + 1;
        assertThrows(function () {
            crypto.getRandomValues(new typedArrayClass(tooBigLength));
        }, RangeError, `${failedMessagePrefix}The ArrayBufferView's byte length (${tooBigLength * typedArrayClass.BYTES_PER_ELEMENT}) exceeds the number of bytes of entropy available via this API.`);
    }
    assertThrows(function () {
        crypto.getRandomValues.apply({}, [new Uint8Array(8)]);
    }, TypeError, 'Illegal invocation');
}

// Test randomUUID(): returns a string of length 36
{
    const uuid = crypto.randomUUID();
    if (typeof uuid !== 'string') throw new Error('Not a string');
    if (uuid.length !== 36) throw new Error('UUID wrong length');
}

// Test randomUUID(): string matches UUID v4 format
{
    const uuid = crypto.randomUUID();
    // Simple v4 regex (e.g.: '6ba7b810-9dad-11d1-80b4-00c04fd430c8')
    const uuidV4 = /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;
    if (!uuidV4.test(uuid)) throw new Error('UUID format invalid');
}

// Test randomUUID(): multiple calls yield different results
{
    const uuid1 = crypto.randomUUID();
    const uuid2 = crypto.randomUUID();
    if (uuid1 === uuid2) throw new Error('UUIDs are not unique');
}

// Test randomUUID(): throws error
{
    assertThrows(function () {
        crypto.randomUUID.apply({});
    }, TypeError, 'Illegal invocation');
}