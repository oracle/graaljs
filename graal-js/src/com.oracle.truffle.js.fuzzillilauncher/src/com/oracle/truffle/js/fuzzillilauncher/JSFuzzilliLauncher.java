/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.js.shell.JSLauncher;
import org.graalvm.polyglot.Context;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

/**
 * Implements a launcher for fuzzing GraalJS with Fuzzilli. When run without the {@code --reproduce}
 * argument, starts running Fuzzilli's REPRL loop. When run with the
 * {@code --reproduce==<scriptfile>} argument, just spawns a context and executes that one script.
 * This design allows the JSFuzzilli launcher to exist separately from the main launcher
 * {@link JSLauncher} while also allowing fuzzing and reproduction of cases.
 */
public class JSFuzzilliLauncher extends JSLauncher {
    // Holds the script to run for reproduction of a crash
    String reproduceScript = null;

    public static void main(String[] args) {
        // Scan for '--reproduce=<script>' argument
        String reproduceScript = null;
        for (String arg : args) {
            if (arg.startsWith("--reproduce=")) {
                String[] parts = arg.split("=");
                assert parts.length == 2;
                try {
                    reproduceScript = Files.readString(Path.of(parts[1]));
                } catch (IOException e) {
                    return;
                }
            }
        }

        // Run launcher with remaining (non '-reproduce') arguments
        new JSFuzzilliLauncher(reproduceScript).launch(Arrays.stream(args).filter((arg) -> {
            return !arg.startsWith("--reproduce");
        }).toArray(String[]::new));
    }

    public JSFuzzilliLauncher(String reproduceScript) {
        this.reproduceScript = reproduceScript;
    }

    @Override
    protected void launch(Context.Builder contextBuilder) {
        int exitCode;

        if (!isAOT()) {
            System.err.println("WARN: AOT disable, fuzzilli won't have coverage info");
        }

        exitCode = JSFuzzilliRunner.runFuzzilliREPRL(contextBuilder, reproduceScript);

        if (exitCode != 0) {
            throw abort((String) null, exitCode);
        }
    }
}
