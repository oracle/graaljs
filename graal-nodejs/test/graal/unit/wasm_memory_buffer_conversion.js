/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The Universal Permissive License (UPL), Version 1.0
 *
 * Subject to the condition set forth below, permission is hereby granted to any
 * person obtaining a copy of this software, associated documentation and/or
 * data (collectively the "Software"), free of charge and under any and all
 * copyright rights in the Software, and any and all patent rights owned or
 * freely licensable by each licensor hereunder covering either (i) the
 * unmodified Software as contributed to or provided by such licensor, or (ii)
 * the Larger Works (as defined below), to deal in both
 *
 * (a) the Software, and
 *
 * (b) any piece of software and/or hardware listed in the lrgrwrks.txt file if
 * one is included with the Software each a "Larger Work" to which the Software
 * is contributed by such licensors),
 *
 * without restriction, including without limitation the rights to copy, create
 * derivative works of, display, perform, and distribute the Software and make,
 * use, sell, offer for sale, import, export, have made, and have sold the
 * Software and the Larger Work(s), and to sublicense the foregoing rights on
 * either these or other terms.
 *
 * This license is subject to the following condition:
 *
 * The above copyright notice and either this complete permission notice or at a
 * minimum a reference to the UPL must be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

'use strict';

const assert = require('assert');
const { Worker } = require('worker_threads');

if (typeof WebAssembly !== 'undefined') {
  describe('WebAssembly.Memory buffer conversion', function() {
    const PAGE_SIZE = 64 * 1024;
    const WORKER_TEST_TIMEOUT = 20000;

    function runWorker(source, value) {
      return new Promise((resolve, reject) => {
        const worker = new Worker(source, { eval: true });
        worker.once('error', (error) => {
          worker.terminate();
          reject(error);
        });
        worker.once('message', (message) => {
          worker.terminate().then(() => resolve(message), reject);
        });
        worker.postMessage(value);
      });
    }

    it('implements fixed-length and resizable buffer conversion', function() {
      assert.strictEqual(WebAssembly.Memory.prototype.toFixedLengthBuffer.length, 0);
      assert.strictEqual(WebAssembly.Memory.prototype.toResizableBuffer.length, 0);
      assert.throws(() => WebAssembly.Memory.prototype.toFixedLengthBuffer.call({}), TypeError);
      assert.throws(() => WebAssembly.Memory.prototype.toResizableBuffer.call({}), TypeError);

      const noMaximum = new WebAssembly.Memory({ initial: 1 });
      const noMaximumBuffer = noMaximum.buffer;
      assert.strictEqual(noMaximumBuffer, noMaximum.toFixedLengthBuffer());
      assert.throws(() => noMaximum.toResizableBuffer(), TypeError);
      assert.strictEqual(noMaximum.buffer, noMaximumBuffer);
      assert.strictEqual(noMaximum.grow(1), 1);
      assert.strictEqual(noMaximumBuffer.detached, true);
      assert.strictEqual(noMaximum.buffer.byteLength, 2 * PAGE_SIZE);

      const memory = new WebAssembly.Memory({ initial: 1, maximum: 4 });
      const original = memory.buffer;
      assert.strictEqual(original.resizable, false);

      const resizable = memory.toResizableBuffer();
      assert.strictEqual(original.detached, true);
      assert.strictEqual(resizable.resizable, true);
      assert.strictEqual(resizable.maxByteLength, 4 * PAGE_SIZE);
      assert.strictEqual(resizable, memory.buffer);
      assert.strictEqual(resizable, memory.toResizableBuffer());

      const view = new Uint8Array(resizable);
      view[0] = 42;
      assert.throws(() => resizable.resize(PAGE_SIZE + 1), RangeError);
      resizable.resize(2 * PAGE_SIZE);
      assert.strictEqual(resizable.byteLength, 2 * PAGE_SIZE);
      assert.strictEqual(view[0], 42);
      assert.throws(() => resizable.resize(PAGE_SIZE), RangeError);

      assert.strictEqual(memory.grow(1), 2);
      assert.strictEqual(resizable, memory.buffer);
      assert.strictEqual(resizable.byteLength, 3 * PAGE_SIZE);
      assert.strictEqual(view[0], 42);

      const fixed = memory.toFixedLengthBuffer();
      assert.strictEqual(resizable.detached, true);
      assert.strictEqual(fixed.resizable, false);
      assert.strictEqual(fixed.byteLength, 3 * PAGE_SIZE);
      assert.strictEqual(fixed, memory.buffer);
      assert.strictEqual(fixed, memory.toFixedLengthBuffer());

      assert.strictEqual(memory.grow(1), 3);
      assert.strictEqual(fixed.detached, true);
      assert.strictEqual(memory.buffer.byteLength, 4 * PAGE_SIZE);

      const maximum32BitPages = new WebAssembly.Memory({ initial: 1, maximum: 65536 });
      const maximum32BitPagesBuffer = maximum32BitPages.toResizableBuffer();
      assert.strictEqual(maximum32BitPagesBuffer.maxByteLength, 4294967296);

      const sharedMaximum32BitPages = new WebAssembly.Memory({ initial: 1, maximum: 65536, shared: true });
      const sharedMaximum32BitPagesBuffer = sharedMaximum32BitPages.toResizableBuffer();
      assert.strictEqual(sharedMaximum32BitPagesBuffer.maxByteLength, 4294967296);
      if (typeof Graal === 'object') {
        assert.throws(() => maximum32BitPagesBuffer.resize(2 ** 31), RangeError);
        assert.strictEqual(maximum32BitPagesBuffer.byteLength, PAGE_SIZE);
        assert.throws(() => sharedMaximum32BitPagesBuffer.grow(2 ** 31), RangeError);
        assert.strictEqual(sharedMaximum32BitPagesBuffer.byteLength, PAGE_SIZE);
      }
    });

    it('preserves backing capacity when cloning a resizable Wasm buffer', async function() {
      const memory = new WebAssembly.Memory({ initial: 1, maximum: 4 });
      const buffer = memory.toResizableBuffer();
      const result = await runWorker(`
        const { parentPort } = require('worker_threads');
        parentPort.once('message', (buffer) => {
          buffer.resize(2 * ${PAGE_SIZE});
          const view = new Uint8Array(buffer);
          view[${PAGE_SIZE}] = 42;
          parentPort.postMessage([buffer.byteLength, view[${PAGE_SIZE}]]);
        });
      `, buffer);
      assert.deepStrictEqual(result, [2 * PAGE_SIZE, 42]);
    }).timeout(WORKER_TEST_TIMEOUT);

    it('preserves length-tracking views when cloning a resizable Wasm buffer', async function() {
      const memory = new WebAssembly.Memory({ initial: 1, maximum: 4 });
      const buffer = memory.toResizableBuffer();
      const view = new Uint8Array(buffer, PAGE_SIZE - 8);
      assert.strictEqual(view.length, 8);
      const length = await runWorker(`
        const { parentPort } = require('worker_threads');
        parentPort.once('message', (view) => {
          view.buffer.resize(2 * ${PAGE_SIZE});
          parentPort.postMessage(view.length);
        });
      `, view);
      assert.strictEqual(length, PAGE_SIZE + 8);
    }).timeout(WORKER_TEST_TIMEOUT);

    it('uses the declared maximum of module-defined and re-exported memories', function() {
      function exportedMemory(memorySection) {
        const header = [0x00, 0x61, 0x73, 0x6d, 0x01, 0x00, 0x00, 0x00];
        const memoryExport = [0x07, 0x0a, 0x01, 0x06, 0x6d, 0x65, 0x6d, 0x6f, 0x72, 0x79, 0x02, 0x00];
        const module = new WebAssembly.Module(new Uint8Array([...header, ...memorySection, ...memoryExport]));
        return new WebAssembly.Instance(module).exports.memory;
      }

      const noMaximum = exportedMemory([0x05, 0x03, 0x01, 0x00, 0x01]);
      assert.throws(() => noMaximum.toResizableBuffer(), TypeError);

      const maximum65536 = exportedMemory([0x05, 0x06, 0x01, 0x01, 0x01, 0x80, 0x80, 0x04]);
      assert.strictEqual(maximum65536.toResizableBuffer().maxByteLength, 4294967296);

      const bytes = new Uint8Array([
        0x00, 0x61, 0x73, 0x6d, 0x01, 0x00, 0x00, 0x00,
        0x02, 0x0e, 0x01, 0x01, 0x6d, 0x06, 0x6d, 0x65, 0x6d, 0x6f, 0x72, 0x79, 0x02, 0x01, 0x01, 0x05,
        0x07, 0x0a, 0x01, 0x06, 0x6d, 0x65, 0x6d, 0x6f, 0x72, 0x79, 0x02, 0x00,
      ]);
      const memory = new WebAssembly.Memory({ initial: 1, maximum: 4 });
      const reexportedMemory = new WebAssembly.Instance(new WebAssembly.Module(bytes), { m: { memory } }).exports.memory;
      assert.strictEqual(memory, reexportedMemory);
      assert.strictEqual(reexportedMemory.toResizableBuffer().maxByteLength, 4 * PAGE_SIZE);
    });

    it('converts shared memory buffers and updates every growable alias', function() {
      const memory = new WebAssembly.Memory({ initial: 1, maximum: 4, shared: true });
      const original = memory.buffer;
      assert.strictEqual(original.growable, false);
      assert.strictEqual(original, memory.toFixedLengthBuffer());

      const growable = memory.toResizableBuffer();
      assert.notStrictEqual(original, growable);
      assert.strictEqual(original.growable, false);
      assert.strictEqual(original.byteLength, PAGE_SIZE);
      assert.strictEqual(growable.growable, true);
      assert.strictEqual(growable.maxByteLength, 4 * PAGE_SIZE);
      assert.strictEqual(growable, memory.buffer);
      assert.strictEqual(growable, memory.toResizableBuffer());

      assert.throws(() => growable.grow(PAGE_SIZE + 1), RangeError);
      growable.grow(2 * PAGE_SIZE);
      assert.strictEqual(growable, memory.buffer);
      assert.strictEqual(growable.byteLength, 2 * PAGE_SIZE);
      assert.strictEqual(memory.grow(1), 2);
      assert.strictEqual(growable, memory.buffer);
      assert.strictEqual(growable.byteLength, 3 * PAGE_SIZE);

      const fixed = memory.toFixedLengthBuffer();
      assert.notStrictEqual(fixed, growable);
      assert.strictEqual(fixed.growable, false);
      assert.strictEqual(fixed.byteLength, 3 * PAGE_SIZE);
      assert.strictEqual(growable.byteLength, 3 * PAGE_SIZE);
      assert.strictEqual(fixed, memory.buffer);
      assert.strictEqual(fixed, memory.toFixedLengthBuffer());

      const currentGrowable = memory.toResizableBuffer();
      assert.notStrictEqual(currentGrowable, growable);
      currentGrowable.grow(4 * PAGE_SIZE);
      assert.strictEqual(growable.byteLength, 4 * PAGE_SIZE);
      assert.strictEqual(currentGrowable.byteLength, 4 * PAGE_SIZE);
      assert.strictEqual(fixed.byteLength, 3 * PAGE_SIZE);
      assert.strictEqual(currentGrowable, memory.buffer);

      const memoryWithFixedCurrent = new WebAssembly.Memory({ initial: 1, maximum: 4, shared: true });
      const oldGrowable = memoryWithFixedCurrent.toResizableBuffer();
      const currentFixed = memoryWithFixedCurrent.toFixedLengthBuffer();
      assert.strictEqual(memoryWithFixedCurrent.grow(1), 1);
      assert.strictEqual(oldGrowable.byteLength, 2 * PAGE_SIZE);
      assert.strictEqual(currentFixed.byteLength, PAGE_SIZE);
      assert.strictEqual(memoryWithFixedCurrent.buffer.byteLength, 2 * PAGE_SIZE);
    });

    it('treats shared Wasm buffer aliases as the same data block', function() {
      const memory = new WebAssembly.Memory({ initial: 1, maximum: 4, shared: true });
      const sourceBuffer = memory.buffer;
      memory.toResizableBuffer();
      const targetBuffer = memory.toFixedLengthBuffer();
      assert.notStrictEqual(sourceBuffer, targetBuffer);

      const speciesDescriptor = Object.getOwnPropertyDescriptor(SharedArrayBuffer, Symbol.species);
      Object.defineProperty(SharedArrayBuffer, Symbol.species, {
        configurable: true,
        value: function() {
          return targetBuffer;
        },
      });
      try {
        assert.throws(() => sourceBuffer.slice(0, 1), TypeError);
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
      assert.deepStrictEqual(Array.from(target), expected);
    });

    it('preserves waiter state across shared Wasm buffer conversions', function() {
      return new Promise((resolve, reject) => {
        const memory = new WebAssembly.Memory({ initial: 1, maximum: 4, shared: true });
        const oldBuffer = memory.buffer;
        const worker = new Worker(`
          const { parentPort } = require('worker_threads');
          parentPort.once('message', (buffer) => {
            parentPort.postMessage('waiting');
            parentPort.postMessage(Atomics.wait(new Int32Array(buffer), 0, 0, 10000));
          });
        `, { eval: true });

        let notifyTimer;
        function fail(error) {
          clearInterval(notifyTimer);
          worker.terminate();
          reject(error);
        }

        worker.once('error', fail);
        worker.on('message', (message) => {
          if (message === 'waiting') {
            const currentView = new Int32Array(memory.toResizableBuffer());
            notifyTimer = setInterval(() => {
              if (Atomics.notify(currentView, 0) === 1) {
                clearInterval(notifyTimer);
              }
            }, 10);
          } else {
            try {
              assert.strictEqual(message, 'ok');
              worker.terminate().then(resolve, reject);
            } catch (error) {
              fail(error);
            }
          }
        });
        worker.postMessage(oldBuffer);
      });
    }).timeout(WORKER_TEST_TIMEOUT);

    it('retains the Wasm association of a cloned growable shared buffer', async function() {
      const memory = new WebAssembly.Memory({ initial: 1, maximum: 4, shared: true });
      const buffer = memory.toResizableBuffer();
      const result = await runWorker(`
        const { parentPort } = require('worker_threads');
        parentPort.once('message', ({ buffer, memory }) => {
          if (buffer !== memory.buffer) {
            throw new Error('cloned buffer is not associated with the cloned memory');
          }
          buffer.grow(2 * ${PAGE_SIZE});
          parentPort.postMessage([buffer.byteLength, memory.grow(0)]);
        });
      `, { buffer, memory });
      assert.deepStrictEqual(result, [2 * PAGE_SIZE, 2]);
      assert.strictEqual(buffer.byteLength, 2 * PAGE_SIZE);
      assert.strictEqual(memory.grow(0), 2);
    }).timeout(WORKER_TEST_TIMEOUT);

    it('preserves shared memory buffer identity and currentness when cloning', async function() {
      const distinctMemory = new WebAssembly.Memory({ initial: 1, maximum: 4, shared: true });
      const a = distinctMemory.toResizableBuffer();
      distinctMemory.toFixedLengthBuffer();
      const b = distinctMemory.toResizableBuffer();

      const fixedMemory = new WebAssembly.Memory({ initial: 1, maximum: 4, shared: true });
      const oldGrowableBuffer = fixedMemory.toResizableBuffer();
      const currentFixedBuffer = fixedMemory.toFixedLengthBuffer();

      const growableMemory = new WebAssembly.Memory({ initial: 1, maximum: 4, shared: true });
      growableMemory.toResizableBuffer();

      const memoryFirst = new WebAssembly.Memory({ initial: 1, maximum: 4, shared: true });
      const memoryFirstBuffer = memoryFirst.toResizableBuffer();

      const result = await runWorker(`
        const { parentPort } = require('worker_threads');
        parentPort.once('message', ({ distinct, fixedCurrent, growableMemory, memoryFirst }) => {
          const fixedCurrentPreserved = fixedCurrent.currentFixedBuffer === fixedCurrent.memory.buffer &&
              fixedCurrent.oldGrowableBuffer !== fixedCurrent.memory.buffer;
          const fixedFrozen = Object.isFrozen(fixedCurrent.currentFixedBuffer);
          fixedCurrent.memory.grow(1);
          parentPort.postMessage({
            distinct: distinct.a !== distinct.b && distinct.b === distinct.memory.buffer,
            fixedCurrent: fixedCurrentPreserved,
            fixedFrozen,
            oldGrowableByteLength: fixedCurrent.oldGrowableBuffer.byteLength,
            fixedByteLength: fixedCurrent.currentFixedBuffer.byteLength,
            growableCurrent: growableMemory.buffer.growable,
            memoryFirst: memoryFirst.buffer === memoryFirst.memory.buffer,
          });
        });
      `, {
        distinct: { a, b, memory: distinctMemory },
        fixedCurrent: { oldGrowableBuffer, currentFixedBuffer, memory: fixedMemory },
        growableMemory,
        memoryFirst: { memory: memoryFirst, buffer: memoryFirstBuffer },
      });
      assert.deepStrictEqual(result, {
        distinct: true,
        fixedCurrent: true,
        fixedFrozen: true,
        oldGrowableByteLength: 2 * PAGE_SIZE,
        fixedByteLength: PAGE_SIZE,
        growableCurrent: true,
        memoryFirst: true,
      });
    }).timeout(WORKER_TEST_TIMEOUT);

    it('obtains the maximum before a module-defined memory is exported', function() {
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
      assert.strictEqual(memory.toResizableBuffer().maxByteLength, 4 * PAGE_SIZE);
    });
  });
}
