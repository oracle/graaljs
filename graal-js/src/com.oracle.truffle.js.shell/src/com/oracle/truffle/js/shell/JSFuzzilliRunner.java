/*
 * Copyright (c) 2019, 2025, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.shell;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Consumer;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Source;

class JSFuzzilliRunner {

    private static final int TIMEOUT_EXIT_CODE = 254;
    private static final int OUT_OF_MEMORY_EXIT_CODE = 253;
    private static final LogLevel logLevel = LogLevel.severe;

    private enum LogLevel {
        none,
        severe,
        info,
        debug
    }

    @SuppressWarnings("deprecation")
    public static int runFuzzilliREPRL(Context.Builder contextBuilder) {
        try (RandomAccessFile controlReadF = new RandomAccessFile(new File("/dev/fd/100"), "rws");
                        RandomAccessFile controlWriteF = new RandomAccessFile(new File("/dev/fd/101"), "rws");
                        RandomAccessFile dataReadF = new RandomAccessFile(new File("/dev/fd/102"), "rws")) {
            log(LogLevel.info, "GraalJS Fuzzilli REPRL started");
            Consumer<String> fuzzout = (String s) -> {
                try {
                    try (RandomAccessFile dataWriteF = new RandomAccessFile(new File("/dev/fd/103"), "rws")) {
                        dataWriteF.write(s.getBytes(StandardCharsets.UTF_8));
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            };
            Runnable crash = () -> {
                throw new ThreadDeath();
            };
            final byte[] helo = new byte[]{'H', 'E', 'L', 'O'};
            final byte[] response = new byte[8];
            final byte[] writeExitCode = new byte[4];
            log(LogLevel.debug, "writing HELO...");
            controlWriteF.write(helo);
            if (!expectResponse(controlReadF, response, "HELO")) {
                log(LogLevel.severe, "Invalid HELO response from parent: " + Arrays.toString(response));
                return -1;
            }
            int status = -1;
            String script = null;
            Timer timer = new Timer(true);
            for (;;) {
                try {
                    if (!expectResponse(controlReadF, response, "exec")) {
                        log(LogLevel.severe, "Unknown action: " + Arrays.toString(response));
                        return -1;
                    }
                    log(LogLevel.debug, "trying to read scriptSize");
                    controlReadF.read(response);
                    int scriptSize = (int) ByteBuffer.wrap(response).order(ByteOrder.LITTLE_ENDIAN).getLong();
                    log(LogLevel.debug, "got scriptSize: " + scriptSize);
                    log(LogLevel.debug, "Reading script of length " + scriptSize);
                    byte[] scriptChars = new byte[scriptSize];
                    int n = dataReadF.read(scriptChars);
                    if (n != scriptSize) {
                        log(LogLevel.severe, String.format(Locale.ROOT, "ERROR: read %d bytes, but scriptSize was %d", n, scriptSize));
                    } else {
                        log(LogLevel.debug, String.format(Locale.ROOT, "got %d bytes", n));
                    }
                    script = new String(scriptChars, StandardCharsets.UTF_8);
                    log(LogLevel.debug, "Got script:");
                    log(LogLevel.debug, script);
                    try (Context context = contextBuilder.build()) {
                        context.getBindings("js").putMember("__fuzzout__", fuzzout);
                        context.getBindings("js").putMember("crash", crash);
                        TimerTask canceller = new TimerTask() {
                            @Override
                            public void run() {
                                context.close(true);
                            }
                        };
                        timer.schedule(canceller, 1000);
                        context.eval(Source.newBuilder("js", script, "fuzzilliInput").build());
                        canceller.cancel();
                    }
                    status = 0;
                } catch (PolyglotException e) {
                    if (e.isExit()) {
                        // normal exit() calls are fine and not to be treated as errors
                        status = 0;
                    } else if (e.isCancelled()) {
                        log(LogLevel.info, "TIMEOUT");
                        // special exit code for timeout
                        status = TIMEOUT_EXIT_CODE;
                    } else if (e.isSyntaxError()) {
                        status = 7;
                    } else if (!e.isInternalError()) {
                        status = 7;
                    } else {
                        if (e.getMessage().startsWith("java.lang.OutOfMemoryError")) {
                            // special exit code for OutOfMemoryErrors
                            sendExitCode(controlWriteF, writeExitCode, OUT_OF_MEMORY_EXIT_CODE);
                            return 1;
                        }
                        log(LogLevel.severe, "INTERNAL ERROR POLYGLOT EXCEPTION");
                        printStackTrace(e);
                        // e is an internal error, treat as crash in the fuzzer
                        status = -1;
                    }
                } catch (OutOfMemoryError e) {
                    // special exit code for OutOfMemoryErrors
                    sendExitCode(controlWriteF, writeExitCode, OUT_OF_MEMORY_EXIT_CODE);
                    return 1;
                } catch (Throwable t) {
                    log(LogLevel.severe, "NON-POLYGLOT EXCEPTION");
                    printStackTrace(t);
                    // all non-polyglot exceptions should be treated as crashes by the fuzzer
                    status = -1;
                }
                log(LogLevel.debug, "Sending exit code " + status);
                sendExitCode(controlWriteF, writeExitCode, status);
            }
        } catch (Throwable t) {
            log(LogLevel.severe, "OUTER THROWABLE");
            printStackTrace(t);
            return -1;
        }
    }

    private static void sendExitCode(final RandomAccessFile controlWriteF, final byte[] writeExitCode, int statusCode) throws IOException {
        int status = statusCode << 8;
        writeExitCode[0] = (byte) (status & 0xff);
        writeExitCode[1] = (byte) ((status >>> 8) & 0xff);
        writeExitCode[2] = (byte) ((status >>> 16) & 0xff);
        writeExitCode[3] = (byte) ((status >>> 24) & 0xff);
        controlWriteF.write(writeExitCode);
    }

    private static boolean expectResponse(RandomAccessFile source, final byte[] buf, String expected) throws IOException {
        log(LogLevel.debug, "trying to read " + expected + "...");
        int bytesRead = source.read(buf, 0, expected.length());
        if (bytesRead != expected.length()) {
            log(LogLevel.severe, "fuzzilliExpectResponse: no input available, got " + bytesRead + " bytes");
            return false;
        }
        log(LogLevel.debug, "fuzzilliExpectResponse: got response: " + new String(buf, 0, expected.length(), StandardCharsets.UTF_8));
        for (int i = 0; i < expected.length(); i++) {
            if (buf[i] != expected.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    private static void printStackTrace(Throwable t) {
        t.printStackTrace(System.out);
        System.out.flush();
    }

    private static void log(LogLevel level, String msg) {
        if (logLevel.ordinal() >= level.ordinal()) {
            System.out.println(msg);
            System.out.flush();
        }
    }
}
