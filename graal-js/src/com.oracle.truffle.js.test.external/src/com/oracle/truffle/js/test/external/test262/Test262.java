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

    private static final String[] PREQUEL_FILES_ES5 = new String[]{
                    "arrayContains.js",
                    "assert.js",
                    "assertRelativeDateMs.js",
                    "byteConversionValues.js",
                    "compareArray.js",
                    "dateConstants.js",
                    "decimalToHexString.js",
                    "detachArrayBuffer.js",
                    "doneprintHandle.js",
                    "fnGlobalObject.js",
                    "nans.js",
                    "promiseHelper.js",
                    "propertyHelper.js",
                    "proxyTrapsHelper.js",
                    "regExpUtils.js",
                    "sta.js",
                    "tcoHelper.js",
                    "testIntl.js",
                    "testTypedArray.js",
    };
    private static final String[] PREQUEL_FILES;
    private static final String[] TEST_DIRS = new String[]{
                    "annexB",
                    "built-ins",
                    "language",
                    "intl402",
    };
    private static final String TESTS_CONFIG_FILE = "test262.json";
    private static final String FAILED_TESTS_FILE = "test262.failed";

    private Source mockupSource;

    static {
        String[] prequelFilesES6 = new String[]{
                        "atomicsHelper.js",
                        "isConstructor.js",
                        "nativeFunctionMatcher.js",
                        "testAtomics.js", // see XXX comment in it!
                        "typeCoercion.js"
        };
        PREQUEL_FILES = new String[PREQUEL_FILES_ES5.length + prequelFilesES6.length];
        System.arraycopy(PREQUEL_FILES_ES5, 0, PREQUEL_FILES, 0, PREQUEL_FILES_ES5.length);
        System.arraycopy(prequelFilesES6, 0, PREQUEL_FILES, PREQUEL_FILES_ES5.length, prequelFilesES6.length);
    }

    public Test262(SuiteConfig config) {
        super(config);
    }

    @Override
    public String[] getPrequelFiles(int ecmaVersion) {
        if (ecmaVersion >= 6) {
            return PREQUEL_FILES;
        }
        return PREQUEL_FILES_ES5;
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
