/*
 * Copyright (c) 2021, 2024, Oracle and/or its affiliates. All rights reserved.
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
var fs = require('fs');
var module = require('./_unit');
var Worker = require('worker_threads').Worker;

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
          if (calls === 0) {
            assert.strictEqual(len, 4 * PAGE_SIZE);
            assert.strictEqual(elem, 42);

            memory.grow(1);

            // no change, already detached
            array[0] = 43;

            // update new memory
            array = new Uint8Array(memory.buffer);
            assert.strictEqual(array[0], 42);
            array[0] = 44;
          } else if (calls === 1) {
            // still using old backing store
            // V8 may grow the wasm memory in place, so we allow that as well
            if (len === 4 * PAGE_SIZE) {
              // grow with copy
              assert.strictEqual(len, 4 * PAGE_SIZE);
              assert.strictEqual(elem, 42);
            } else {
              // grow in place
              assert.strictEqual(len, 5 * PAGE_SIZE);
              assert.strictEqual(elem, 44);
            }
          } else if (calls === 2) {
            // GetBackingStore() of detached buffer
            assert.strictEqual(len, 0);
            assert.strictEqual(elem, undefined);
          }
          calls++;
        }));
        assert.strictEqual(calls, 3);

        assert.ok(module.WasmMemory_CheckBackingStore(memory.buffer, (len, elem) => {
          if (calls === 3) {
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
      
      const wasmModuleBuilderCode = fs.readFileSync('../../../graal-nodejs/deps/v8/test/mjsunit/wasm/wasm-module-builder.js');
      const NUM_ITERATIONS = 50000;

      it('works with a native mutex', function(done) {
        let memory = new WebAssembly.Memory({initial: 1, maximum: 1, shared: true});

        let moduleCode = `${wasmModuleBuilderCode}
        let builder = new WasmModuleBuilder();
        builder.addImportedMemory("env", "imported_mem", 1, 1, "shared");
        // Try to lock a mutex at the given address.
        // Returns 1 if the mutex was successfully locked, and 0 otherwise.
        builder.addFunction("tryLockMutex", kSig_i_i)
            .addBody([
            kExprLocalGet, 0,   // mutex address
            ...wasmI32Const(0), // expected value (0 => unlocked)
            ...wasmI32Const(1), // replacement value (1 => locked)
            kAtomicPrefix,
            kExprI32AtomicCompareExchange, 2, 0,
            kExprI32Eqz])
            .exportFunc();
        // Lock a mutex at the given address, retrying until successful.
        builder.addFunction("lockMutex", kSig_v_i)
            .addBody([
            kExprBlock, kWasmVoid,
                kExprLoop, kWasmVoid,
                    // Try to lock the mutex. $tryLockMutex returns 1 if the mutex
                    // was locked, and 0 otherwise.
                    kExprLocalGet, 0,
                    kExprCallFunction, 0,
                    kExprBrIf, 1,
                    // Wait for the other agent to finish with mutex.
                    kExprLocalGet, 0,    // mutex address
                    ...wasmI32Const(1),  // expected value (1 => locked)
                    ...wasmI64Const(-1), // infinite timeout
                    kAtomicPrefix,
                    kExprI32AtomicWait, 2, 0,
                    // Ignore the result and try to acquire the mutex again.
                    kExprDrop,
                    kExprBr, 0,
                kExprEnd,
            kExprEnd])
            .exportFunc();
        // Unlock a mutex at the given address.
        builder.addFunction("unlockMutex", kSig_v_i)
            .addBody([
            // Unlock the mutex.
            kExprLocalGet, 0,   // mutex address
            ...wasmI32Const(0), // 0 => unlocked
            kAtomicPrefix,
            kExprI32AtomicStore, 2, 0,
            // Notify one agent that is waiting on this lock.
            kExprLocalGet, 0,   // mutex address
            ...wasmI32Const(1), // notify 1 waiter
            kAtomicPrefix,
            kExprAtomicNotify, 2, 0,
            kExprDrop])
            .exportFunc();
        // Unsafe non-atomic increment, which must be guarded by a mutex.
        builder.addFunction("increment", kSig_v_i)
            .addBody([
            kExprLocalGet, 0,
            kExprLocalGet, 0,
            kExprI32LoadMem, 2, 0,
            ...wasmI32Const(1),
            kExprI32Add,
            kExprI32StoreMem, 2, 0])
            .exportFunc();
        let moduleBytes = builder.toBuffer();
        globalThis.kWasmOpcodeNames = undefined;
        new WebAssembly.Module(moduleBytes)`;

        let module = eval(moduleCode);

        const WORKER_COUNT = 2;
        let completedWorkers = 0;
        for (let workerNo = 0; workerNo < WORKER_COUNT; workerNo++) {
            const w = new Worker(`
                const { parentPort } = require('worker_threads');

                parentPort.on('message', (message) => {
                    let module = message.module;
                    let memory = message.memory;
                    let instance = new WebAssembly.Instance(module, {env: {imported_mem: memory}});
                    for (let i = 0; i < ${NUM_ITERATIONS}; i++) {
                        instance.exports.lockMutex(0);
                        instance.exports.increment(4);
                        instance.exports.unlockMutex(0);
                    }
                    parentPort.postMessage('done');
                });`,
            {
                eval: true
            });

            w.on('message', (message) => {
                assert.strictEqual(message, 'done');
                w.terminate();
                if (++completedWorkers === WORKER_COUNT) {
                    let i32a = new Int32Array(memory.buffer);
                    assert.strictEqual(i32a[0], 0);  // mutex unlocked
                    assert.strictEqual(i32a[1], 2 * NUM_ITERATIONS); // all increments reflected
                    done();
                }
            });

            w.postMessage({module: module, memory: memory});
        }
      }).timeout(10000);

      it('works with a fast mutex', function(done) {
        let memory = new WebAssembly.Memory({initial: 1, maximum: 1, shared: true});

        let moduleCode = `${wasmModuleBuilderCode}
        let builder = new WasmModuleBuilder();
        builder.addImportedMemory("env", "imported_mem", 1, 1, "shared");
        builder.addFunction("lockMutex", kSig_v_i)
            .addLocals(kWasmI32, 1)
            .addBody([
                kExprBlock, kWasmVoid,
                    kExprLocalGet, 0,
                    ...wasmI32Const(0),
                    ...wasmI32Const(1),
                    kAtomicPrefix,
                    kExprI32AtomicCompareExchange, 2, 0,
                    kExprLocalSet, 1,
                    kExprLocalGet, 1,
                    kExprI32Eqz,
                    kExprBrIf, 0,
                    kExprLoop, kWasmVoid,
                        kExprLocalGet, 1,
                        ...wasmI32Const(2),
                        kExprI32Eq,
                        kExprLocalGet, 0,
                        ...wasmI32Const(1),
                        ...wasmI32Const(2),
                        kAtomicPrefix,
                        kExprI32AtomicCompareExchange, 2, 0,
                        kExprI32Eqz,
                        kExprI32Eqz,
                        kExprI32Ior,
                        kExprIf, kWasmVoid,
                            kExprLocalGet, 0,
                            ...wasmI32Const(2),
                            ...wasmI64Const(-1),
                            kAtomicPrefix,
                            kExprI32AtomicWait, 2, 0,
                            kExprDrop,
                        kExprEnd,
                        kExprLocalGet, 0,
                        ...wasmI32Const(0),
                        ...wasmI32Const(2),
                        kAtomicPrefix,
                        kExprI32AtomicCompareExchange, 2, 0,
                        kExprLocalSet, 1,
                        kExprLocalGet, 1,
                        kExprI32Eqz,
                        kExprI32Eqz,
                        kExprBrIf, 0,
                    kExprEnd,
                kExprEnd])
            .exportFunc();
        builder.addFunction("unlockMutex", kSig_v_i)
            .addBody([
                kExprLocalGet, 0,
                ...wasmI32Const(1),
                kAtomicPrefix,
                kExprI32AtomicSub, 2, 0,
                ...wasmI32Const(1),
                kExprI32Eq,
                kExprI32Eqz,
                kExprIf, kWasmVoid,
                    kExprLocalGet, 0,
                    ...wasmI32Const(0),
                    kAtomicPrefix,
                    kExprI32AtomicStore, 2, 0,
                    kExprLocalGet, 0,
                    ...wasmI32Const(1),
                    kAtomicPrefix,
                    kExprAtomicNotify, 2, 0,
                    kExprDrop,
                kExprEnd])
            .exportFunc();
        builder.addFunction("increment", kSig_v_i)
            .addBody([
                kExprLocalGet, 0,
                kExprLocalGet, 0,
                kExprI32LoadMem, 2, 0,
                ...wasmI32Const(1),
                kExprI32Add,
                kExprI32StoreMem, 2, 0])
            .exportFunc();
        let moduleBytes = builder.toBuffer();
        globalThis.kWasmOpcodeNames = undefined;
        new WebAssembly.Module(moduleBytes)`;

        let module = eval(moduleCode);

        const WORKER_COUNT = 2;
        let completedWorkers = 0;
        for (let workerNo = 0; workerNo < WORKER_COUNT; workerNo++) {
            const w = new Worker(`
                const { parentPort } = require('worker_threads');

                parentPort.on('message', (message) => {
                    let module = message.module;
                    let memory = message.memory;
                    let instance = new WebAssembly.Instance(module, {env: {imported_mem: memory}});
                    for (let i = 0; i < ${NUM_ITERATIONS}; i++) {
                        instance.exports.lockMutex(0);
                        instance.exports.increment(4);
                        instance.exports.unlockMutex(0);
                    }
                    parentPort.postMessage('done');
                });`,
            {
                eval: true
            });

            w.on('message', (message) => {
                assert.strictEqual(message, 'done');
                w.terminate();
                if (++completedWorkers === WORKER_COUNT) {
                    let i32a = new Int32Array(memory.buffer);
                    assert.strictEqual(i32a[0], 0);  // mutex unlocked
                    assert.strictEqual(i32a[1], 2 * NUM_ITERATIONS); // all increments reflected
                    done();
                }
            });

            w.postMessage({module: module, memory: memory});
        }
      }).timeout(10000);

      it('works with an atomic increment', function(done) {
        let memory = new WebAssembly.Memory({initial: 1, maximum: 1, shared: true});

        let moduleCode = `${wasmModuleBuilderCode}
        let builder = new WasmModuleBuilder();
        builder.addImportedMemory("env", "imported_mem", 1, 1, "shared");
        builder.addFunction("increment", kSig_i_ii)
            .addBody([
            kExprLocalGet, 0,
            kExprLocalGet, 1,
            kAtomicPrefix,
            kExprI32AtomicAdd, 2, 0])
            .exportFunc();
        let moduleBytes = builder.toBuffer();
        globalThis.kWasmOpcodeNames = undefined;
        new WebAssembly.Module(moduleBytes)`;

        let module = eval(moduleCode);

        const WORKER_COUNT = 2;
        let completedWorkers = 0;
        for (let workerNo = 0; workerNo < WORKER_COUNT; workerNo++) {
            const w = new Worker(`
                const { parentPort } = require('worker_threads');

                parentPort.on('message', (message) => {
                    let module = message.module;
                    let memory = message.memory;
                    let instance = new WebAssembly.Instance(module, {env: {imported_mem: memory}});
                    for (let i = 0; i < ${NUM_ITERATIONS}; i++) {
                        instance.exports.increment(0, 1);
                    }
                    parentPort.postMessage('done');
                });`,
            {
                eval: true
            });

            w.on('message', (message) => {
                assert.strictEqual(message, 'done');
                w.terminate();
                if (++completedWorkers === WORKER_COUNT) {
                    let i32a = new Int32Array(memory.buffer);
                    assert.strictEqual(i32a[0], 2 * NUM_ITERATIONS); // all increments reflected
                    done();
                }
            });

            w.postMessage({module: module, memory: memory});
        }
      }).timeout(10000);
    });
  });
}
