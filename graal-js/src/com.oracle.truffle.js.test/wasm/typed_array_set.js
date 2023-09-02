/*
 * Copyright (c) 2023, 2023, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Test of TypedArray.prototype.set on arrays backed by wasm buffers.
 * 
 * @option webassembly
 * @option wasm.UseUnsafeMemory
 */

load('../js/assert.js');

let nonBigIntConstructors = [
    Int8Array,
    Uint8Array,
    Uint8ClampedArray,
    Int16Array,
    Uint16Array,
    Int32Array,
    Uint32Array,
    Float32Array,
    Float64Array
];

let bigIntConstructors = [
    BigInt64Array,
    BigUint64Array   
];

function createWASMBuffer() {
    return new WebAssembly.Memory({ initial: 1 }).buffer;
}

function fillBuffer(buffer) {
    let array = new Uint8Array(buffer);
    for (let i = 0; i < 1024; i++) {
        array[i] = 42 + i;
    }
}

function createFilledWASMBuffer() {
    let buffer = createWASMBuffer();
    fillBuffer(buffer);
    return buffer;
}

function createFilledJSBuffer() {
    let buffer = new ArrayBuffer(65536);
    fillBuffer(buffer);
    return buffer;
}

nonBigIntConstructors.forEach(NonBigIntConstructor =>
    bigIntConstructors.forEach(BigIntConstructor => {
        let wasmBuffer = createFilledWASMBuffer();
        let jsBuffer = createFilledJSBuffer();
        let wasmNonBigIntArray = new NonBigIntConstructor(wasmBuffer);
        let wasmBigIntArray = new BigIntConstructor(wasmBuffer);
        let jsNonBigIntArray = new NonBigIntConstructor(jsBuffer);
        let jsBigIntArray = new BigIntConstructor(jsBuffer);
        assertThrows(() => {
            wasmNonBigIntArray.set(jsBigIntArray.subarray(0, 512));
        }, TypeError);
        assertThrows(() => {
            wasmBigIntArray.set(jsNonBigIntArray.subarray(0, 512));
        }, TypeError);
        assertThrows(() => {
            jsNonBigIntArray.set(wasmBigIntArray.subarray(0, 512));
        }, TypeError);
        assertThrows(() => {
            jsBigIntArray.set(wasmNonBigIntArray.subarray(0, 512));
        }, TypeError);
    })
);

function test(sourceIsWasm, SourceConstructor, TargetConstructor) {
    let expected = new TargetConstructor(createFilledJSBuffer());
    expected.set(new SourceConstructor(createFilledJSBuffer()).subarray(32, 512));
    
    let sourceBuffer;
    let targetBuffer;
    if (sourceIsWasm) {
        sourceBuffer = createFilledWASMBuffer();
        targetBuffer = createFilledJSBuffer();
    } else {
        sourceBuffer = createFilledJSBuffer();
        targetBuffer = createFilledWASMBuffer();        
    }
    let actual = new TargetConstructor(targetBuffer);
    actual.set(new SourceConstructor(sourceBuffer).subarray(32, 512));

    assertSameContent(expected, actual);
}

nonBigIntConstructors.forEach(NonBigIntConstructorA => {
    nonBigIntConstructors.forEach(NonBigIntConstructorB => {
        test(true, NonBigIntConstructorA, NonBigIntConstructorB);
        test(false, NonBigIntConstructorA, NonBigIntConstructorB);
    });
});

bigIntConstructors.forEach(BigIntConstructorA => {
    bigIntConstructors.forEach(BigIntConstructorB => {
        test(true, BigIntConstructorA, BigIntConstructorB);
        test(false, BigIntConstructorA, BigIntConstructorB);
    });
});
