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
const childProcess = require('child_process');
const http2 = require('http2');

const RUN_CHILD_ARG = '--run-http2-baseobject-weak-race-child';

const DEFAULT_ARGS = [
    45000, // durationMs
    48,    // clientsPerTurn
    384,   // maxActiveClients
    1,     // streamsPerClient
    4,     // controlFramesPerClient
    2,     // clientCloseDelayMs
    10,    // serverCloseDelayMs
    64,    // tickBacklogPerTurn
    1024,  // pressureObjectsPerTurn
    512,   // pressureStringSize
    2000,  // reportEveryMs
];

if (process.argv.includes(RUN_CHILD_ARG)) {
    runStress(process.argv.slice(process.argv.indexOf(RUN_CHILD_ARG) + 1));
} else {
    describe.skip('HTTP/2 BaseObject weak-reference race', function () {
        const args = stressArgs();
        const repeats = Number(process.env.GRAAL_NODEJS_HTTP2_WEAK_STRESS_REPEATS || 1);
        const durationMs = args[0];

        this.timeout(repeats * (durationMs + 20000));

        it('should not run a BaseObject weak callback while the native object is strongly referenced', function () {
            for (let i = 0; i < repeats; i++) {
                const result = childProcess.spawnSync(
                    process.execPath,
                    [__filename, RUN_CHILD_ARG].concat(args.map(String)),
                    {
                        encoding: 'utf8',
                        maxBuffer: 64 * 1024 * 1024,
                        timeout: durationMs + 20000,
                    });
                assert.strictEqual(formatChildFailure(result), null);
            }
        });
    });
}

function stressArgs() {
    if (!process.env.GRAAL_NODEJS_HTTP2_WEAK_STRESS_ARGS) {
        return DEFAULT_ARGS.slice();
    }
    const args = process.env.GRAAL_NODEJS_HTTP2_WEAK_STRESS_ARGS.trim().split(/\s+/).map(Number);
    assert.strictEqual(args.length, DEFAULT_ARGS.length);
    assert.ok(args.every(Number.isFinite));
    return args;
}

function formatChildFailure(result) {
    if (result.error) {
        return result.error.message;
    }
    if (result.status === 0 && result.signal === null) {
        return null;
    }
    return [
        `status=${result.status} signal=${result.signal}`,
        'stdout:',
        result.stdout,
        'stderr:',
        result.stderr,
    ].join('\n');
}

function runStress(argv) {
    const [
        durationMs = DEFAULT_ARGS[0],
        clientsPerTurn = DEFAULT_ARGS[1],
        maxActiveClients = DEFAULT_ARGS[2],
        streamsPerClient = DEFAULT_ARGS[3],
        controlFramesPerClient = DEFAULT_ARGS[4],
        clientCloseDelayMs = DEFAULT_ARGS[5],
        serverCloseDelayMs = DEFAULT_ARGS[6],
        tickBacklogPerTurn = DEFAULT_ARGS[7],
        pressureObjectsPerTurn = DEFAULT_ARGS[8],
        pressureStringSize = DEFAULT_ARGS[9],
        reportEveryMs = DEFAULT_ARGS[10],
    ] = argv.map(Number);

    const noop = () => {};
    const endAt = Date.now() + durationMs;
    let activeClients = 0;
    let createdClients = 0;
    let closedClients = 0;
    let createdStreams = 0;
    let closedStreams = 0;
    let submittedPings = 0;
    let submittedSettings = 0;
    let turns = 0;
    let maxObservedActiveClients = 0;
    let pressureSink;
    let lastReportAt = Date.now();
    let lastReportClients = 0;

    const server = http2.createServer();

    server.on('session', (session) => {
        session.on('error', noop);
        for (let i = 0; i < controlFramesPerClient; i++) {
            try {
                session.settings({ enablePush: false }, noop);
            } catch {}
        }
        setTimeout(() => {
            try {
                session.close();
            } catch {}
        }, serverCloseDelayMs).unref();
    });

    server.on('stream', (stream) => {
        stream.on('error', noop);
        stream.respond({ ':status': 204 });
        stream.end();
    });

    server.listen(0, '127.0.0.1', () => {
        const url = `http://localhost:${server.address().port}`;
        setTimeout(() => turn(url), 1);
    });

    function turn(url) {
        turns++;
        for (let i = 0; i < clientsPerTurn && activeClients < maxActiveClients; i++) {
            createClient(url);
        }

        if (activeClients > maxObservedActiveClients) {
            maxObservedActiveClients = activeClients;
        }

        allocatePressure();
        queueTickBacklog();
        maybeReport();

        if (Date.now() < endAt) {
            setTimeout(() => turn(url), 1);
            return;
        }

        server.close();
        setTimeout(() => {
            console.log(JSON.stringify({
                durationMs,
                clientsPerTurn,
                maxActiveClients,
                streamsPerClient,
                controlFramesPerClient,
                clientCloseDelayMs,
                serverCloseDelayMs,
                tickBacklogPerTurn,
                pressureObjectsPerTurn,
                pressureStringSize,
                createdClients,
                closedClients,
                createdStreams,
                closedStreams,
                submittedPings,
                submittedSettings,
                turns,
                maxObservedActiveClients,
                activeClients,
            }));
            process.exit(0);
        }, 5000).unref();
    }

    function createClient(url) {
        activeClients++;
        createdClients++;

        const client = http2.connect(url);
        client.on('error', noop);
        client.on('goaway', noop);
        client.on('close', () => {
            activeClients--;
            closedClients++;
        });

        client.once('connect', () => {
            for (let i = 0; i < controlFramesPerClient; i++) {
                try {
                    client.ping(Buffer.alloc(8, i & 0xff), noop);
                    submittedPings++;
                } catch {}

                try {
                    client.settings({ enablePush: false }, noop);
                    submittedSettings++;
                } catch {}
            }

            for (let i = 0; i < streamsPerClient; i++) {
                const req = client.request({ ':path': `/${createdClients}/${i}` });
                createdStreams++;
                req.on('error', noop);
                req.on('response', noop);
                req.on('close', () => closedStreams++);
                req.resume();
                req.end();
            }

            setTimeout(() => {
                try {
                    client.close();
                } catch {
                    try {
                        client.destroy();
                    } catch {}
                }
            }, clientCloseDelayMs).unref();
        });
    }

    function allocatePressure() {
        const arr = new Array(pressureObjectsPerTurn);
        for (let i = 0; i < arr.length; i++) {
            arr[i] = {
                i,
                data: 'x'.repeat(pressureStringSize),
                buf: Buffer.allocUnsafe(128),
            };
        }
        pressureSink = arr;
    }

    function queueTickBacklog() {
        for (let i = 0; i < tickBacklogPerTurn; i++) {
            if ((i & 1) === 0) {
                process.nextTick(noop);
            } else {
                Promise.resolve().then(noop);
            }
        }
    }

    function maybeReport() {
        const now = Date.now();
        if (now - lastReportAt < reportEveryMs) {
            return;
        }

        const elapsedMs = now - (endAt - durationMs);
        const intervalSec = (now - lastReportAt) / 1000;
        const clientsPerSec = (createdClients - lastReportClients) / intervalSec;
        lastReportAt = now;
        lastReportClients = createdClients;

        console.error(JSON.stringify({
            elapsedMs,
            createdClients,
            closedClients,
            activeClients,
            createdStreams,
            closedStreams,
            submittedPings,
            submittedSettings,
            turns,
            maxObservedActiveClients,
            clientsPerSec: Math.round(clientsPerSec),
        }));
    }
}
