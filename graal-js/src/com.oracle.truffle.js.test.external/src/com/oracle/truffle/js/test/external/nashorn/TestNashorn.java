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
package com.oracle.truffle.js.test.external.nashorn;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Paths;
import java.util.List;
import java.util.TimeZone;

import com.oracle.truffle.js.runtime.JSTruffleOptions;
import com.oracle.truffle.js.test.external.suite.SuiteConfig;
import com.oracle.truffle.js.test.external.suite.TestFile;
import com.oracle.truffle.js.test.external.suite.TestRunnable;
import com.oracle.truffle.js.test.external.suite.TestSuite;

public class TestNashorn extends TestSuite {

    private static final String SUITE_NAME = "testnashorn";
    private static final String SUITE_DESCRIPTION = "Nashorn testsuite";
    private static final String DEFAULT_LOC = "";
    private static final String DEFAULT_CONFIG_LOC = Paths.get("..", "..", "test").toString();
    private static final String TESTS_REL_LOC = Paths.get("test", "script").toString();
    private static final String HARNESS_REL_LOC = "";

    private static final String[] TEST_DIRS = new String[]{"basic", "error"};
    private static final String TESTS_CONFIG_FILE = "testNashorn.json";
    private static final String FAILED_TESTS_FILE = "testnashorn.failed";

    public static PrintStream origOut;
    public static PrintStream origErr;

    private static final boolean isWindows = System.getProperty("os.name").startsWith("Windows");
    private static Boolean useNashornDiffBat;

    public TestNashorn(SuiteConfig config) {
        super(config);
    }

    @Override
    public String[] getPrequelFiles(int ecmaVersion) {
        throw new UnsupportedOperationException();
    }

    @Override
    public TestRunnable createTestRunnable(TestFile file) {
        return new TestNashornRunnable(this, file);
    }

    @Override
    protected void setupTestFile(TestFile testFile) {
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
    protected String formatLogFailHTML(String filename, String failMessage, String reason) {
        String linkSource = formatLink(getConfig().getSuiteTestsLoc() + filename, filename, false);
        String linkExpected = formatLink(getConfig().getSuiteTestsLoc() + filename + ".EXPECTED", "EXPECTED", true);
        StringBuilder html = new StringBuilder(200);
        html.append(failMessage);
        html.append(' ').append(linkSource);
        html.append(' ').append(linkExpected);
        if (getConfig().isSaveOutput()) {
            String outputFileName = Paths.get(getConfig().getSuiteLoc(), "output", toFlatName(filename)).toString();
            String linkDiff = formatLink(outputFileName + ".DIFF", "DIFF", false);
            String linkResult = formatLink(outputFileName + ".RESULT", "RESULT", false);
            html.append(' ').append(linkResult);
            html.append(' ').append(linkDiff);
        }
        html.append(' ').append(reason);
        return html.toString();
    }

    private static String formatLink(String fileName, String description, boolean checkFile) {
        if (!checkFile || new File(fileName).exists()) {
            return "<a href=\"" + fileName + "\">" + description + "</a>";
        } else {
            return description;
        }
    }

    @Override
    public String getHTMLFileName() {
        return "testnashorn.htm";
    }

    @Override
    public String getReportFileName() {
        return "testnashorn.txt";
    }

    public static void writeResult(String name, String result) {
        try {
            String flatName = toFlatName(name);
            String resultFileName = "output" + File.separator + flatName + ".RESULT";
            String expectedFileName = "test" + File.separator + "script" + File.separator + name + ".EXPECTED";
            String diffFileName = "output" + File.separator + flatName + ".DIFF";

            try (FileWriter writer = new FileWriter(resultFileName)) {
                writer.write(result);
            }

            String[] diffCmd;
            if (isWindows) {
                if (useNashornDiffBat == null) {
                    diffCmd = new String[]{"nashorndiff.bat", expectedFileName, resultFileName, diffFileName};
                    try {
                        Process p = Runtime.getRuntime().exec(diffCmd);
                        p.waitFor();
                        useNashornDiffBat = true;
                    } catch (IOException ioex) {
                        useNashornDiffBat = false;
                    }
                    return;
                }
                if (useNashornDiffBat) {
                    diffCmd = new String[]{"nashorndiff.bat", expectedFileName, resultFileName, diffFileName};
                } else {
                    // fallback - using fc command
                    diffCmd = new String[]{"cmd", "/C", String.format("fc \"%s\" \"%s\" > \"%s\"", expectedFileName, resultFileName, diffFileName)};
                }
            } else {
                diffCmd = new String[]{"bash", "-c", String.format("diff -u '%s' '%s' > '%s'", expectedFileName, resultFileName, diffFileName)};
            }
            Process p = Runtime.getRuntime().exec(diffCmd);
            p.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String toFlatName(String name) {
        return name.replace("/", "_").replace("\\", "_");
    }

    private static void initializeSaveOutput() {
        File outputDir = new File("output");
        outputDir.mkdir();
        for (File f : outputDir.listFiles()) {
            String name = f.getName();
            if (name.endsWith(".DIFF") || name.endsWith(".RESULT")) {
                f.delete();
            }
        }
    }

    @Override
    protected void printSkippedFilesHTML(PrintStream stream, List<TestFile> skippedFiles) {
        stream.println("<ul>");
        for (TestFile testFile : skippedFiles) {
            String filePath = testFile.getFilePath();
            String linkSource = formatLink(getConfig().getSuiteTestsLoc() + filePath, filePath, false);
            String linkExpected = formatLink(getConfig().getSuiteTestsLoc() + filePath + ".EXPECTED", "EXPECTED", true);
            stream.println("<li>" + linkSource + " " + linkExpected + "</li>");
        }
        stream.println("</ul>");
    }

    private static void exit(int status) {
        System.exit(status);
    }

    public static void main(String[] args) throws Exception {
        if (!JSTruffleOptions.NashornCompatibilityMode) {
            System.err.println("Nashorn testsuite requires NashornCompatibilityMode.");
            exit(-2);
        }

        SuiteConfig config = new SuiteConfig(SUITE_NAME, SUITE_DESCRIPTION, DEFAULT_LOC, DEFAULT_CONFIG_LOC, TESTS_REL_LOC, HARNESS_REL_LOC);

        TimeZone pstZone = TimeZone.getTimeZone("PST"); // Californian Time (PST)
        TimeZone.setDefault(pstZone);

        System.out.println("Checking your Javascript conformance. Using Nashorn testsuite.\n");

        if (args.length > 0) {
            for (String arg : args) {
                if (arg.equals("saveoutput")) {
                    config.setSaveOutput(true);
                    config.setHtmlOutput(true);
                } else {
                    TestSuite.parseDefaultArgs(arg, config);
                }
            }
        }

        // always generate html output for 'single=' param
        if (config.getEndsWithFilter() != null) {
            config.setSaveOutput(true);
            config.setHtmlOutput(true);
        }
        if (config.isSaveOutput()) {
            initializeSaveOutput();
        }

        TestNashorn suite = new TestNashorn(config);
        // remember out & err after TestSuite is created since it uses its own impls!
        origOut = System.out;
        origErr = System.err;
        exit(suite.runTestSuite(TEST_DIRS));
    }
}
