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
package com.oracle.truffle.js.test.external.test262;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.stream.Stream;

import org.graalvm.polyglot.Source;

import com.oracle.truffle.js.runtime.JSContextOptions;
import com.oracle.truffle.js.test.external.suite.SuiteConfig;
import com.oracle.truffle.js.test.external.suite.TestFile;
import com.oracle.truffle.js.test.external.suite.TestRunnable;
import com.oracle.truffle.js.test.external.suite.TestSuite;

public class Test262 extends TestSuite {

    private static final String SUITE_NAME = "test262";
    private static final String SUITE_DESCRIPTION = "ECMAscript test262";
    private static final String DEFAULT_LOC = Paths.get("lib", "test262").toString();
    private static final String DEFAULT_CONFIG_LOC = "test";
    private static final String TESTS_REL_LOC = "test";
    private static final String HARNESS_REL_LOC = "harness";

    private static final String[] COMMON_PREQUEL_FILES = new String[]{
                    "assert.js",
                    "sta.js"
    };
    private static final String[] ASYNC_PREQUEL_FILES = new String[]{
                    "doneprintHandle.js"
    };
    private static final String[] TEST_DIRS = new String[]{
                    "annexB",
                    "built-ins",
                    "language",
                    "intl402",
    };
    private static final String TESTS_CONFIG_FILE = "test262.json";
    private static final String FAILED_TESTS_FILE = "test262.failed";

    private final Map<String, String> commonOptions;
    private final List<String> commonOptionsExtLauncher;

    public Test262(SuiteConfig config) {
        super(config);
        Map<String, String> options = new HashMap<>();
        options.put(JSContextOptions.INTL_402_NAME, "true");
        options.put(JSContextOptions.TEST262_MODE_NAME, "true");
        options.put(JSContextOptions.GLOBAL_ARGUMENTS_NAME, "false");
        config.addCommonOptions(options);
        commonOptions = Collections.unmodifiableMap(options);
        commonOptionsExtLauncher = optionsToExtLauncherOptions(options);
    }

    public Source[] getHarnessSources(boolean strict, boolean async, Stream<String> includes) {
        Stream<Source> prologStream;
        if (getConfig().isExtLauncher()) {
            prologStream = Stream.empty();
        } else {
            Source prologSource = Source.newBuilder("js", strict ? "var strict_mode = true;" : "var strict_mode = false;", "").buildLiteral();
            prologStream = Stream.of(prologSource);
        }
        String harnessLocation = getConfig().getSuiteHarnessLoc();
        Stream<String> harnessNamesStream = Stream.of(COMMON_PREQUEL_FILES);
        if (async) {
            harnessNamesStream = Stream.concat(harnessNamesStream, Stream.of(ASYNC_PREQUEL_FILES));
        }
        Stream<Source> harnessStream = Stream.concat(harnessNamesStream, includes).map(pfn -> {
            try {
                return Source.newBuilder("js", Paths.get(harnessLocation, pfn).toFile()).build();
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        });

        String prefix = strict ? "\"use strict\";" : "";
        return Stream.concat(prologStream, harnessStream.map(s -> applyPrefix(s, prefix))).toArray(Source[]::new);
    }

    private static Source applyPrefix(Source source, String prefix) {
        if (prefix.isEmpty()) {
            return source;
        }
        return Source.newBuilder("js", new File(source.getPath())).content(prefix + source.getCharacters()).buildLiteral();
    }

    @Override
    public String[] getPrequelFiles(int ecmaVersion) {
        return COMMON_PREQUEL_FILES;
    }

    @Override
    public TestRunnable createTestRunnable(TestFile file) {
        return new Test262Runnable(this, file);
    }

    @Override
    protected File getTestsConfigFile() {
        return Paths.get(getConfig().getSuiteConfigLoc(), TESTS_CONFIG_FILE).toFile();
    }

    @Override
    protected File getUnexpectedlyFailedTestsFile() {
        return new File(FAILED_TESTS_FILE);
    }

    @Override
    public String getHTMLFileName() {
        return "test262.htm";
    }

    @Override
    public String getReportFileName() {
        return "test262.txt";
    }

    @Override
    public boolean executeWithSeparateThreads() {
        return false;
    }

    @Override
    public Map<String, String> getCommonOptions() {
        return commonOptions;
    }

    @Override
    public List<String> getCommonExtLauncherOptions() {
        return commonOptionsExtLauncher;
    }

    @Override
    protected boolean isTestExecutable(String name) {
        /*
         * Files bearing a name ending in `_FIXTURE.js` should not be interpreted as standalone
         * tests; they are intended to be referenced by test files (INTERPRETING.md).
         */
        return !name.endsWith("_FIXTURE.js");
    }

    public static void main(String[] args) throws Exception {
        SuiteConfig.Builder configBuilder = new SuiteConfig.Builder(SUITE_NAME, SUITE_DESCRIPTION, DEFAULT_LOC, DEFAULT_CONFIG_LOC, TESTS_REL_LOC, HARNESS_REL_LOC);

        TimeZone pstZone = TimeZone.getTimeZone("PST"); // =Californian Time (PST)
        TimeZone.setDefault(pstZone);

        System.out.println("Checking your Javascript conformance. Using ECMAScript Test262 testsuite.\n");

        TestSuite.parseDefaultArgs(args, configBuilder);

        Test262 suite = new Test262(configBuilder.build());
        System.exit(suite.runTestSuite(TEST_DIRS));
    }
}
