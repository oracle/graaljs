/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

var assert = require('assert');
var module = require('./_unit');

if (typeof WebAssembly !== 'undefined') {
  describe('WebAssembly', function () {
    describe('Memory', function () {
      const PAGE_SIZE = 64 * 1024;

      it('can access and grow memory using memory.buffer and memory.grow()', function () {
        let memory = new WebAssembly.Memory({ initial: 4 });
        assert.strictEqual(memory.buffer.byteLength, 4 * PAGE_SIZE);

        let array = new Uint8Array(memory.buffer);
        array[0] = 42;

        array = new Uint8Array(memory.buffer);
        assert.strictEqual(array[0], 42);
        array[0] = 43;

        memory.grow(1);
        assert.strictEqual(memory.buffer.byteLength, 5 * PAGE_SIZE);

        array = new Uint8Array(memory.buffer);
        assert.strictEqual(array[0], 43);
      }).timeout(5000);

      it('can get backing store of a memory and continue to use it after grow()', function () {
        let memory = new WebAssembly.Memory({ initial: 4 });

        let array = new Uint8Array(memory.buffer);
        array[0] = 42;

        let calls = 0;
        assert.ok(module.WasmMemory_CheckBackingStore(memory.buffer, (len, elem) => {
          if (calls == 0) {
            assert.strictEqual(len, 4 * PAGE_SIZE);
            assert.strictEqual(elem, 42);

            memory.grow(1);

            // no change, already detached
            array[0] = 43;

            // update new memory
            array = new Uint8Array(memory.buffer);
            assert.strictEqual(array[0], 42);
            array[0] = 44;
          } else if (calls == 1) {
            // still using old backing store
            // V8 may grow the wasm memory in place, so we allow that as well
            if (len == 4 * PAGE_SIZE) {
              // grow with copy
              assert.strictEqual(len, 4 * PAGE_SIZE);
              assert.strictEqual(elem, 42);
            } else {
              // grow in place
              assert.strictEqual(len, 5 * PAGE_SIZE);
              assert.strictEqual(elem, 44);
            }
          } else if (calls == 2) {
            // GetBackingStore() of detached buffer
            assert.strictEqual(len, 0);
            assert.strictEqual(elem, undefined);
          }
          calls++;
        }));
        assert.strictEqual(calls, 3);

        assert.ok(module.WasmMemory_CheckBackingStore(memory.buffer, (len, elem) => {
          if (calls == 3) {
            // new, grown backing store
            assert.strictEqual(len, 5 * PAGE_SIZE);
            assert.strictEqual(elem, 44);
          }
          calls++;
        }));
        assert.strictEqual(calls, 6);
      }).timeout(5000);

      it('Buffer() should return the underlying ArrayBuffer', function() {
        let memory = new WebAssembly.Memory({ initial: 1 });
        assert.strictEqual(module.WasmMemory_Buffer(memory), memory.buffer);
      });

      it('can be sent to a Worker', function(done) {
        const { Worker } = require('worker_threads');

        const memory = new WebAssembly.Memory({initial: 1, maximum: 1, shared: true});

        const w = new Worker(`
            const { parentPort } = require('worker_threads');

            let memory;

            const notify = function() {
                const result = Atomics.notify(new Int32Array(memory.buffer), 0);
                if (result === 1) {
                    parentPort.postMessage('done');
                } else {
                    // There is a tiny chance that the waiting in the other
                    // thread haven't started yet => try again later
                    setTimeout(notify, 100);
                }
            };

            parentPort.on('message', (message) => {
                memory = message;
                notify();
            });`,
        {
            eval: true
        });

        w.on('message', (message) => {
            assert.strictEqual(message, 'done');
            w.terminate().then(() => done());
        });

        w.postMessage(memory);

        assert.strictEqual(Atomics.wait(new Int32Array(memory.buffer), 0, 0), 'ok');
      });
    });
  });
}
