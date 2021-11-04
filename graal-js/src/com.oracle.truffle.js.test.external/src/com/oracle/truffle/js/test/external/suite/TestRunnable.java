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

import static com.oracle.truffle.js.lang.JavaScriptLanguage.ID;
import static com.oracle.truffle.js.lang.JavaScriptLanguage.MODULE_MIME_TYPE;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.graalvm.polyglot.Source;

public abstract class TestRunnable implements Runnable {

    protected static final Pattern EXTERNAL_LAUNCHER_ERROR_PATTERN = Pattern.compile("^(\\w+Error)(?::|\\s+at)");
    protected static final Pattern EXTERNAL_LAUNCHER_EXCEPTION_PATTERN = Pattern.compile("^([\\w\\.]+Exception):");

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

    protected static Source toSource(File scriptFile, boolean module) {
        try {
            Source.Builder builder = Source.newBuilder("js", scriptFile);
            if (module) {
                builder.mimeType(MODULE_MIME_TYPE);
            }
            return builder.build();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected Source[] loadHarnessSources(int ecmaVersion) {
        String harnessLocation = getConfig().getSuiteHarnessLoc();
        return HARNESS_SOURCES.computeIfAbsent(ecmaVersion, k -> Arrays.stream(suite.getPrequelFiles(k)).map(pfn -> {
            try {
                return Source.newBuilder(ID, Paths.get(harnessLocation, pfn).toFile()).build();
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }).toArray(Source[]::new));
    }

    protected void logFailure(int ecmaVersion, Throwable cause) {
        suite.logFail(testFile, failedMsg(ecmaVersion), cause);
    }

    protected void logFailure(int ecmaVersion, String msg) {
        suite.logFail(testFile, failedMsg(ecmaVersion), msg);
    }

    protected void logTimeout(int ecmaVersion) {
        suite.logFail(testFile, timeoutMsg(ecmaVersion), "");
    }

    protected static String failedMsg(int ecmaVersion) {
        return "FAILED" + ecmaVersionToString(ecmaVersion);
    }

    protected static String timeoutMsg(int ecmaVersion) {
        return "TIMEOUT" + ecmaVersionToString(ecmaVersion);
    }

    protected static String ecmaVersionToString(int ecmaVersion) {
        return " (ES" + ecmaVersion + ")";
    }

    protected static String extLauncherFindErrorMessage(String output) {
        Matcher matcher = EXTERNAL_LAUNCHER_ERROR_PATTERN.matcher(output);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    protected static String extLauncherFindError(String output) {
        String line = extLauncherFindLine(EXTERNAL_LAUNCHER_ERROR_PATTERN, output);
        if (line != null) {
            return line;
        }
        line = extLauncherFindLine(EXTERNAL_LAUNCHER_EXCEPTION_PATTERN, output);
        if (line != null) {
            return line;
        }
        return output;
    }

    private static String extLauncherFindLine(Pattern pattern, String output) {
        Matcher matcher = pattern.matcher(output);
        if (matcher.find()) {
            return output.substring(matcher.start(), output.indexOf('\n', matcher.start()));
        }
        return null;
    }

    protected static OutputStream makeDualStream(OutputStream s1, OutputStream s2) {
        return new OutputStream() {

            @Override
            public void write(int b) throws IOException {
                s1.write(b);
                s2.write(b);
            }
        };
    }

    protected static Stream<String> getStrings(List<String> scriptCode, String prefix, Pattern findPattern, Pattern splitPattern) {
        Stream<String> stream = Stream.empty();
        for (String line : scriptCode) {
            if (line.contains(prefix)) {
                Matcher matcher = findPattern.matcher(line);
                if (matcher.find()) {
                    stream = Stream.concat(stream, splitPattern.splitAsStream(matcher.group(1)));
                }
            }
        }
        return stream;
    }

    // ~ Inner classes

    @FunctionalInterface
    public interface TestTask {
        TestFile.Result run(int ecmaVersion);
    }
}
