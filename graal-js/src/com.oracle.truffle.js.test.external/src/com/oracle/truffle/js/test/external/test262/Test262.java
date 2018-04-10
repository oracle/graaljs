/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.test.external.test262;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.stream.Stream;
import java.util.TimeZone;

import org.graalvm.polyglot.Source;

import com.oracle.truffle.js.test.external.suite.SuiteConfig;
import com.oracle.truffle.js.test.external.suite.TestFile;
import com.oracle.truffle.js.test.external.suite.TestRunnable;
import com.oracle.truffle.js.test.external.suite.TestSuite;
import com.oracle.truffle.js.test.external.testv8.TestV8;

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

    private Source mockupSource;

    public Test262(SuiteConfig config) {
        super(config);
    }

    public Source[] getHarnessSources(boolean strict, boolean async, Stream<String> includes) {
        Source prologSource = Source.newBuilder("js", strict ? "var strict_mode = true;" : "var strict_mode = false;", "").buildLiteral();
        Stream<Source> prologStream = Stream.of(prologSource);
        Stream<Source> mockupStream = Stream.of(getMockupSource());

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
        return Stream.concat(Stream.concat(prologStream, harnessStream).map(s -> applyPrefix(s, prefix)), mockupStream).toArray(Source[]::new);
    }

    private static Source applyPrefix(Source source, String prefix) {
        if (prefix.isEmpty()) {
            return source;
        }
        return Source.newBuilder("js", prefix + source.getCharacters(), source.getName()).buildLiteral();
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
    protected void setupTestFile(TestFile testFile) {
        // force running tests with latest ES version
        // see GR-3508
        testFile.setEcmaVersion(TestFile.EcmaVersion.forVersions(TestFile.EcmaVersion.MAX_VERSION));
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

    private static Source loadMockup() {
        InputStream resourceStream = TestV8.class.getResourceAsStream("/com/oracle/truffle/js/test/external/resources/test262mockup.js");
        try {
            return Source.newBuilder("js", new InputStreamReader(resourceStream, StandardCharsets.UTF_8), "test262mockup.js").internal(true).build();
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    protected Source getMockupSource() {
        if (this.mockupSource == null) {
            this.mockupSource = loadMockup();
        }
        return this.mockupSource;
    }

    @Override
    protected boolean isTestExecutable(String name) {
        /*
         * Files bearing a name ending in `_FIXTURE.js` should not be interpreted as standalone
         * tests; they are intended to be referenced by test files (INTERPRETING.md).
         */
        return !name.endsWith("_FIXTURE.js");
    }

    public static void main(String[] args) {
        SuiteConfig config = new SuiteConfig(SUITE_NAME, SUITE_DESCRIPTION, DEFAULT_LOC, DEFAULT_CONFIG_LOC, TESTS_REL_LOC, HARNESS_REL_LOC);

        TimeZone pstZone = TimeZone.getTimeZone("PST"); // =Californian Time (PST)
        TimeZone.setDefault(pstZone);

        System.out.println("Checking your Javascript conformance. Using ECMAScript Test262 testsuite.\n");

        if (args.length > 0) {
            for (String arg : args) {
                TestSuite.parseDefaultArgs(arg, config);
            }
        }

        Test262 suite = new Test262(config);
        System.exit(suite.runTestSuite(TEST_DIRS));
    }
}
