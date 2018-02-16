/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
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
        options.put(JSContextOptions.SHARED_ARRAY_BUFFER_NAME, "true");
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
