/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.test.external.nashorn;

import java.io.File;
import java.nio.ByteOrder;
import java.util.List;

import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Source;

import com.oracle.truffle.js.parser.JavaScriptLanguage;
import com.oracle.truffle.js.test.external.suite.TestFile;
import com.oracle.truffle.js.test.external.suite.TestRunnable;
import com.oracle.truffle.js.test.external.suite.TestSuite;

public class TestNashornRunnable extends TestRunnable {

    private static final Source[] HARNESS_SOURCES;

    static {
        HARNESS_SOURCES = createHarnessSources();
    }

    public TestNashornRunnable(TestSuite suite, TestFile testFile) {
        super(suite, testFile);
    }

    @Override
    public void run() {
        suite.printProgress(testFile);
        final File file = suite.resolveTestFilePath(testFile);
        List<String> scriptCodeList = TestSuite.readFileContentList(file);
        String scriptCode = TestSuite.toPrintableCode(scriptCodeList);
        boolean hasException = false;
        String exceptionMessage = null;
        suite.logVerbose(getName());

        if (getConfig().isPrintScript()) {
            synchronized (suite) {
                System.out.println("================================================================");
                System.out.println("====== Testcase: " + getName());
                System.out.println("================================================================");
                System.out.println();
                System.out.println(scriptCode);
            }
        }

        boolean littleEndian = ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN;
        if (!scriptCode.contains("@test") || scriptCode.contains("@subtest") || (littleEndian && scriptCode.contains("@bigendian") || !littleEndian && scriptCode.contains("@littleendian"))) {
            testFile.setResult(TestFile.Result.IGNORED);
            suite.logVerbose("Skipping test " + testFile.getFilePath() + " (not a real test file)");
            return;
        }

        // ecma version - verify that it is not set in config
        TestFile.EcmaVersion ecmaVersion = testFile.getEcmaVersion();
        assert ecmaVersion == null : "'ecmaVersion' should not be configured for " + testFile;

        // now run it
        TestNashornCallable tc = new TestNashornCallable(suite, loadHarnessSources(-1), toSource(file), file);
        try {
            tc.call();
        } catch (Throwable e) {
            if (e instanceof PolyglotException && ((PolyglotException) e).isGuestException()) {
                hasException = true;
                if (((PolyglotException) e).isSyntaxError()) {
                    exceptionMessage = e.getMessage().replaceFirst("^SyntaxError: ", "").replace("\r\n", "\n").replace("\\", "/");
                } else {
                    exceptionMessage = e.getMessage().replace("\r\n", "\n") + "\n";
                }
            } else {
                testFile.setResult(TestFile.Result.failed(e));
                suite.logFail(testFile, "Error: exception had been thrown: " + e, suite.getBackTrace(e));
                return;
            }
        }

        synchronized (TestNashorn.origOut) {
            System.setErr(TestNashorn.origErr);
            System.setOut(TestNashorn.origOut);

            String resultOutput = replaceNull(tc.getResultOutput()) + (hasException ? exceptionMessage : "");
            String resultError = replaceNull(tc.getResultError());
            String outputFilter = getConfig().getOutputFilter();
            if (outputFilter != null) {
                resultOutput = resultOutput.replace(outputFilter, "");
                resultError = resultError.replace(outputFilter, "");
            }
            String expectedOutput = replaceNull(readExpectedOutput(file));

            assert testFile.getResult() == null : testFile.getResult();
            if (expectedOutput.equals(resultOutput) && resultError.length() == 0) {
                testFile.setResult(TestFile.Result.PASSED);
                suite.logVerbose("Passed normal " + getName());
            } else {
                testFile.setResult(TestFile.Result.failed("error: result not as expected"));
                suite.logFail(testFile, "Error: result not as expected", String.format("Expected output:\n%s\nActual output:\n%s\n%s", expectedOutput, resultOutput, resultError));
                if (getConfig().isSaveOutput()) {
                    TestNashorn.writeResult(getName(), resultOutput + resultError);
                }
            }
        }
    }

    private static String replaceNull(String s) {
        return s == null ? "" : s;
    }

    private static String readExpectedOutput(File testFile) {
        File expectedFile = new File(testFile.getAbsolutePath() + ".EXPECTED");
        if (expectedFile.exists()) {
            List<String> expectedContent = TestSuite.readFileContentList(expectedFile);
            return TestSuite.toPrintableCode(expectedContent);
        } else {
            return "";
        }
    }

    /**
     * Nashorn test framework definitions.
     *
     * @see org.junit.Assert
     */
    @Override
    protected Source[] loadHarnessSources(int ecmaVersion) {
        return HARNESS_SOURCES;
    }

    private static Source[] createHarnessSources() {
        // @formatter:off
        String printErrorSource = "Object.defineProperty(this, \"printError\", {\n" +
                "    configurable: true,\n" +
                "    enumerable: false,\n" +
                "    writable: true,\n" +
                "    value: function (e) {\n" +
                "        var msg = e.message;\n" +
                "        var str = e.name + ':';\n" +
                "        if (e.lineNumber > 0) {\n" +
                "            str += e.lineNumber + ':';\n" +
                "        }\n" +
                "        if (e.columnNumber > 0) {\n" +
                "            str += e.columnNumber + ':';\n" +
                "        }\n" +
                "        str += msg.substring(msg.indexOf(' ') + 1);\n" +
                "        print(str);\n" +
                "    }\n" +
                "});\n";
        String failSource = "Object.defineProperty(this, \"fail\", {\n" +
                "    configurable: true,\n" +
                "    enumerable: false,\n" +
                "    writable: true,\n" +
                "    value: function (message, error) {\n" +
                "        var throwable = null;\n" +
                "        if (typeof error != 'undefined') {\n" +
                "            if (error instanceof java.lang.Throwable) {\n" +
                "                throwable = error;\n" +
                "            } else if (error.nashornException instanceof java.lang.Throwable) {\n" +
                "                throwable = error.nashornException;\n" +
                "            }\n" +
                "        }\n" +
                "        if (throwable != null) {\n" +
                "            throw new TypeError(message + ': ' +  throwable);\n" +
                "        } else {\n" +
                "            throw new TypeError(message);\n" +
                "        }\n" +
                "    }\n" +
                "});\n";
        String assertSource = "Object.defineProperty(this, \"Assert\", {\n" +
                "    configurable: true,\n" +
                "    enumerable: false,\n" +
                "    writable: true,\n" +
                "    value: {\n" +
                "        fail: Packages.org.junit.Assert.fail,\n" +
                "        assertTrue: Packages.org.junit.Assert.assertTrue,\n" +
                "        assertFalse: Packages.org.junit.Assert.assertFalse,\n" +
                "        assertSame: Packages.org.junit.Assert.assertSame,\n" +
                "        assertEquals: function(a, b) {\n" +
                "            if (typeof a == 'number') Packages.org.junit.Assert.assertTrue('expected:<'+a+'> but was:<'+b+'>',\n" +
                "                                      typeof b == 'number' && (a === b && (a !== 0 || (1 / a) === (1 / b)) || isNaN(a) && isNaN(b)));\n" +
                "            else Packages.org.junit.Assert.assertEquals(a, b);\n" +
                "        },\n" +
                "        'assertEquals(float, float, float)': Packages.org.junit.Assert['assertEquals(float,float,float)'],\n" +
                "    }\n" +
                "});\n";
        // @formatter:on
        return new Source[]{Source.newBuilder(JavaScriptLanguage.ID, printErrorSource + failSource + assertSource, "assert.js").buildLiteral()};
    }

}
