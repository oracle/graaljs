/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.test.external.testv8;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.TimeZone;

import org.graalvm.polyglot.Source;

import com.oracle.truffle.js.parser.JavaScriptLanguage;
import com.oracle.truffle.js.test.external.suite.SuiteConfig;
import com.oracle.truffle.js.test.external.suite.TestFile;
import com.oracle.truffle.js.test.external.suite.TestRunnable;
import com.oracle.truffle.js.test.external.suite.TestSuite;

public class TestV8 extends TestSuite {

    private static final String SUITE_NAME = "testv8";
    private static final String SUITE_DESCRIPTION = "Google V8 testsuite";
    private static final String DEFAULT_LOC = Paths.get("lib", "testv8", "testv8-20170906").toString();
    private static final String DEFAULT_CONFIG_LOC = "test";
    private static final String TESTS_REL_LOC = "test";
    private static final String HARNESS_REL_LOC = "";

    private static final String[] PREQUEL_FILES = new String[]{"/test/intl/assert.js", "/test/intl/utils.js", "/test/mjsunit/mjsunit.js"};
    private static final String[] TEST_DIRS = new String[]{"mjsunit"};
    private static final String TESTS_CONFIG_FILE = "testV8.json";
    private static final String FAILED_TESTS_FILE = "testv8.failed";

    private Source mockupSource;

    public TestV8(SuiteConfig config) {
        super(config);
    }

    @Override
    public String[] getPrequelFiles(int ecmaVersion) {
        return PREQUEL_FILES;
    }

    @Override
    public TestRunnable createTestRunnable(TestFile file) {
        return new TestV8Runnable(this, file);
    }

    @Override
    protected void setupTestFile(TestFile testFile) {
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
        return "testv8.htm";
    }

    @Override
    public String getReportFileName() {
        return "testv8.txt";
    }

    private static Source loadV8Mockup() {
        InputStream resourceStream = TestV8.class.getResourceAsStream("/com/oracle/truffle/js/test/external/resources/v8mockup.js");
        try {
            return Source.newBuilder(JavaScriptLanguage.ID, new InputStreamReader(resourceStream, StandardCharsets.UTF_8), "v8mockup.js").internal(true).build();
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    protected Source getMockupSource() {
        if (this.mockupSource == null) {
            this.mockupSource = loadV8Mockup();
        }
        return this.mockupSource;
    }

    public static void main(String[] args) {
        SuiteConfig config = new SuiteConfig(SUITE_NAME, SUITE_DESCRIPTION, DEFAULT_LOC, DEFAULT_CONFIG_LOC, TESTS_REL_LOC, HARNESS_REL_LOC);

        TimeZone pstZone = TimeZone.getTimeZone("PST"); // =Californian Time (PST)
        TimeZone.setDefault(pstZone);

        System.out.println("Checking your Javascript conformance. Using Google V8 testsuite.\n");

        if (args.length > 0) {
            for (String arg : args) {
                TestSuite.parseDefaultArgs(arg, config);
            }
        }

        TestV8 suite = new TestV8(config);
        System.exit(suite.runTestSuite(TEST_DIRS));
    }
}
