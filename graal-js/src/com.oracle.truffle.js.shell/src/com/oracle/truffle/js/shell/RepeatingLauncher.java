/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.js.shell.JSLauncher.PreprocessResult.Consumed;
import static java.lang.Math.abs;

import java.util.Map;

import org.graalvm.options.OptionCategory;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;

public class RepeatingLauncher extends JSLauncher {
    private int engines = 1;
    private int runs = 1;

    @Override
    protected PreprocessResult preprocessArgument(String argument, String value, Map<String, String> polyglotOptions) {
        if (argument.equals("engines")) {
            engines = parsePositiveInteger(argument, value);
            return Consumed;
        }
        if (argument.equals("runs")) {
            runs = parsePositiveInteger(argument, value);
            return Consumed;
        }
        return super.preprocessArgument(argument, value, polyglotOptions);
    }

    protected int parsePositiveInteger(String optName, String optValue) {
        try {
            int val = Integer.parseInt(optValue);
            if (val >= 0) {
                return val;
            }
        } catch (NumberFormatException e) {
            // fall through
        }
        throw abort("The value of '" + optName + "' must be a positive integer");
    }

    @Override
    protected int executeScripts(Context.Builder contextBuilder) {
        int result = 0;
        for (int i = 0; i < engines; i++) {
            int newResult = super.executeScripts(contextBuilder);
            if (abs(newResult) > abs(result)) {
                result = newResult;
            }
        }
        return result;
    }

    @Override
    Source[] parseSources() {
        Source[] originalSources = super.parseSources();
        if (runs == 1) {
            return originalSources;
        }
        Source[] newSources = new Source[originalSources.length * runs];
        for (int i = 0; i < originalSources.length; i++) {
            for (int j = 0; j < runs; j++) {
                newSources[i * runs + j] = originalSources[i];
            }
        }
        return newSources;
    }

    @Override
    protected void validateArguments(Map<String, String> polyglotOptions) {
        super.validateArguments(polyglotOptions);
        if (!hasSources()) {
            if (engines > 1) {
                throw abort("Can not use --engines with the REPL");
            }
            if (runs > 1) {
                throw abort("Can not use --runs with the REPL");
            }
        }
    }

    @Override
    protected void printHelp(OptionCategory maxCategory) {
        super.printHelp(maxCategory);
        System.out.println("\nCustom developer options:");
        printOption("--runs N", "run scripts N times");
        printOption("--engines N", "load scripts in N different engines");
    }
}
