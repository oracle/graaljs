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
package com.oracle.truffle.js.test.external.testv8;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.graalvm.polyglot.Source;

import com.oracle.truffle.js.parser.JavaScriptLanguage;
import com.oracle.truffle.js.runtime.ExitException;
import com.oracle.truffle.js.runtime.JSContextOptions;
import com.oracle.truffle.js.test.external.suite.TestCallable;
import com.oracle.truffle.js.test.external.suite.TestFile;
import com.oracle.truffle.js.test.external.suite.TestRunnable;
import com.oracle.truffle.js.test.external.suite.TestSuite;

public class TestV8Runnable extends TestRunnable {
    private static final int LONG_RUNNING_TEST_SECONDS = 55;

    private static final Map<String, String> commonOptions;
    static {
        Map<String, String> options = new HashMap<>();
        options.put(JSContextOptions.V8_COMPATIBILITY_MODE_NAME, "true");
        options.put(JSContextOptions.V8_REALM_BUILTIN_NAME, "true");
        commonOptions = Collections.unmodifiableMap(options);
    }

    public TestV8Runnable(TestSuite suite, TestFile testFile) {
        super(suite, testFile);
    }

    @Override
    public void run() {
        suite.printProgress(testFile);
        final File file = suite.resolveTestFilePath(testFile);
        List<String> code = TestSuite.readFileContentList(file);
        boolean negative = isNegativeTest(code);
        boolean shouldThrow = shouldThrow(code);
        suite.logVerbose("Starting: " + getName());

        if (getConfig().isPrintScript()) {
            printScript(TestSuite.toPrintableCode(code));
        }

        // ecma versions
        TestFile.EcmaVersion ecmaVersion = testFile.getEcmaVersion();
        assert ecmaVersion != null : testFile;

        // now run it
        testFile.setResult(runTest(ecmaVersion, version -> runInternal(version, file, negative, shouldThrow)));
    }

    private TestFile.Result runInternal(int ecmaVersion, File file, boolean negative, boolean shouldThrow) {
        final String ecmaVersionSuffix = " (ES" + ecmaVersion + ")";
        suite.logVerbose(getName() + ecmaVersionSuffix);
        TestFile.Result testResult;

        long startDate = System.currentTimeMillis();
        String testFileNamePrefix = "\nTEST_FILE_NAME = \"" + file.getPath() + "\"\n";
        Source testFileNamePrefixSource = Source.newBuilder(JavaScriptLanguage.ID, testFileNamePrefix, "").buildLiteral();
        Source[] prequelSources = loadHarnessSources(ecmaVersion);
        Source[] sources = Arrays.copyOf(prequelSources, prequelSources.length + 2);
        sources[sources.length - 1] = testFileNamePrefixSource;
        sources[sources.length - 2] = ((TestV8) suite).getMockupSource();

        TestCallable tc = new TestCallable(suite, sources, toSource(file), file, ecmaVersion, commonOptions);
        if (!suite.getConfig().isPrintFullOutput()) {
            tc.setOutput(DUMMY_OUTPUT_STREAM);
        }
        reportStart();
        try {
            tc.call();
            testResult = TestFile.Result.PASSED;
        } catch (Throwable e) {
            if (e instanceof ExitException && ((ExitException) e).getStatus() == 0) {
                testResult = TestFile.Result.PASSED;
            } else {
                testResult = TestFile.Result.failed(ecmaVersionSuffix.trim(), e);
                if (!negative && !shouldThrow) {
                    suite.logFail(testFile, "FAILED" + ecmaVersionSuffix, e);
                }
            }
        }
        reportEnd(startDate);

        assert testResult != null : testFile;
        if (negative) {
            if (!testResult.isFailure()) {
                suite.logFail(testFile, "FAILED" + ecmaVersionSuffix, "negative test, was expected to fail but didn't");
            } else {
                testResult = TestFile.Result.PASSED;
            }
        }
        // test with --throws must fail
        if (shouldThrow) {
            if (!testResult.isFailure()) {
                suite.logFail(testFile, "FAILED" + ecmaVersionSuffix, "--throws test, was expected to fail but didn't");
            } else {
                testResult = TestFile.Result.PASSED;
            }
        }
        return testResult;
    }

    private void reportStart() {
        synchronized (suite) {
            suite.getActiveTests().add(this);
        }
    }

    private void reportEnd(long startDate) {
        long endDate = System.currentTimeMillis();
        long executionTime = endDate - startDate;
        synchronized (suite) {
            if (executionTime > LONG_RUNNING_TEST_SECONDS * 1000) {
                System.out.println("Long running test finished: " + getTestFile().getFilePath() + " " + (endDate - startDate));
            }

            suite.getActiveTests().remove(this);
            String activeTestNames = "";
            if (suite.getConfig().isVerbose()) {
                for (TestRunnable test : suite.getActiveTests()) {
                    if (activeTestNames.length() > 0) {
                        activeTestNames += ", ";
                    }
                    activeTestNames += test.getName();
                }
                suite.logVerbose("Finished test: " + getName() + " after " + executionTime + " ms, active: " + activeTestNames);
            }
        }
    }

    private void printScript(String scriptCode) {
        synchronized (suite) {
            System.out.println("================================================================");
            System.out.println("====== Testcase: " + getName());
            System.out.println("================================================================");
            System.out.println();
            System.out.println(scriptCode);
        }
    }

    private static boolean isNegativeTest(List<String> scriptCode) {
        for (String line : scriptCode) {
            if (line.contains("* @negative")) {
                return true;
            }
        }
        return false;
    }

    private static boolean shouldThrow(List<String> scriptCode) {
        for (String line : scriptCode) {
            if (line.contains("--throws")) {
                return true;
            }
        }
        return false;
    }

    private static final OutputStream DUMMY_OUTPUT_STREAM = new OutputStream() {

        @Override
        public void write(int b) throws IOException {
        }
    };

}
