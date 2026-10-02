/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Tests of %TypedArray%.prototype.set.
 */

load("assert.js");

// SetTypedArrayFromArrayLike validates the target before checking whether the offset is in range.
{
    const buffer = new ArrayBuffer(8);
    const target = new Uint8Array(buffer);
    buffer.transfer();
    assertThrows(() => target.set([1], 2 ** 31), TypeError);
}

// The source is accessed before a large positive offset is rejected.
{
    let lengthAccessed = false;
    const source = {
        get length() {
            lengthAccessed = true;
            return 1;
        }
    };
    assertThrows(() => new Uint8Array(8).set(source, 2 ** 31), RangeError);
    assertTrue(lengthAccessed);
}

// SetTypedArrayFromTypedArray validates the source before checking whether the offset is in range.
{
    const buffer = new ArrayBuffer(8);
    const source = new Uint8Array(buffer);
    buffer.transfer();
    assertThrows(() => new Uint8Array(8).set(source, 2 ** 31), TypeError);
}

// Overlapping views with different element widths use the source values as a snapshot.
{
    const buffer = new ArrayBuffer(16);
    const bytes = new Uint8Array(buffer);
    for (let i = 0; i < bytes.length; i++) {
        bytes[i] = i + 1;
    }

    const source = new Uint8Array(buffer, 8, 4);
    const target = new Uint32Array(buffer);
    target.set(source);

    assertSameContent([9, 10, 11, 12], target);
}

// The actual target index is revalidated if the target shrinks during source access or conversion.
function testShrinkDuringSet(createSource) {
    const buffer = new ArrayBuffer(4, {maxByteLength: 4});
    const target = new Uint8Array(buffer);

    target.set(createSource(buffer), 2);
    buffer.resize(4);

    assertSameContent([0, 0, 0, 0], target);
}

testShrinkDuringSet(buffer => ({
    length: 1,
    get 0() {
        buffer.resize(1);
        return 42;
    }
}));

testShrinkDuringSet(buffer => [{
    valueOf() {
        buffer.resize(1);
        return 42;
    }
}]);
