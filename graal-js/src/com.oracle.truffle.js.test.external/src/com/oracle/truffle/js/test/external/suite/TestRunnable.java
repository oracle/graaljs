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
                return Source.newBuilder(AbstractJavaScriptLanguage.ID, Paths.get(harnessLocation, pfn).toFile()).build();
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
