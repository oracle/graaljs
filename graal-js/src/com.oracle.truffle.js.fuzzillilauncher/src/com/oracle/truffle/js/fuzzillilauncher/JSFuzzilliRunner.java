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
package com.oracle.truffle.js.fuzzillilauncher;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.io.UncheckedIOException;
import java.lang.reflect.Constructor;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;

import org.graalvm.collections.Pair;
import org.graalvm.nativeimage.c.function.CFunction;
import org.graalvm.nativeimage.c.function.CFunction.Transition;
import org.graalvm.nativeimage.c.function.CLibrary;

/**
 * Implements the REPRL (read-eval-print-reset-loop) loop required by Fuzzilli.
 */
public class JSFuzzilliRunner {

    public static class FuzzilliHelper implements ProxyExecutable {
        public FileOutputStream dataWriteF;

        public FuzzilliHelper(FileOutputStream dataWriteF) {
            this.dataWriteF = dataWriteF;
        }

        @Override
        public final Object execute(Value... args) {
            var cmd = args[0].asString();
            var arg = args[1];
            if (cmd.equals("FUZZILLI_PRINT")) {
                try {
                    byte[] x = String.format("%s\n", arg.toString()).getBytes(StandardCharsets.UTF_8);
                    if (dataWriteF != null) {
                        dataWriteF.write(x);
                    }
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            } else if (cmd.equals("FUZZILLI_CRASH") && arg.isNumber()) {
                switch (arg.asInt()) {
                    case 0:
                        fuzzilliSegfault();
                        break;
                    default:
                        throw new ThreadDeath();
                }
            }
            return null;
        }
    }

    // Hard coded file descriptors (the below comment is from the fuzzilli repo)
    // Well-known file descriptor numbers for reprl <-> child communication, child process side
    private static final int REPRL_CHILD_CTRL_IN = 100;
    private static final int REPRL_CHILD_CTRL_OUT = 101;
    private static final int REPRL_CHILD_DATA_IN = 102;
    private static final int REPRL_CHILD_DATA_OUT = 103;

    private static final int TIMEOUT_EXIT_CODE = 254;
    private static final int OUT_OF_MEMORY_EXIT_CODE = 253;
    private static LogLevel logLevel = LogLevel.severe;
    private static final String GRAALJS_FUZZILLI_LOG = "GRAALJS_FUZZILLI_LOG";

    private enum LogLevel {
        none,
        severe,
        info,
        debug
    }

    @CLibrary(value = "coverage", requireStatic = true)
    @CFunction(value = "__sanitizer_cov_reset_edgeguards", transition = Transition.NO_TRANSITION)
    public static native void sanitizerCovResetEdgeguards();

    @CLibrary(value = "coverage", requireStatic = true)
    @CFunction(value = "__fuzzilli_segfault", transition = Transition.NO_TRANSITION)
    public static native void fuzzilliSegfault();

    /**
     * Run one script on one newly created Context. Return an exit code and status.
     *
     */
    private static Pair<Integer, Integer> runFuzzilliExecute(Context.Builder contextBuilder, FileOutputStream dataWriteF, String script) {
        Pair<Integer, Integer> exitAndStatus;
        final int noCrashStatus = 0;
        final int crashStatus = 1;
        final int exitNoError = 0;
        final int exitWithError = 1;

        try (Context context = contextBuilder.build()) {
            FuzzilliHelper helper = new FuzzilliHelper(dataWriteF);
            context.getBindings("js").putMember("fuzzilli", helper);
            context.eval(Source.newBuilder("js", script, "fuzzilliInput").build());

            exitAndStatus = Pair.create(exitNoError, noCrashStatus);
        } catch (PolyglotException e) {
            log(LogLevel.debug, String.format("PolyglotException: %s (isInternal: %b)", e.toString(), e.isInternalError()));
            printStackTrace(e);
            if (e.isExit()) {
                // normal exit() calls are fine and not to be treated as errors
                // Exit code from the PolyglotException.
                // Status code (0) indicates no crash
                log(LogLevel.debug, String.format("Standard exit with code %d", e.getExitStatus()));
                exitAndStatus = Pair.create(e.getExitStatus(), noCrashStatus);
            } else if (e.isSyntaxError()) {
                // Syntax error indicates issue with fuzzer
                // Exit code (1) indicates error
                // Status code (0) indicates no crash
                exitAndStatus = Pair.create(exitWithError, noCrashStatus);
            } else if (!e.isInternalError()) {
                // Error exit (1), but no crash (0)
                exitAndStatus = Pair.create(exitWithError, noCrashStatus);
            } else {
                // InternalError will indicate crash status (1)
                exitAndStatus = Pair.create(exitNoError, crashStatus);
            }
        } catch (OutOfMemoryError e) {
            // special exit code for OutOfMemoryErrors
            log(LogLevel.info, String.format("Out of memory error: %s", e.toString()));
            exitAndStatus = Pair.create(exitNoError, crashStatus);
        } catch (Throwable t) {
            log(LogLevel.severe, "NON-POLYGLOT EXCEPTION");
            printStackTrace(t);
            // all non-polyglot exceptions should be treated as crashes by the fuzzer
            exitAndStatus = Pair.create(exitNoError, crashStatus);
        }

        return exitAndStatus;
    }

    public static int runFuzzilliREPRL(Context.Builder contextBuilder, String inputScript) {
        contextBuilder.option("js.unhandled-rejections", "throw");
        initLogLevel();
        boolean runREPRL = (inputScript == null);
        Pair<Integer, Integer> exitAndStatus = null;

        FileDescriptor dataWriteFD;
        if (!runREPRL) {
            exitAndStatus = runFuzzilliExecute(contextBuilder, null, inputScript);
            log(LogLevel.debug, String.format("Exit code = %d, status code = %d", exitAndStatus.getLeft(), exitAndStatus.getRight()));
            return 0;
        } else {
            try {
                Constructor<FileDescriptor> ctor = FileDescriptor.class.getDeclaredConstructor(Integer.TYPE);
                ctor.setAccessible(true);
                dataWriteFD = ctor.newInstance(REPRL_CHILD_DATA_OUT);
                ctor.setAccessible(false);
            } catch (Throwable t) {
                log(LogLevel.severe, "Can't open fuzz out file descriptor (" + Integer.toString(REPRL_CHILD_DATA_OUT) + ")");
                printStackTrace(t);
                return -1;
            }

            // Open the pipes for communicating with Fuzzilli
            try (FileInputStream controlReadF = new FileInputStream(new File("/dev/fd/" + Integer.toString(REPRL_CHILD_CTRL_IN)));
                            FileOutputStream controlWriteF = new FileOutputStream(new File("/dev/fd/" + Integer.toString(REPRL_CHILD_CTRL_OUT)));
                            RandomAccessFile dataReadF = new RandomAccessFile(new File("/dev/fd/" + Integer.toString(REPRL_CHILD_DATA_IN)), "rws");
                            FileOutputStream dataWriteF = new FileOutputStream(dataWriteFD)) {
                log(LogLevel.info, "GraalJS Fuzzilli REPRL started");

                // Handshake protocol, involving sending and receiving the 'HELO' message.
                final byte[] helo = new byte[]{'H', 'E', 'L', 'O'};
                final byte[] response = new byte[8];
                log(LogLevel.debug, "writing HELO...");
                controlWriteF.write(helo);
                if (!expectResponse(controlReadF, response, "HELO")) {
                    log(LogLevel.severe, "Invalid HELO response from parent: " + Arrays.toString(response));
                    return -1;
                }

                // Exit must be non-zero with uncaught exception, status must be set when crash, one
                // of them must be zero
                for (;;) {
                    // This loop does REPR
                    if (!expectResponse(controlReadF, response, "exec")) {
                        log(LogLevel.severe, "Unknown action: " + Arrays.toString(response));
                        return -1;
                    }
                    log(LogLevel.debug, "trying to read scriptSize");

                    // R: 'read' in new script
                    // First, read in the script length (long)
                    byte[] scriptSizeMsg = controlReadF.readNBytes(Long.BYTES);
                    int scriptSize = (int) ByteBuffer.wrap(scriptSizeMsg).order(ByteOrder.LITTLE_ENDIAN).getLong();
                    log(LogLevel.debug, "got scriptSize: " + scriptSize);
                    // Second, read in the script itself
                    byte[] scriptChars = new byte[scriptSize];
                    dataReadF.seek(0);
                    int n = dataReadF.read(scriptChars);
                    if (n != scriptSize) {
                        log(LogLevel.severe, String.format("ERROR: read %d bytes, but scriptSize was %d", n, scriptSize));
                    } else {
                        log(LogLevel.debug, String.format("got %d bytes", n));
                    }
                    String script = new String(scriptChars, StandardCharsets.UTF_8);
                    log(LogLevel.debug, "Got script:");
                    log(LogLevel.debug, script);

                    // E: 'execute' received script
                    // P: 'print' output is skipped, unless there's a crash
                    // R: 'reset' is implicit since each eval is called separately
                    exitAndStatus = runFuzzilliExecute(contextBuilder, dataWriteF, script);

                    // R: 'reset' the communication pipes and the fuzzing coverage state too
                    dataWriteF.flush();
                    System.out.flush();
                    System.err.flush();
                    sendExitCode(controlWriteF, exitAndStatus);
                    sanitizerCovResetEdgeguards();
                }
            } catch (Throwable t) {
                log(LogLevel.severe, "OUTER THROWABLE");
                printStackTrace(t);
                System.out.flush();
                System.err.flush();
                return -1;
            }
        }
    }

    private static void initLogLevel() {
        if (System.getenv().containsKey(GRAALJS_FUZZILLI_LOG)) {
            switch (System.getenv(GRAALJS_FUZZILLI_LOG)) {
                case "none":
                    logLevel = LogLevel.none;
                    break;
                case "severe":
                    logLevel = LogLevel.severe;
                    break;
                case "info":
                    logLevel = LogLevel.info;
                    break;
                case "debug":
                    logLevel = LogLevel.debug;
                    break;
                default:
                    System.err.printf("Invalid value for %s: %s", GRAALJS_FUZZILLI_LOG, System.getenv(GRAALJS_FUZZILLI_LOG));
                    System.exit(1);
            }
        }
    }

    private static void sendExitCode(final OutputStream controlWriteF, Pair<Integer, Integer> exitAndStatus) throws IOException {
        /*- From fuzzilli libreprl.h
         * The 32bit REPRL exit status as returned by reprl_execute has the following format:
         *     [ 00000000 | did_timeout | exit_code | terminating_signal ]
         */
        log(LogLevel.debug, "Sending exit code " + exitAndStatus);
        int status = (exitAndStatus.getLeft() & 0xff) << 8;
        status |= exitAndStatus.getRight() & 0xff;
        ByteBuffer buffer = ByteBuffer.allocate(4);
        buffer.order(ByteOrder.nativeOrder());
        buffer.putInt(status);
        controlWriteF.write(buffer.array());
    }

    private static boolean expectResponse(InputStream source, final byte[] buf, String expected) throws IOException {
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
