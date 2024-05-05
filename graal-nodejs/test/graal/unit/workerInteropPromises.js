/*
 * Copyright (c) 2019, 2019, Oracle and/or its affiliates. All rights reserved.
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
const {
    Worker,
    isMainThread,
} = require('worker_threads')

class PromiseWorker {

    constructor(runnableFunction) {
        // For every incoming message, the worker executes a given user-defined
        // function and returns the result back to the main worker.
        this.worker = new Worker(`
                            const {
                                parentPort
                            } = require('worker_threads');
                            const assert = require('assert');
                            const runnableFunction = eval('${runnableFunction}');
                            assert(typeof runnableFunction === 'function');

                            parentPort.on('message', m => {
                                const resolveRejectPair = m.resolveRejectPair;
                                const data = m.data;
                                try {
                                    const result = runnableFunction(data);
                                    parentPort.postMessage({result:result, resolveRejectPair:resolveRejectPair});
                                } catch (e) {
                                    parentPort.postMessage({error:e.toString(), resolveRejectPair:resolveRejectPair});
                                }
                            });
            `, {
                eval: true
            });
        this.worker.on('message', m => {
            const promiseResolve = m.resolveRejectPair[0];
            const promiseReject = m.resolveRejectPair[1];
            if (m.error) {
                promiseReject(m.error);
            } else {
                promiseResolve(m.result);
            }
        });
    }

    compute(num) {
        // Using GraalVM's Java-to-JS interop, one can use a Java array to "wrap" the Promise's reject and resolve functions.
        // In this way, they can be "borrowed" to a worker. Note that calling reject or resolve from the worker thread would
        // result in an exception, because JS functions can be executed only by the thread that created them. Nevertheless,
        // they can be sent back to the main worker, who will safely call them to resolve (or reject) a promise. In this way,
        // there is no need to maintain an explicit mapping between worker messages and the corresponding promises.
        const JavaArray = Java.type('java.lang.Object[]');
        const worker = this.worker;
        return new Promise((resolve, reject) => {
            const wrapper = new JavaArray(2);
            wrapper[0] = resolve;
            wrapper[1] = reject;
            worker.postMessage({data:num, resolveRejectPair:wrapper});
        });
    }

    kill() {
        this.worker.terminate();
    }
}

describe('Java interop can be used to map promises to worker messages', () => {
    if (typeof java === 'undefined') {
        // No interop
        return;
    }

    it('Can use a promise to await on a worker incoming message.', async () => {
        if (isMainThread) {
            const worker = new PromiseWorker(`n => n + 42`);
            var sum = 0;
            var expected = 0;
            for (var i = 0; i < 100; i++) {
                sum = sum + await worker.compute(i);
                expected += i + 42;
            }
            assert(expected === sum);
            worker.kill();
        }
    }).timeout(15000);

    it('Worker failures reject promises.', async () => {
        if (isMainThread) {
            const worker = new PromiseWorker(`m => {throw m;}`);
            var hit = false;
            try {
                var result = await worker.compute('will fail');
            } catch (e) {
                assert(e === 'will fail');
                hit = true;
            }
            assert(hit);
            worker.kill();
        }
    }).timeout(15000);
});
