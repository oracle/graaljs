/*
 * Copyright (c) 2023, 2023, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Basic tests of ArrayBuffer.prototype.transfer[ToFixedLength]
 * 
 * @option ecmascript-version=staging
 */

load("assert.js");

['transfer', 'transferToFixedLength'].forEach(name => {
    var fn = ArrayBuffer.prototype[name];
    assertSame(name, fn.name);
    assertSame(0, fn.length);

    var desc = Object.getOwnPropertyDescriptor(ArrayBuffer.prototype, name);
    assertSame(false, desc.enumerable);
    assertSame(true, desc.writable);
    assertSame(true, desc.configurable);

    assertThrows(() => fn.apply(42), TypeError);
    assertThrows(() => fn.apply({}), TypeError);
    assertThrows(() => fn.apply(new SharedArrayBuffer(8)), TypeError);

    [undefined, 4, 8, 16].forEach((newLength) => {
        var buffer = new ArrayBuffer(8);
        assertSame(false, buffer.detached);

        var array = new Uint8Array(buffer);
        for (var i = 0; i < 8; i++) {
            array[i] = 8 - i;
        }

        var newBuffer = fn.call(buffer, newLength);

        assertSame(true, buffer.detached);
        assertSame(0, buffer.byteLength);
        assertSame(false, newBuffer.detached);

        var expectedNewLength = newLength || 8;
        assertSame(expectedNewLength, newBuffer.byteLength);
        array = new Uint8Array(newBuffer);
        for (var i = 0; i < expectedNewLength; i++) {
            var expectedValue = i < 8 ? (8 - i) : 0;
            assertSame(expectedValue, array[i]);
        }

        assertThrows(() => fn.apply(buffer), TypeError);

        var result = fn.call(newBuffer, {
            valueOf() {
                return 42;
            }
        });
        assertSame(result.byteLength, 42);
    });

});
