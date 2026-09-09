/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Tests fixed-length and resizable WebAssembly memory buffers.
 *
 * @option ecmascript-version=2024
 * @option webassembly
 * @option worker
 * @option wasm.Threads
 * @option wasm.UseUnsafeMemory
 */

load('../js/assert.js');

const pageSize = 65536;

assertSame(0, WebAssembly.Memory.prototype.toFixedLengthBuffer.length);
assertSame(0, WebAssembly.Memory.prototype.toResizableBuffer.length);
assertThrows(() => WebAssembly.Memory.prototype.toFixedLengthBuffer.call({}), TypeError);
assertThrows(() => WebAssembly.Memory.prototype.toResizableBuffer.call({}), TypeError);

(function testUnsharedMemory() {
    const noMaximum = new WebAssembly.Memory({initial: 1});
    const noMaximumBuffer = noMaximum.buffer;
    assertTrue(noMaximumBuffer === noMaximum.toFixedLengthBuffer());
    assertThrows(() => noMaximum.toResizableBuffer(), TypeError);
    assertTrue(noMaximum.buffer === noMaximumBuffer);
    assertSame(1, noMaximum.grow(1));
    assertTrue(noMaximumBuffer.detached);
    assertSame(2 * pageSize, noMaximum.buffer.byteLength);

    const memory = new WebAssembly.Memory({initial: 1, maximum: 4});
    const original = memory.buffer;
    assertFalse(original.resizable);

    const resizable = memory.toResizableBuffer();
    assertTrue(original.detached);
    assertTrue(resizable.resizable);
    assertSame(4 * pageSize, resizable.maxByteLength);
    assertTrue(resizable === memory.buffer);
    assertTrue(resizable === memory.toResizableBuffer());

    const view = new Uint8Array(resizable);
    view[0] = 42;
    assertThrows(() => resizable.resize(pageSize + 1), RangeError);
    resizable.resize(2 * pageSize);
    assertSame(2 * pageSize, resizable.byteLength);
    assertSame(42, view[0]);
    assertThrows(() => resizable.resize(pageSize), RangeError);

    assertSame(2, memory.grow(1));
    assertTrue(resizable === memory.buffer);
    assertSame(3 * pageSize, resizable.byteLength);
    assertSame(42, view[0]);

    const fixed = memory.toFixedLengthBuffer();
    assertTrue(resizable.detached);
    assertFalse(fixed.resizable);
    assertSame(3 * pageSize, fixed.byteLength);
    assertTrue(fixed === memory.buffer);
    assertTrue(fixed === memory.toFixedLengthBuffer());

    assertSame(3, memory.grow(1));
    assertTrue(fixed.detached);
    assertSame(4 * pageSize, memory.buffer.byteLength);

    const maximum32BitPages = new WebAssembly.Memory({initial: 1, maximum: 65536});
    const maximum32BitPagesBuffer = maximum32BitPages.toResizableBuffer();
    assertSame(4294967296, maximum32BitPagesBuffer.maxByteLength);
    assertThrows(() => maximum32BitPagesBuffer.resize(2 ** 31), RangeError, 'Buffer too large');
    assertSame(pageSize, maximum32BitPagesBuffer.byteLength);

    const sharedMaximum32BitPages = new WebAssembly.Memory({initial: 1, maximum: 65536, shared: true});
    const sharedMaximum32BitPagesBuffer = sharedMaximum32BitPages.toResizableBuffer();
    assertSame(4294967296, sharedMaximum32BitPagesBuffer.maxByteLength);
    assertThrows(() => sharedMaximum32BitPagesBuffer.grow(2 ** 31), RangeError, 'Buffer too large');
    assertSame(pageSize, sharedMaximum32BitPagesBuffer.byteLength);
})();

(function testClonedResizableBufferPreservesBackingCapacity() {
    const memory = new WebAssembly.Memory({initial: 1, maximum: 4});
    const buffer = memory.toResizableBuffer();
    const worker = new Worker(`
        onmessage = function (event) {
            const buffer = event.data;
            buffer.resize(2 * ${pageSize});
            const view = new Uint8Array(buffer);
            view[${pageSize}] = 42;
            postMessage([buffer.byteLength, view[${pageSize}]]);
        };
    `, {type: "string"});
    worker.postMessage(buffer);
    assertSameContent([2 * pageSize, 42], worker.getMessage());
    worker.terminate();
})();

(function testModuleDefinedMemoryMaximum() {
    function exportedMemory(memorySection) {
        const header = [0x00, 0x61, 0x73, 0x6d, 0x01, 0x00, 0x00, 0x00];
        const memoryExport = [0x07, 0x0a, 0x01, 0x06, 0x6d, 0x65, 0x6d, 0x6f, 0x72, 0x79, 0x02, 0x00];
        const module = new WebAssembly.Module(new Uint8Array([...header, ...memorySection, ...memoryExport]));
        return new WebAssembly.Instance(module).exports.memory;
    }

    const noMaximum = exportedMemory([0x05, 0x03, 0x01, 0x00, 0x01]);
    assertThrows(() => noMaximum.toResizableBuffer(), TypeError);

    const maximum65536 = exportedMemory([0x05, 0x06, 0x01, 0x01, 0x01, 0x80, 0x80, 0x04]);
    assertSame(4294967296, maximum65536.toResizableBuffer().maxByteLength);
})();

(function testReexportedMemoryUsesOriginalMaximum() {
    const bytes = new Uint8Array([
        0x00, 0x61, 0x73, 0x6d, 0x01, 0x00, 0x00, 0x00,
        0x02, 0x0e, 0x01, 0x01, 0x6d, 0x06, 0x6d, 0x65, 0x6d, 0x6f, 0x72, 0x79, 0x02, 0x01, 0x01, 0x05,
        0x07, 0x0a, 0x01, 0x06, 0x6d, 0x65, 0x6d, 0x6f, 0x72, 0x79, 0x02, 0x00,
    ]);
    const memory = new WebAssembly.Memory({initial: 1, maximum: 4});
    const reexportedMemory = new WebAssembly.Instance(new WebAssembly.Module(bytes), {m: {memory}}).exports.memory;
    assertTrue(memory === reexportedMemory);
    assertSame(4 * pageSize, reexportedMemory.toResizableBuffer().maxByteLength);
})();

(function testSharedMemory() {
    const memory = new WebAssembly.Memory({initial: 1, maximum: 4, shared: true});
    const original = memory.buffer;
    assertFalse(original.growable);
    assertTrue(original === memory.toFixedLengthBuffer());

    const growable = memory.toResizableBuffer();
    assertFalse(original === growable);
    assertFalse(original.growable);
    assertSame(pageSize, original.byteLength);
    assertTrue(growable.growable);
    assertSame(4 * pageSize, growable.maxByteLength);
    assertTrue(growable === memory.buffer);
    assertTrue(growable === memory.toResizableBuffer());

    assertThrows(() => growable.grow(pageSize + 1), RangeError);
    growable.grow(2 * pageSize);
    assertTrue(growable === memory.buffer);
    assertSame(2 * pageSize, growable.byteLength);
    assertSame(2, memory.grow(1));
    assertTrue(growable === memory.buffer);
    assertSame(3 * pageSize, growable.byteLength);

    const fixed = memory.toFixedLengthBuffer();
    assertFalse(fixed === growable);
    assertFalse(fixed.growable);
    assertSame(3 * pageSize, fixed.byteLength);
    assertSame(3 * pageSize, growable.byteLength);
    assertTrue(fixed === memory.buffer);
    assertTrue(fixed === memory.toFixedLengthBuffer());

    const currentGrowable = memory.toResizableBuffer();
    assertFalse(currentGrowable === growable);
    currentGrowable.grow(4 * pageSize);
    assertSame(4 * pageSize, growable.byteLength);
    assertSame(4 * pageSize, currentGrowable.byteLength);
    assertSame(3 * pageSize, fixed.byteLength);
    assertTrue(currentGrowable === memory.buffer);
})();

(function testSharedMemoryGrowthWithFixedCurrentBuffer() {
    const memory = new WebAssembly.Memory({initial: 1, maximum: 4, shared: true});
    const growable = memory.toResizableBuffer();
    const fixed = memory.toFixedLengthBuffer();

    assertSame(1, memory.grow(1));
    assertSame(2 * pageSize, growable.byteLength);
    assertSame(pageSize, fixed.byteLength);
    assertSame(2 * pageSize, memory.buffer.byteLength);
})();

(function testSharedMemoryAliasesHaveSameDataBlock() {
    const memory = new WebAssembly.Memory({initial: 1, maximum: 4, shared: true});
    const sourceBuffer = memory.buffer;
    memory.toResizableBuffer();
    const targetBuffer = memory.toFixedLengthBuffer();
    assertFalse(sourceBuffer === targetBuffer);

    const speciesDescriptor = Object.getOwnPropertyDescriptor(SharedArrayBuffer, Symbol.species);
    Object.defineProperty(SharedArrayBuffer, Symbol.species, {
        configurable: true,
        value: function() {
            return targetBuffer;
        },
    });
    try {
        assertThrows(() => sourceBuffer.slice(0, 1), TypeError);
    } finally {
        Object.defineProperty(SharedArrayBuffer, Symbol.species, speciesDescriptor);
    }

    const source = new Uint16Array(sourceBuffer, 0, 3);
    source.set([1, 2, 3]);
    const target = new Uint8Array(targetBuffer, 0, 6);
    const littleEndian = new Uint8Array(new Uint16Array([1]).buffer)[0] === 1;
    const targetOffset = littleEndian ? 2 : 3;
    const expected = Array.from(target);
    for (let i = 0; i < source.length; i++) {
        expected[targetOffset + i] = source[i];
    }
    target.set(source, targetOffset);
    assertSameContent(expected, target);
})();

(function testSharedMemoryConversionsPreserveWaiters() {
    const memory = new WebAssembly.Memory({initial: 1, maximum: 4, shared: true});
    const oldBuffer = memory.buffer;
    const worker = new Worker(`
        onmessage = function (event) {
            postMessage("waiting");
            postMessage(Atomics.wait(new Int32Array(event.data), 0, 0, 10000));
        };
    `, {type: "string"});
    worker.postMessage(oldBuffer);
    assertSame("waiting", worker.getMessage());

    const currentView = new Int32Array(memory.toResizableBuffer());
    const deadline = Date.now() + 10000;
    let notified = 0;
    while (notified === 0 && Date.now() < deadline) {
        notified = Atomics.notify(currentView, 0);
    }
    assertSame(1, notified);
    assertSame("ok", worker.getMessage());
    worker.terminate();
})();

(function testClonedGrowableSharedBufferRetainsMemoryAssociation() {
    const memory = new WebAssembly.Memory({initial: 1, maximum: 4, shared: true});
    const buffer = memory.toResizableBuffer();
    const worker = new Worker(`
        onmessage = function (event) {
            const {buffer, memory} = event.data;
            if (buffer !== memory.buffer) {
                throw new Error("cloned buffer is not associated with the cloned memory");
            }
            buffer.grow(2 * ${pageSize});
            if (buffer.byteLength !== 2 * ${pageSize} || memory.grow(0) !== 2) {
                throw new Error("growing the cloned buffer did not grow the Wasm memory");
            }
            postMessage("done");
        };
    `, {type: "string"});
    worker.postMessage({buffer, memory});
    assertSame("done", worker.getMessage());
    worker.terminate();
    assertSame(2 * pageSize, buffer.byteLength);
    assertSame(2, memory.grow(0));
})();

(function testClonedSharedMemoryPreservesBufferIdentityAndCurrentness() {
    const distinctMemory = new WebAssembly.Memory({initial: 1, maximum: 4, shared: true});
    const a = distinctMemory.toResizableBuffer();
    distinctMemory.toFixedLengthBuffer();
    const b = distinctMemory.toResizableBuffer();

    const fixedMemory = new WebAssembly.Memory({initial: 1, maximum: 4, shared: true});
    const oldGrowableBuffer = fixedMemory.toResizableBuffer();
    const currentFixedBuffer = fixedMemory.toFixedLengthBuffer();

    const growableMemory = new WebAssembly.Memory({initial: 1, maximum: 4, shared: true});
    growableMemory.toResizableBuffer();

    const worker = new Worker(`
        onmessage = function (event) {
            const {distinct, fixedCurrent, growableMemory} = event.data;
            if (distinct.a === distinct.b || distinct.b !== distinct.memory.buffer) {
                throw new Error("growable buffer identity was not preserved");
            }
            if (fixedCurrent.currentFixedBuffer !== fixedCurrent.memory.buffer ||
                            fixedCurrent.oldGrowableBuffer === fixedCurrent.memory.buffer) {
                throw new Error("the current fixed buffer was not preserved");
            }
            if (!Object.isFrozen(fixedCurrent.currentFixedBuffer)) {
                throw new Error("the cloned fixed memory buffer is not frozen");
            }
            if (!growableMemory.buffer.growable) {
                throw new Error("a memory-only clone lost its growable current buffer");
            }
            postMessage("done");
        };
    `, {type: "string"});
    worker.postMessage({
        distinct: {a, b, memory: distinctMemory},
        fixedCurrent: {oldGrowableBuffer, currentFixedBuffer, memory: fixedMemory},
        growableMemory,
    });
    assertSame("done", worker.getMessage());
    worker.terminate();
})();

(function testAtomicsCallbackBeforeMemoryExport() {
    const bytes = new Uint8Array([
        0x00, 0x61, 0x73, 0x6d, 0x01, 0x00, 0x00, 0x00,
        0x01, 0x04, 0x01, 0x60, 0x00, 0x00,
        0x03, 0x02, 0x01, 0x00,
        0x05, 0x04, 0x01, 0x03, 0x01, 0x04,
        0x07, 0x0a, 0x01, 0x06, 0x6d, 0x65, 0x6d, 0x6f, 0x72, 0x79, 0x02, 0x00,
        0x08, 0x01, 0x00,
        0x0a, 0x0d, 0x01, 0x0b, 0x00, 0x41, 0x00, 0x41, 0x00, 0xfe, 0x00, 0x02, 0x00, 0x1a, 0x0b,
    ]);
    const memory = new WebAssembly.Instance(new WebAssembly.Module(bytes)).exports.memory;
    assertSame(4 * pageSize, memory.toResizableBuffer().maxByteLength);
})();
