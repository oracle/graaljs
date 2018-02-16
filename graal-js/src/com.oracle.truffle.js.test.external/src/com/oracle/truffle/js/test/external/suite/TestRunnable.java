/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.test.external.suite;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.graalvm.polyglot.Source;

import com.oracle.truffle.js.runtime.AbstractJavaScriptLanguage;

public abstract class TestRunnable implements Runnable {

    private static final ConcurrentMap<Integer, Source[]> HARNESS_SOURCES = new ConcurrentHashMap<>();

    protected final TestSuite suite;
    protected final TestFile testFile;

    public TestRunnable(TestSuite suite, TestFile testFile) {
        assert testFile != null;
        this.testFile = testFile;
        this.suite = suite;
    }

    public String getName() {
        return testFile.getFilePath();
    }

    public TestFile getTestFile() {
        return testFile;
    }

    protected final SuiteConfig getConfig() {
        return suite.getConfig();
    }

    protected TestFile.Result runTest(TestFile.EcmaVersion ecmaVersion, TestTask testTask) {
        TestFile.Result testResult = null;
        for (int version : ecmaVersion.getAllVersions()) {
            TestFile.Result versionResult = testTask.run(version);
            if (testResult == null) {
                testResult = versionResult;
            } else if (versionResult.isTimeout()) {
                // we prefer timeouts because we count them
                testResult = versionResult;
            } else if (versionResult.isFailure() && !testResult.isFailure()) {
                testResult = versionResult;
            }
        }
        assert testResult != null : testFile;
        return testResult;
    }

    protected static Source toSource(File scriptFile) {
        try {
            return Source.newBuilder("js", scriptFile).build();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected Source[] loadHarnessSources(int ecmaVersion) {
        String harnessLocation = getConfig().getSuiteHarnessLoc();
        return HARNESS_SOURCES.computeIfAbsent(ecmaVersion, k -> Arrays.stream(suite.getPrequelFiles(k)).map(pfn -> {
            try {
                return AbstractJavaScriptLanguage.newSourceFromFileName(Paths.get(harnessLocation, pfn).toString());
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }).toArray(Source[]::new));
    }

    // ~ Inner classes

    public interface TestTask {
        TestFile.Result run(int ecmaVersion);
    }

}
