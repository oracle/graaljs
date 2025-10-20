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

var assert = require('assert');
var module = require('./_unit');

const {
    Worker,
    MessagePort,
    isMainThread,
    workerData
} = require('worker_threads')


describe('Java interop messages', function() {
    this.timeout(20000);
    if (!module.hasJavaInterop()) {
        // no interop
        return;
    }
    it('can share and use Java interop objects', function(done) {
        if (isMainThread) {
            var A = Java.type('java.util.concurrent.atomic.AtomicInteger');
            var atomic = new A();
            let w = new Worker(`
                            const {
                                parentPort
                            } = require('worker_threads');

                            parentPort.on('message', (m) => {
                                const val = m.incrementAndGet();
                                parentPort.postMessage(val);
                            });
            `, {
                eval: true
            });
            w.on('message', (m) => {
                assert(m === 1);
                const val = atomic.incrementAndGet();
                assert(val === 2);
                w.terminate().then(()=>{done()});
            });
            w.postMessage(atomic);
        }
    });
    it('can send more than one message', function(done) {
        if (isMainThread) {
            var A = Java.type('java.util.concurrent.atomic.AtomicInteger');
            let w = new Worker(`
                            const {
                                parentPort
                            } = require('worker_threads');

                            parentPort.on('message', (m) => {
                                const val = m.incrementAndGet();
                                parentPort.postMessage(val);
                            });
            `, {
                eval: true
            });
            var atomic = new A(0);
            var received = 0;
            w.on('message', (m) => {
                assert(m === ++received);
                var atomic = new A(m);
                if (m === 10) {
                    w.terminate().then(()=>{done()});
                } else {
                    w.postMessage(atomic);
                }
            });
            w.postMessage(atomic);
        }
    });
    it('can nest Java objects', function(done) {
        if (isMainThread) {
            var A = Java.type('java.util.concurrent.atomic.AtomicInteger');
            let w = new Worker(`
                            const {
                                parentPort
                            } = require('worker_threads');

                            parentPort.on('message', (m) => {
                                const val = m.counter.incrementAndGet();
                                parentPort.postMessage(val);
                            });
            `, {
                eval: true
            });
            var atomic = new A(0);
            var received = 0;
            w.on('message', (m) => {
                assert(m === ++received);
                var atomic = new A(m);
                if (m === 10) {
                    w.terminate().then(()=>{done()});
                } else {
                    w.postMessage({
                        counter: atomic
                    });
                }
            });
            w.postMessage({
                counter: atomic
            });
        }
    });
    it('can share with multiple workers', function(done) {
        if (isMainThread) {
            const M = Java.type('java.util.concurrent.ConcurrentHashMap');
            const map = new M();
            const workersNum = 3;
            var received = 0;
            for (var worker = 0; worker < workersNum; worker++) {
                let w = new Worker(`
                                const {
                                    parentPort,
                                    workerData,
                                } = require('worker_threads');

                                parentPort.on('message', (m) => {
                                    m.put(workerData, true);
                                    parentPort.postMessage('OK');
                                });
                `, {
                    workerData: 42 + worker,
                    eval: true
                });
                w.on('message', (m) => {
                    if (++received === workersNum) {
                        assert(map.size() === workersNum);
                        for (var i = 0; i < workersNum; i++) {
                            assert(map.get(42 + i) === true);
                        }
                        w.terminate().then(()=>{done()});
                    } else {
                        w.terminate();
                    }
                });
                w.postMessage(map);
            }
        }
    });
    it('workers can send Java objects back to main', function(done) {
        if (isMainThread) {
            let w = new Worker(`
                            const {
                                parentPort
                            } = require('worker_threads');

                            parentPort.on('message', (m) => {
                                var A = Java.type('java.util.concurrent.atomic.AtomicInteger');
                                var atomic = new A(m);
                                parentPort.postMessage(atomic);
                            });
            `, {
                eval: true
            });
            w.on('message', (m) => {
                const val = m.incrementAndGet();
                assert(val === 42);
                w.terminate().then(()=>{done()});
            });
            w.postMessage(41);
        }
    });
    it('Child workers can send messages, too', function(done) {
        if (isMainThread) {
            let w = new Worker(`
                            const {
                                parentPort
                            } = require('worker_threads');

                            var point = Java.type('java.awt.Point');
                            parentPort.postMessage({point:new point(40, 2)});
            `, {
                eval: true
            });
            w.on('message', (m) => {
                const val = m.point.x + m.point.y;
                assert(val === 42);
                w.terminate().then(()=>{done()});
            });
        }
    });
    it('Can send messageports and close them', function(done) {
        if (isMainThread) {
            const worker = new Worker(`
                            const {
                                parentPort,
                                MessageChannel
                            } = require('worker_threads');

                            var channel = new MessageChannel();

                            channel.port1.postMessage(Java.type('java.awt.Point'));
                            parentPort.postMessage(channel.port1, [channel.port1]);
                            parentPort.postMessage(channel.port2, [channel.port2]);
                `, {
                eval: true
            });
            var ports = [];
            worker.on('message', function(port) {
                ports.push(port);
            });
            worker.on('exit', function() {
                for (var port of ports) {
                    port.on('message', function(message) {
                        const point = new message(40, 2);
                        assert(point.x + point.y === 42);
                        ports.map(p => p.unref());
                        done();
                    });
                }
            });
        }
    });
    it('can send Long and BigInteger messages', function(done) {
        if (isMainThread) {
            const worker = new Worker(`
                            const {
                                parentPort
                            } = require('worker_threads');

                            parentPort.postMessage(java.lang.Long.MAX_VALUE);
                            parentPort.postMessage(java.math.BigInteger.valueOf(java.lang.Long.MAX_VALUE).shiftLeft(1));
                `, {
                eval: true
            });
            let received = 0;
            worker.on('message', function(value) {
                switch (received++) {
                    case 0:
                        assert.equal(java.lang.Long.MAX_VALUE, value);
                        assert.equal(2n ** 63n - 1n, value);
                        break;
                    case 1:
                        assert.equal(2n ** 64n - 2n, value);
                        worker.terminate().then(()=>{done()});
                        break;
                }
            });
        }
    });
    it('Java Refs encoding order is respected', function(done) {
        if (isMainThread) {
            var A = Java.type('java.util.concurrent.atomic.AtomicInteger');
            var a1 = new A(1);
            var a2 = new A(2);
            var a3 = new A(3);
            let w = new Worker(`
                            const {
                                parentPort
                            } = require('worker_threads');
                            const assert = require('assert');

                            parentPort.on('message', (m) => {
                                assert(m.a1.incrementAndGet() === 2);
                                assert(m.a2.incrementAndGet() === 3);
                                assert(m.a3.a3.incrementAndGet() === 4);
                                parentPort.postMessage(m);
                            });
            `, {
                eval: true
            });
            w.on('message', (m) => {
                assert(m.a1.get() === 2);
                assert(m.a2.get() === 3);
                assert(m.a3.a3.get() === 4);
                w.terminate().then(()=>{done()});
            });
            w.postMessage({a1:a1, a2:a2, a3:{a3:a3}});
        }
    });
    it('Multiple objects can be shared', function(done) {
        if (isMainThread) {
            let w = new Worker(`
                            const {
                                parentPort
                            } = require('worker_threads');

                            parentPort.on('message', (m) => {
                                const x = m.random.nextInt(100);
                                const y = m.random.nextInt(100);
                                const Point = Java.type('java.awt.Point');
                                parentPort.postMessage(new Point(x, y));
                            });
            `, {
                eval: true
            });
            const JavaRandom = Java.type('java.util.Random');
            w.postMessage({random : new JavaRandom()});
            w.on('message', (point) => {
                const val = point.x + point.y;
                assert(val >= 0 && val < 200);
                w.terminate().then(()=>{done()});
            });
        }
    });
    it('Java can schedule back to the main Node.js event loop using a (blocking) worker', function(done) {
        if (isMainThread) {
            const JavaAsyncClass = Java.type('com.oracle.truffle.js.test.threading.JavaAsyncTaskScheduler.Example');
            function javaAsyncNotifier() {
                this.queue = new java.util.concurrent.LinkedBlockingDeque();
                this.worker = new Worker(`
                                    const {
                                        workerData,
                                        parentPort
                                    } = require('worker_threads');

                                    while (true) {
                                        // block the worker waiting for the next notification from Java
                                        var data = workerData.queue.take();
                                        // notify the main event loop that we got new data
                                        parentPort.postMessage(data);
                                    }`,
                {eval: true, workerData: { queue: this.queue }});
            }
            // register a callback to be executed when something is added to the shared queue
            const asyncJavaEvents = new javaAsyncNotifier();
            asyncJavaEvents.worker.on('message', (n) => {
                assert(n >= 42);
                asyncJavaEvents.worker.terminate().then(()=>{done()});
            });
            // pass the queue to Java, to give a way to notify us back when data is available
            new JavaAsyncClass(asyncJavaEvents.queue).doAsynchronousWork();
        }
    });
});
