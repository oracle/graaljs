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
        String harnessSource = "(function(global){" +
                "Object.defineProperty(global, 'printError', {\n" +
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
                "});\n" +
                "Object.defineProperty(global, 'fail', {\n" +
                "    configurable: true,\n" +
                "    enumerable: false,\n" +
                "    writable: true,\n" +
                "    value: function (message, error) {\n" +
                "        var Throwable = Java.type('java.lang.Throwable');" +
                "        var throwable = null;\n" +
                "        if (typeof error != 'undefined') {\n" +
                "            if (error instanceof Throwable) {\n" +
                "                throwable = error;\n" +
                "            } else if (error.nashornException instanceof Throwable) {\n" +
                "                throwable = error.nashornException;\n" +
                "            }\n" +
                "        }\n" +
                "        if (throwable != null) {\n" +
                "            throw new TypeError(message + ': ' +  throwable);\n" +
                "        } else {\n" +
                "            throw new TypeError(message);\n" +
                "        }\n" +
                "    }\n" +
                "});\n" +
                "  var Assert = Java.type('org.junit.Assert');\n" +
                "  Object.defineProperty(global, 'Assert', {\n" +
                "    configurable: true,\n" +
                "    enumerable: false,\n" +
                "    writable: true,\n" +
                "    value: {\n" +
                "        fail: Assert.fail,\n" +
                "        assertTrue: Assert.assertTrue,\n" +
                "        assertFalse: Assert.assertFalse,\n" +
                "        assertSame: Assert.assertSame,\n" +
                "        assertEquals: function(a, b) {\n" +
                "            if (typeof a == 'number') Assert.assertTrue('expected:<'+a+'> but was:<'+b+'>',\n" +
                "                                      typeof b == 'number' && (a === b && (a !== 0 || (1 / a) === (1 / b)) || isNaN(a) && isNaN(b)));\n" +
                "            else Assert.assertEquals(a, b);\n" +
                "        },\n" +
                "        'assertEquals(float, float, float)': Assert['assertEquals(float,float,float)'],\n" +
                "    }\n" +
                "  });\n" +
                "})(this);\n";
        // @formatter:on
        return new Source[]{Source.newBuilder(JavaScriptLanguage.ID, harnessSource, "assert.js").buildLiteral()};
    }

}
