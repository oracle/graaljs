/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.js.lang.JavaScriptLanguage.MODULE_MIME_TYPE;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
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
import com.oracle.truffle.js.runtime.JSTruffleOptions;
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
    private static final String FEATURES_PREFIX = "features: ";
    private static final String ONLY_STRICT_FLAG = "onlyStrict";
    private static final String MODULE_FLAG = "module";
    private static final String CAN_BLOCK_IS_FALSE_FLAG = "CanBlockIsFalse";
    private static final Pattern FLAGS_PATTERN = Pattern.compile("flags: \\[((?:(?:, )?(?:\\w+))*)\\]");
    private static final Pattern INCLUDES_PATTERN = Pattern.compile("includes: \\[(.*)\\]");
    private static final Pattern FEATURES_PATTERN = Pattern.compile("features: \\[(.*)\\]");
    private static final Pattern SPLIT_PATTERN = Pattern.compile(", ");

    private static final Map<String, String> commonOptions;
    static {
        Map<String, String> options = new HashMap<>();
        options.put(JSContextOptions.INTL_402_NAME, "true");
        commonOptions = Collections.unmodifiableMap(options);
    }

    private static final Set<String> SUPPORTED_FEATURES = new HashSet<>(Arrays.asList(new String[]{
                    "Array.prototype.flat",
                    "Array.prototype.flatMap",
                    "Array.prototype.values",
                    "ArrayBuffer",
                    "Atomics",
                    "BigInt",
                    "DataView",
                    "DataView.prototype.getInt16",
                    "DataView.prototype.getInt32",
                    "DataView.prototype.getInt8",
                    "DataView.prototype.getFloat32",
                    "DataView.prototype.getFloat64",
                    "DataView.prototype.getUint16",
                    "DataView.prototype.getUint32",
                    "DataView.prototype.setUint8",
                    "Float32Array",
                    "Float64Array",
                    "Int32Array",
                    "Int8Array",
                    "Intl.ListFormat",
                    "Intl.NumberFormat-unified",
                    "Intl.RelativeTimeFormat",
                    "Intl.Segmenter",
                    "Map",
                    "Object.fromEntries",
                    "Object.is",
                    "Promise.prototype.finally",
                    "Proxy",
                    "Reflect",
                    "Reflect.construct",
                    "Reflect.set",
                    "Reflect.setPrototypeOf",
                    "Set",
                    "SharedArrayBuffer",
                    "String.fromCodePoint",
                    "String.prototype.endsWith",
                    "String.prototype.includes",
                    "String.prototype.matchAll",
                    "String.prototype.trimStart",
                    "String.prototype.trimEnd",
                    "Symbol",
                    "Symbol.asyncIterator",
                    "Symbol.hasInstance",
                    "Symbol.isConcatSpreadable",
                    "Symbol.iterator",
                    "Symbol.match",
                    "Symbol.matchAll",
                    "Symbol.prototype.description",
                    "Symbol.replace",
                    "Symbol.search",
                    "Symbol.species",
                    "Symbol.split",
                    "Symbol.toPrimitive",
                    "Symbol.toStringTag",
                    "Symbol.unscopables",
                    "TypedArray",
                    "Uint16Array",
                    "Uint8Array",
                    "Uint8ClampedArray",
                    "WeakSet",
                    "WeakMap",

                    "arrow-function",
                    "async-functions",
                    "async-iteration",
                    "caller",
                    "class",
                    "computed-property-names",
                    "const",
                    "cross-realm",
                    "default-parameters",
                    "destructuring-assignment",
                    "destructuring-binding",
                    "dynamic-import",
                    "export-star-as-namespace-from-module",
                    "for-of",
                    "generators",
                    "globalThis",
                    "import.meta",
                    "json-superset",
                    "let",
                    "new.target",
                    "object-rest",
                    "object-spread",
                    "optional-catch-binding",
                    "regexp-dotall",
                    "regexp-lookbehind",
                    "regexp-named-groups",
                    "regexp-unicode-property-escapes",
                    "string-trimming",
                    "super",
                    "template",
                    "u180e",
                    "well-formed-json-stringify",
    }));
    private static final Set<String> UNSUPPORTED_FEATURES = new HashSet<>(Arrays.asList(new String[]{
                    "Intl.Locale",
                    "IsHTMLDDA",
                    "class-fields-private",
                    "class-fields-public",
                    "class-methods-private",
                    "class-static-fields-private",
                    "class-static-fields-public",
                    "class-static-methods-private",
                    "numeric-separator-literal",
                    "tail-call-optimization"
    }));
    private static final Set<String> ES2020_FEATURES = new HashSet<>(Arrays.asList(new String[]{
                    "dynamic-import",
                    "import.meta"
    }));

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
        boolean agentCannotBlock = flags.contains(CAN_BLOCK_IS_FALSE_FLAG);

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

        Source[] harnessSources = ((Test262) suite).getHarnessSources(runStrict, asyncTest, getIncludes(scriptCodeList));

        final Map<String, String> options;
        if (agentCannotBlock) {
            options = new HashMap<>(commonOptions);
            options.put(JSContextOptions.AGENT_CAN_BLOCK_NAME, "false");
        } else {
            options = commonOptions;
        }

        boolean supported = true;
        boolean requiresES2020 = false;
        for (String feature : getFeatures(scriptCodeList)) {
            if (SUPPORTED_FEATURES.contains(feature)) {
                if (ES2020_FEATURES.contains(feature)) {
                    requiresES2020 = true;
                }
            } else {
                assert UNSUPPORTED_FEATURES.contains(feature) : feature;
                supported = false;
            }
        }

        TestFile.EcmaVersion ecmaVersion = testFile.getEcmaVersion();
        if (ecmaVersion == null) {
            int version = requiresES2020 ? JSTruffleOptions.ECMAScript2020 : JSTruffleOptions.LatestECMAScriptVersion;
            ecmaVersion = TestFile.EcmaVersion.forVersions(version);
        }

        if (supported) {
            testFile.setResult(runTest(ecmaVersion, version -> runInternal(version, file, testSource, negative, asyncTest, negativeExpectedMessage, harnessSources, options)));
        } else {
            testFile.setStatus(TestFile.Status.SKIP);
        }
    }

    private TestFile.Result runInternal(int ecmaVersion, File file, org.graalvm.polyglot.Source testSource, boolean negative, boolean asyncTest, String negativeExpectedMessage,
                    Source[] harnessSources, Map<String, String> options) {
        final String ecmaVersionSuffix = " (ES" + ecmaVersion + ")";
        suite.logVerbose(getName() + ecmaVersionSuffix);
        TestFile.Result testResult;

        OutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        OutputStream outputStream = byteArrayOutputStream;
        if (getConfig().isPrintFullOutput()) {
            outputStream = makeDualStream(byteArrayOutputStream, System.out);
        }

        TestCallable tc = new TestCallable(suite, harnessSources, testSource, file, ecmaVersion, options);
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
            testResult = TestFile.Result.timeout(e);
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

            testResult = TestFile.Result.failed(cause);
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

    private static Source createSource(File testFile, String code, boolean module) {
        Source.Builder builder = Source.newBuilder("js", testFile).content(code);
        if (module) {
            builder.mimeType(MODULE_MIME_TYPE);
        }
        return builder.buildLiteral();
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

    private static Set<String> getFeatures(List<String> scriptCode) {
        return getStrings(scriptCode, FEATURES_PREFIX, FEATURES_PATTERN).collect(Collectors.toSet());
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
