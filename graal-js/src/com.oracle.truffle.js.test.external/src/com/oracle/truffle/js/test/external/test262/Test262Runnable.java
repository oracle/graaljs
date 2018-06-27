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
package com.oracle.truffle.js.test.external.test262;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Source;

import com.oracle.truffle.js.runtime.JSContextOptions;
import com.oracle.truffle.js.test.external.suite.TestCallable;
import com.oracle.truffle.js.test.external.suite.TestFile;
import com.oracle.truffle.js.test.external.suite.TestRunnable;
import com.oracle.truffle.js.test.external.suite.TestSuite;
import com.oracle.truffle.js.test.external.suite.TestSuite.TestThread;

public class Test262Runnable extends TestRunnable {
    private static final String ASYNC_TEST_COMPLETE = "Test262:AsyncTestComplete";
    private static final Pattern NEGATIVE_PREFIX = Pattern.compile("^[\t ]*negative:");
    private static final Pattern NEGATIVE_TYPE_PREFIX = Pattern.compile("^[\t ]*type: ");
    private static final String FLAGS_PREFIX = "flags: ";
    private static final String INCLUDES_PREFIX = "includes: ";
    private static final String ONLY_STRICT_FLAG = "onlyStrict";
    private static final String MODULE_FLAG = "module";
    private static final Pattern FLAGS_PATTERN = Pattern.compile("flags: \\[((?:(?:, )?(?:\\w+))*)\\]");
    private static final Pattern INCLUDES_PATTERN = Pattern.compile("includes: \\[(.*)\\]");
    private static final Pattern SPLIT_PATTERN = Pattern.compile(", ");
    private static final Pattern ECMA_VERSION_PATTERN = Pattern.compile("^\\W*es(?<version>\\d+)id:");
    private static final String PENDING_ECMA_VERSION_LINE = "esid: pending";

    private static final Map<String, String> commonOptions;
    static {
        Map<String, String> options = new HashMap<>();
        options.put(JSContextOptions.INTL_402_NAME, "true");
        commonOptions = Collections.unmodifiableMap(options);
    }

    public Test262Runnable(TestSuite suite, TestFile testFile) {
        super(suite, testFile);
    }

    @Override
    public void run() {
        suite.printProgress(testFile);
        final File file = suite.resolveTestFilePath(testFile);
        List<String> scriptCodeList = TestSuite.readFileContentList(file);
        String negativeExpectedMessage = getNegativeMessage(scriptCodeList);
        boolean negative = negativeExpectedMessage != null;
        Set<String> flags = getFlags(scriptCodeList);
        boolean runStrict = flags.contains(ONLY_STRICT_FLAG);
        boolean asyncTest = isAsyncTest(scriptCodeList);
        boolean module = flags.contains(MODULE_FLAG);

        assert !asyncTest || !negative || negativeExpectedMessage.equals("SyntaxError") : "unsupported async negative test (does not expect an early SyntaxError): " + testFile.getFilePath();

        String prefix = runStrict ? "\"use strict\";" : "";
        org.graalvm.polyglot.Source testSource = createSource(file, prefix + TestSuite.toPrintableCode(scriptCodeList), module);

        if (getConfig().isPrintScript()) {
            synchronized (suite) {
                System.out.println("================================================================");
                System.out.println("====== Testcase: " + getName());
                System.out.println("================================================================");
                System.out.println();
                System.out.println(TestSuite.toPrintableCode(scriptCodeList));
            }
        }

        // ecma versions
        TestFile.EcmaVersion ecmaVersion = testFile.getEcmaVersion();
        if (ecmaVersion == null) {
            int[] detectedVersions = detectScriptEcmaVersions(scriptCodeList);
            assert detectedVersions.length != 0 : testFile.getFilePath();
            ecmaVersion = TestFile.EcmaVersion.forVersions(detectedVersions);
        }

        Source[] harnessSources = ((Test262) suite).getHarnessSources(runStrict, asyncTest, getIncludes(scriptCodeList));

        // now run it
        testFile.setResult(runTest(ecmaVersion, version -> runInternal(version, file, testSource, negative, asyncTest, negativeExpectedMessage, harnessSources)));
    }

    private TestFile.Result runInternal(int ecmaVersion, File file, org.graalvm.polyglot.Source testSource, boolean negative, boolean asyncTest, String negativeExpectedMessage,
                    Source[] harnessSources) {
        final String ecmaVersionSuffix = " (ES" + ecmaVersion + ")";
        suite.logVerbose(getName() + ecmaVersionSuffix);
        TestFile.Result testResult;

        OutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        OutputStream outputStream = byteArrayOutputStream;
        if (getConfig().isPrintFullOutput()) {
            outputStream = makeDualStream(byteArrayOutputStream, System.out);
        }

        TestCallable tc = new TestCallable(suite, harnessSources, testSource, file, ecmaVersion, commonOptions);
        tc.setOutput(outputStream);
        tc.setError(outputStream);

        String negativeThrowCauseMsg = null;
        Throwable negativeThrowCause = null;
        Future<Object> future = null;
        try {
            Thread t = Thread.currentThread();
            if (suite.executeWithSeparateThreads() && getConfig().isUseThreads() && t instanceof TestThread) {
                future = ((TestThread) t).getExecutor().submit(tc);
                future.get(getConfig().getTimeoutTest(), TimeUnit.SECONDS);
            } else {
                tc.call();
            }
            testResult = TestFile.Result.PASSED;
        } catch (TimeoutException e) {
            testResult = TestFile.Result.timeout(ecmaVersionSuffix.trim(), e);
            suite.logFail(testFile, "TIMEOUT" + ecmaVersionSuffix, "");
            if (future != null) {
                boolean result = future.cancel(true);
                if (!result) {
                    suite.logVerbose("Could not cancel!" + getName());
                }
            }
        } catch (Throwable e) {
            Throwable cause = e;
            if (suite.executeWithSeparateThreads()) {
                cause = e.getCause() != null ? e.getCause() : e;
            }

            testResult = TestFile.Result.failed(ecmaVersionSuffix.trim(), cause);
            if (negative) {
                negativeThrowCause = cause;
                negativeThrowCauseMsg = cause.getMessage();
            } else {
                suite.logFail(testFile, "FAILED" + ecmaVersionSuffix, cause);
            }
        }

        if (asyncTest && !negative) {
            String stdout = byteArrayOutputStream.toString();
            if (!stdout.contains(ASYNC_TEST_COMPLETE)) {
                testResult = TestFile.Result.failed("async test failed" + ecmaVersionSuffix);
                suite.logFail(testFile, "FAILED" + ecmaVersionSuffix, String.format("async test; expected output: '%s' actual: '%s'", ASYNC_TEST_COMPLETE, stdout));
            }
        }

        assert testResult != null : testFile;
        // Negate if this test must fail according to the specification.
        if (negative) {
            if (!testResult.isFailure()) {
                testResult = TestFile.Result.failed("negative test expected to fail" + ecmaVersionSuffix);
                suite.logFail(testFile, "FAILED" + ecmaVersionSuffix, "negative test, was expected to fail but didn't");
            } else if (negativeThrowCause instanceof PolyglotException &&
                            ((negativeThrowCauseMsg != null && negativeThrowCauseMsg.contains(negativeExpectedMessage)) || negativeExpectedMessage.equals("."))) {
                // e.g. 11.13.1-4-29gs.js has "negative: ."
                testResult = TestFile.Result.PASSED;
            } else {
                suite.logFail(testFile, "FAILED" + ecmaVersionSuffix, "negative test, was expected to fail, what it did, but for wrong reasons:\n\n" +
                                (negativeThrowCause instanceof PolyglotException ? negativeThrowCauseMsg : negativeThrowCause) + "\n\n" +
                                "expected: " + negativeExpectedMessage);
                testResult = TestFile.Result.failed("negative test expected to fail with different reasons" + ecmaVersionSuffix);
            }
        }
        return testResult;
    }

    private int[] detectScriptEcmaVersions(List<String> scriptCodeList) {
        List<String> matches = new ArrayList<>();
        for (String line : scriptCodeList) {
            if (line.endsWith("---*/")) {
                break;
            }
            if (line.equals(PENDING_ECMA_VERSION_LINE)) {
                matches.add(PENDING_ECMA_VERSION_LINE);
            } else {
                Matcher matcher = ECMA_VERSION_PATTERN.matcher(line);
                if (matcher.find()) {
                    matches.add(matcher.group("version"));
                }
            }
        }
        assert matches.size() == new HashSet<>(matches).size() : testFile.getFilePath() + ": duplicated versions " + matches;
        if (matches.isEmpty()) {
            return new int[]{TestFile.EcmaVersion.MAX_VERSION};
        }
        int[] versions = new int[matches.size()];
        for (int i = 0; i < matches.size(); i++) {
            String match = matches.get(i);
            if (match.equals(PENDING_ECMA_VERSION_LINE)) {
                versions[i] = TestFile.EcmaVersion.MAX_VERSION;
            } else {
                versions[i] = Integer.parseInt(match);
            }
        }
        return versions;
    }

    private static Source createSource(File testFile, String code, boolean module) {
        return Source.newBuilder("js", code, (module ? "module:" : "") + testFile.getPath()).buildLiteral();
    }

    private static String getNegativeMessage(List<String> scriptCode) {
        boolean lookForType = false;
        for (String line : scriptCode) {
            if (!lookForType) {
                Matcher matcher = NEGATIVE_PREFIX.matcher(line);
                if (matcher.find()) {
                    String candidate = line.substring(matcher.end());
                    if (candidate.length() > 0) {
                        return candidate;
                    } else {
                        lookForType = true;
                    }
                }
            } else {
                Matcher matcher = NEGATIVE_TYPE_PREFIX.matcher(line);
                if (matcher.find()) {
                    return line.substring(matcher.end());
                }
            }
        }
        return null;
    }

    private static Stream<String> getStrings(List<String> scriptCode, String prefix, Pattern pattern) {
        for (String line : scriptCode) {
            if (line.contains(prefix)) {
                Matcher matcher = pattern.matcher(line);
                if (matcher.find()) {
                    return SPLIT_PATTERN.splitAsStream(matcher.group(1));
                }
            }
        }
        return Stream.empty();
    }

    private static Set<String> getFlags(List<String> scriptCode) {
        return getStrings(scriptCode, FLAGS_PREFIX, FLAGS_PATTERN).collect(Collectors.toSet());
    }

    private static Stream<String> getIncludes(List<String> scriptCode) {
        Stream<String> includes = getStrings(scriptCode, INCLUDES_PREFIX, INCLUDES_PATTERN);

        // There are few tests whose "includes:" section has the form
        // includes:
        // - propertyHelper.js
        for (String line : scriptCode) {
            if ("- propertyHelper.js".equals(line.trim())) {
                assert includes.count() == 0;
                includes = Stream.of("propertyHelper.js");
            }
        }
        return includes;
    }

    private static boolean isAsyncTest(List<String> scriptCodeList) {
        return scriptCodeList.stream().anyMatch(s -> s.contains("$DONE"));
    }

    private static OutputStream makeDualStream(OutputStream s1, OutputStream s2) {
        return new OutputStream() {

            @Override
            public void write(int b) throws IOException {
                s1.write(b);
                s2.write(b);
            }
        };
    }
}
