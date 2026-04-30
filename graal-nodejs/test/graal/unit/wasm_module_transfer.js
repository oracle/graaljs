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

const assert = require('assert');
const { spawnSync } = require('child_process');

const CHILD_ARG = '--run-wasm-module-transfer-regression';

if (process.argv.includes(CHILD_ARG)) {
    runRegression();
} else if (typeof WebAssembly !== 'undefined') {
    describe('WebAssembly.Module transfer', function () {
        it.skipOnNode('does not share Wasm interop call targets between workers', function () {
            this.timeout(120000);

            const result = spawnSync(process.execPath, [
                '--vm.ea',
                ...process.execArgv,
                '--experimental-options',
                '--engine.DisableCodeSharing=true',
                __filename,
                CHILD_ARG
            ], {
                encoding: 'utf8'
            });

            assert.strictEqual(result.status, 0,
                'child process failed\nstdout:\n' + result.stdout + '\nstderr:\n' + result.stderr);
        });
    });
}

function runRegression() {
    const { Worker } = require('worker_threads');

    const ATTEMPTS = 8;
    const WORKERS = 4;
    const FUNCTIONS = 128;
    const PARAMS = 8;

    // Minimal unsigned LEB128 encoding helpers for building a Wasm binary directly in JS.
    function u32(value) {
        const out = [];
        do {
            let byte = value & 0x7f;
            value >>>= 7;
            if (value !== 0) {
                byte |= 0x80;
            }
            out.push(byte);
        } while (value !== 0);
        return out;
    }

    function str(s) {
        const out = u32(s.length);
        for (let i = 0; i < s.length; i++) {
            out.push(s.charCodeAt(i));
        }
        return out;
    }

    function section(id, bytes) {
        return [id, ...u32(bytes.length), ...bytes];
    }

    function makeModule(attempt) {
        const bytes = [0x00, 0x61, 0x73, 0x6d, 0x01, 0x00, 0x00, 0x00];

        // Vary source bytes across attempts without changing the Wasm behavior.
        bytes.push(...section(0, [...str('attempt'), ...u32(attempt)]));

        // Wasm caches the JS interop adapter per function type, so many distinct
        // function types give the test independent chances to hit cross-layer reuse.
        const types = u32(FUNCTIONS);
        for (let i = 0; i < FUNCTIONS; i++) {
            types.push(0x60, ...u32(PARAMS));
            for (let p = 0; p < PARAMS; p++) {
                types.push(((i >>> p) & 1) ? 0x7c : 0x7f);
            }
            types.push(0x01, 0x7f);
        }
        bytes.push(...section(1, types));

        const funcs = u32(FUNCTIONS);
        for (let i = 0; i < FUNCTIONS; i++) {
            funcs.push(...u32(i));
        }
        bytes.push(...section(3, funcs));

        const exports = u32(FUNCTIONS);
        for (let i = 0; i < FUNCTIONS; i++) {
            exports.push(...str('f' + i), 0x00, ...u32(i));
        }
        bytes.push(...section(7, exports));

        const code = u32(FUNCTIONS);
        for (let i = 0; i < FUNCTIONS; i++) {
            const body = [0x00, 0x41, 0x00, 0x0b];
            code.push(...u32(body.length), ...body);
        }
        bytes.push(...section(10, code));

        return new Uint8Array(bytes);
    }

    const workerSource = `
        const { parentPort } = require('worker_threads');
        const FUNCTIONS = ${FUNCTIONS};
        const PARAMS = ${PARAMS};
        const args = Array(PARAMS).fill(1);

        parentPort.on('message', message => {
            const ia = new Int32Array(message.sab);
            const instance = new WebAssembly.Instance(message.module);
            const exports = instance.exports;
            parentPort.postMessage('ready');

            while (Atomics.load(ia, 0) === 0) {
                Atomics.wait(ia, 0, 0);
            }

            for (let i = 0; i < FUNCTIONS; i++) {
                const r = exports['f' + i](...args);
                if (r !== 0) {
                    throw new Error('bad result ' + i + ': ' + r);
                }
            }
            parentPort.postMessage('done');
        });
    `;

    function waitForMessage(worker, expected) {
        return new Promise((resolve, reject) => {
            function cleanup() {
                worker.off('message', onMessage);
                worker.off('error', onError);
                worker.off('exit', onExit);
            }

            function onMessage(message) {
                cleanup();
                if (message === expected) {
                    resolve();
                } else {
                    reject(new Error('expected ' + expected + ', got ' + message));
                }
            }

            function onError(error) {
                cleanup();
                reject(error);
            }

            function onExit(code) {
                cleanup();
                reject(new Error('worker exited before ' + expected + ' with code ' + code));
            }

            worker.once('message', onMessage);
            worker.once('error', onError);
            worker.once('exit', onExit);
        });
    }

    async function runAttempt(attempt) {
        const module = new WebAssembly.Module(makeModule(attempt));
        const sab = new SharedArrayBuffer(4);
        const ia = new Int32Array(sab);
        const workers = [];
        const ready = [];

        try {
            for (let i = 0; i < WORKERS; i++) {
                const worker = new Worker(workerSource, { eval: true });
                workers.push(worker);
                ready.push(waitForMessage(worker, 'ready'));
                worker.postMessage({ module, sab });
            }

            await Promise.all(ready);
            const done = workers.map(worker => waitForMessage(worker, 'done'));
            Atomics.store(ia, 0, 1);
            Atomics.notify(ia, 0, WORKERS);
            await Promise.all(done);
        } finally {
            await Promise.all(workers.map(worker => worker.terminate()));
        }
    }

    (async function main() {
        for (let attempt = 0; attempt < ATTEMPTS; attempt++) {
            await runAttempt(attempt);
        }
    })().catch(error => {
        console.error(error && error.stack || error);
        process.exit(1);
    });
}
