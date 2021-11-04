/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.test.external.suite;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import com.oracle.truffle.js.runtime.JSContextOptions;

public class TestExtProcessCallable extends AbstractTestCallable {

    public enum TestShellCallableResult {
        SUCCESS,
        FAILURE,
        TIMEOUT
    }

    private static final List<String> CONSTANT_OPTIONS = Arrays.asList("--experimental-options");

    private final List<String> cmd;
    private OutputStream stdout;
    private OutputStream stderr;

    public TestExtProcessCallable(TestSuite suite, int ecmaScriptVersion, List<String> args) {
        this(suite, ecmaScriptVersion, args, Collections.emptyMap());
    }

    public TestExtProcessCallable(TestSuite suite, int ecmaScriptVersion, List<String> args, Map<String, String> extraOptions) {
        super(suite);
        this.cmd = createCommand(suite, ecmaScriptVersion, extraOptions, args);
    }

    private static List<String> createCommand(TestSuite suite, int ecmaScriptVersion, Map<String, String> extraOptions, List<String> args) {
        List<String> ret = new ArrayList<>(1 + CONSTANT_OPTIONS.size() + suite.getCommonExtLauncherOptions().size() + extraOptions.size() + args.size());
        ret.add(suite.getConfig().getExtLauncher());
        ret.addAll(CONSTANT_OPTIONS);
        ret.addAll(suite.getCommonExtLauncherOptions());
        ret.add(optionToString(JSContextOptions.ECMASCRIPT_VERSION_NAME, ecmaScriptVersionToOptionString(ecmaScriptVersion)));
        for (Map.Entry<String, String> entry : extraOptions.entrySet()) {
            ret.add(optionToString(entry.getKey(), entry.getValue()));
        }
        ret.addAll(args);
        return ret;
    }

    public static String optionToString(String key, String value) {
        return value.isEmpty() ? key : "--" + key + "=" + value;
    }

    @Override
    public TestShellCallableResult call() throws Exception {
        if (suite.getConfig().isPrintCommand()) {
            suite.log(cmd.stream().collect(Collectors.joining(" ")));
        }
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        Process p = pb.start();
        Future<?> stdoutPipe = null;
        Future<?> stderrPipe = null;
        if (stdout != null) {
            stdoutPipe = suite.getExtLauncherPipePool().submit(pipe(p.getInputStream(), stdout));
        }
        if (stderr != null) {
            stderrPipe = suite.getExtLauncherPipePool().submit(pipe(p.getErrorStream(), stderr));
        }
        if (!p.waitFor(getConfig().getTimeoutTest(), TimeUnit.SECONDS)) {
            p.destroyForcibly();
            killPipe(stdoutPipe);
            killPipe(stderrPipe);
            return TestShellCallableResult.TIMEOUT;
        }
        try {
            waitForPipe(stdoutPipe);
            waitForPipe(stderrPipe);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            killPipe(stdoutPipe);
            killPipe(stderrPipe);
            System.out.println("WARNING: timeout while waiting for stdout/stderr pipes. Does the application produce abnormal amounts of output?");
            return TestShellCallableResult.TIMEOUT;
        }
        return p.exitValue() == 0 ? TestShellCallableResult.SUCCESS : TestShellCallableResult.FAILURE;
    }

    private static void killPipe(Future<?> pipe) {
        if (pipe != null) {
            pipe.cancel(true);
        }
    }

    private void waitForPipe(Future<?> pipe) throws InterruptedException, ExecutionException, TimeoutException {
        if (pipe == null) {
            return;
        }
        pipe.get(getConfig().getTimeoutTest(), TimeUnit.SECONDS);
    }

    private static Runnable pipe(InputStream from, OutputStream to) {
        return () -> {
            try {
                byte[] buffer = new byte[256];
                int bytesRead;
                while ((bytesRead = from.read(buffer)) != -1) {
                    to.write(buffer, 0, bytesRead);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        };
    }

    @Override
    public void setOutput(OutputStream out) {
        this.stdout = out;
    }

    @Override
    public void setError(OutputStream err) {
        this.stderr = err;
    }
}
