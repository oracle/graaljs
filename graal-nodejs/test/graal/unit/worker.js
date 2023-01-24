/*
 * Copyright (c) 2018, 2023, Oracle and/or its affiliates. All rights reserved.
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
var module = require('./_unit');

const {
    Worker,
    isMainThread
} = require('worker_threads');

describe('Worker', function () {
    if (module.hasJavaInterop()) {
        it('terminate should terminate Thread.sleep()', function (done) {
            var worker = new Worker('java.lang.Thread.sleep(1000000)', {eval: true});
            worker.on('online', function () {
                setTimeout(function () {
                    worker.terminate().then(() => done());
                }, 1000);
            });
        }).timeout(5000);

        it('The Main thread can load classes from the classpath', function (done) {
            if (isMainThread) {
                const JavaAsyncClass = Java.type('com.oracle.truffle.js.test.threading.JavaAsyncTaskScheduler.Example');
                done();
            }
        }).timeout(5000);

        it('A Worker thread can load classes from the classpath, too', function (done) {
            if (isMainThread) {
                let w = new Worker(`
                                const {
                                    parentPort
                                } = require('worker_threads');

                                parentPort.on('message', (m) => {
                                    const JavaAsyncClass = Java.type('com.oracle.truffle.js.test.threading.JavaAsyncTaskScheduler.Example');
                                    parentPort.postMessage('ok!');
                                });
                `, {
                    eval: true
                });
                w.on('message', (m) => {
                    assert(m === 'ok!');
                    w.terminate().then(()=>{done()});
                });
                w.postMessage('ignore me');
            }
        }).timeout(5000);

        it('The Main thread can load classes from the classpath and nest context calls', function (done) {
            if (isMainThread) {
                const JavaAsyncClass = Java.type('com.oracle.truffle.js.test.threading.JavaAsyncTaskScheduler.Example');
                const num = JavaAsyncClass.evalJsCode();
                assert(num === 42);
                done();
            }
        }).timeout(5000);

        it('A Worker thread can load classes from the classpath and nest context calls, too', function (done) {
            if (isMainThread) {
                let w = new Worker(`
                                const {
                                    parentPort
                                } = require('worker_threads');
                                const assert = require('assert');

                                parentPort.on('message', (m) => {
                                    const JavaAsyncClass = Java.type('com.oracle.truffle.js.test.threading.JavaAsyncTaskScheduler.Example');
                                    const num = JavaAsyncClass.evalJsCode();
                                    assert(num === 42);
                                    parentPort.postMessage(num);
                                });
                `, {
                    eval: true
                });
                w.on('message', (m) => {
                    assert(m === 42);
                    w.terminate().then(()=>{done()});
                });
                w.postMessage('ignore me');
            }
        }).timeout(5000);

    }
});
