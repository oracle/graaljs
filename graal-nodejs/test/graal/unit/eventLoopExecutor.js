/*
 * Copyright (c) 2024, 2024, Oracle and/or its affiliates. All rights reserved.
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
const unitTest = require('./_unit');

const {
    Worker,
    isMainThread
} = require('worker_threads');

describe('eventLoopExecutor', function () {
    if (unitTest.hasJavaInterop()) {
        const eventLoopExecutor = require('node:graal').eventLoopExecutor;
        const EventLoopExecutorTest = Java.type('com.oracle.truffle.trufflenode.test.EventLoopExecutorTest');

        it('refuses null Runnable', function () {
            assert.ok(EventLoopExecutorTest.testNullRunnable(eventLoopExecutor));
        });

        it('allows asynchronous resolution of promises', function (done) {
            const { promise, resolve } = Promise.withResolvers();
            promise.then(result => {
                assert.strictEqual(result, 42);
                done();
            });
            EventLoopExecutorTest.testAsyncResolution(eventLoopExecutor, resolve);
        }).timeout(10000);

        it('works in a worker', function (done) {
            const w = new Worker(`
                const {
                    parentPort
                } = require('worker_threads');
                const assert = require('assert');

                const { promise, resolve } = Promise.withResolvers();
                promise.then(result => {
                    assert.strictEqual(result, 42);
                    parentPort.postMessage('done');
                });

                // keep the Worker alive
                parentPort.on('message', (m) => assert.fail('Unexpected message: ' + m));

                const eventLoopExecutor = require('node:graal').eventLoopExecutor;
                const EventLoopExecutorTest = Java.type('com.oracle.truffle.trufflenode.test.EventLoopExecutorTest');
                EventLoopExecutorTest.testAsyncResolution(eventLoopExecutor, resolve);`,
            {
                eval: true
            });
            w.on('message', () => {
                w.terminate().then(()=>{done()});
            });
        }).timeout(10000);

        it('refuses to post to a terminated worker', function (done) {
            const w = new Worker(`
                const {
                    parentPort
                } = require('worker_threads');

                const eventLoopExecutor = require('node:graal').eventLoopExecutor;
                parentPort.postMessage(eventLoopExecutor);`,
            {
                eval: true
            });
            w.on('message', (workerExecutor) => {
                w.terminate().then(() => {
                    assert.ok(EventLoopExecutorTest.testFinishedEventLoop(workerExecutor));
                    done();
                });
            });
        }).timeout(10000);
    }
});
