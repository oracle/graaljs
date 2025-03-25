/*
 * Copyright (c) 2018, 2025, Oracle and/or its affiliates. All rights reserved.
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;

import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.io.IOAccess;

import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.JSContextOptions;
import com.oracle.truffle.js.test.external.suite.ForwardingFileSystem;
import com.oracle.truffle.js.test.external.suite.TestCallable;
import com.oracle.truffle.js.test.external.suite.TestExtProcessCallable;
import com.oracle.truffle.js.test.external.suite.TestFile;
import com.oracle.truffle.js.test.external.suite.TestRunnable;
import com.oracle.truffle.js.test.external.suite.TestSuite;
import com.oracle.truffle.js.test.external.suite.TestSuite.TestThread;

public class Test262Runnable extends TestRunnable {
    private static final String ASYNC_TEST_COMPLETE = "Test262:AsyncTestComplete";
    private static final String ONLY_STRICT_FLAG = "onlyStrict";
    private static final String ASYNC_FLAG = "async";
    private static final String MODULE_FLAG = "module";
    private static final String RAW_FLAG = "raw";
    private static final String CAN_BLOCK_IS_FALSE_FLAG = "CanBlockIsFalse";
    private static final Pattern FRONTMATTER_PATTERN = Pattern.compile(Pattern.quote("/*---") + "(.*)" + Pattern.quote("---*/"), Pattern.DOTALL);

    private static final Set<String> SUPPORTED_FEATURES = featureSet(new String[]{
                    "AggregateError",
                    "Array.fromAsync",
                    "Array.prototype.at",
                    "Array.prototype.flat",
                    "Array.prototype.flatMap",
                    "Array.prototype.includes",
                    "Array.prototype.values",
                    "ArrayBuffer",
                    "Atomics",
                    "Atomics.pause",
                    "Atomics.waitAsync",
                    "BigInt",
                    "DataView",
                    "DataView.prototype.getFloat32",
                    "DataView.prototype.getFloat64",
                    "DataView.prototype.getInt16",
                    "DataView.prototype.getInt32",
                    "DataView.prototype.getInt8",
                    "DataView.prototype.getUint16",
                    "DataView.prototype.getUint32",
                    "DataView.prototype.setUint8",
                    "Error.isError",
                    "FinalizationRegistry",
                    "FinalizationRegistry.prototype.cleanupSome",
                    "Float16Array",
                    "Float32Array",
                    "Float64Array",
                    "Int16Array",
                    "Int32Array",
                    "Int8Array",
                    "Intl-enumeration",
                    "Intl.DateTimeFormat-datetimestyle",
                    "Intl.DateTimeFormat-dayPeriod",
                    "Intl.DateTimeFormat-extend-timezonename",
                    "Intl.DateTimeFormat-formatRange",
                    "Intl.DateTimeFormat-fractionalSecondDigits",
                    "Intl.DisplayNames",
                    "Intl.DisplayNames-v2",
                    "Intl.DurationFormat",
                    "Intl.ListFormat",
                    "Intl.Locale",
                    "Intl.Locale-info",
                    "Intl.NumberFormat-unified",
                    "Intl.NumberFormat-v3",
                    "Intl.RelativeTimeFormat",
                    "Intl.Segmenter",
                    "Map",
                    "Math.sumPrecise",
                    "Object.fromEntries",
                    "Object.hasOwn",
                    "Object.is",
                    "Promise",
                    "Promise.allSettled",
                    "Promise.any",
                    "Promise.prototype.finally",
                    "Proxy",
                    "Reflect",
                    "Reflect.construct",
                    "Reflect.set",
                    "Reflect.setPrototypeOf",
                    "RegExp.escape",
                    "Set",
                    "ShadowRealm",
                    "SharedArrayBuffer",
                    "String.fromCodePoint",
                    "String.prototype.at",
                    "String.prototype.endsWith",
                    "String.prototype.includes",
                    "String.prototype.isWellFormed",
                    "String.prototype.matchAll",
                    "String.prototype.replaceAll",
                    "String.prototype.toWellFormed",
                    "String.prototype.trimEnd",
                    "String.prototype.trimStart",
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
                    "Temporal",
                    "TypedArray",
                    "TypedArray.prototype.at",
                    "Uint16Array",
                    "Uint32Array",
                    "Uint8Array",
                    "Uint8ClampedArray",
                    "WeakMap",
                    "WeakRef",
                    "WeakSet",

                    "__getter__",
                    "__proto__",
                    "__setter__",
                    "align-detached-buffer-semantics-with-web-reality",
                    "arbitrary-module-namespace-names",
                    "array-find-from-last",
                    "array-grouping",
                    "arraybuffer-transfer",
                    "arrow-function",
                    "async-functions",
                    "async-iteration",
                    "caller",
                    "canonical-tz",
                    "change-array-by-copy",
                    "class",
                    "class-fields-private",
                    "class-fields-private-in",
                    "class-fields-public",
                    "class-methods-private",
                    "class-static-block",
                    "class-static-fields-private",
                    "class-static-fields-public",
                    "class-static-methods-private",
                    "cleanupSome",
                    "coalesce-expression",
                    "computed-property-names",
                    "const",
                    "cross-realm",
                    "decorators",
                    "default-parameters",
                    "destructuring-assignment",
                    "destructuring-binding",
                    "dynamic-import",
                    "error-cause",
                    "exponentiation",
                    "export-star-as-namespace-from-module",
                    "for-in-order",
                    "for-of",
                    "generators",
                    "globalThis",
                    "hashbang",
                    "host-gc-required",
                    "import-assertions",
                    "import-attributes",
                    "import.meta",
                    "intl-normative-optional",
                    "iterator-helpers",
                    "iterator-sequencing",
                    "json-modules",
                    "json-parse-with-source",
                    "json-superset",
                    "legacy-regexp",
                    "let",
                    "logical-assignment-operators",
                    "new.target",
                    "numeric-separator-literal",
                    "object-rest",
                    "object-spread",
                    "optional-catch-binding",
                    "optional-chaining",
                    "promise-try",
                    "promise-with-resolvers",
                    "proxy-missing-checks",
                    "regexp-dotall",
                    "regexp-duplicate-named-groups",
                    "regexp-lookbehind",
                    "regexp-match-indices",
                    "regexp-modifiers",
                    "regexp-named-groups",
                    "regexp-unicode-property-escapes",
                    "regexp-v-flag",
                    "resizable-arraybuffer",
                    "rest-parameters",
                    "set-methods",
                    "source-phase-imports",
                    "source-phase-imports-module-source",
                    "string-trimming",
                    "super",
                    "symbols-as-weakmap-keys",
                    "template",
                    "top-level-await",
                    "u180e",
                    "uint8array-base64",
                    "well-formed-json-stringify",
    });
    private static final Set<String> UNSUPPORTED_FEATURES = featureSet(new String[]{
                    "IsHTMLDDA",
                    "explicit-resource-management",
                    "import-defer",
                    "tail-call-optimization",
    });
    private static final Set<String> STAGING_FEATURES = featureSet(new String[]{
                    "Array.fromAsync",
                    "Atomics.pause",
                    "Error.isError",
                    "FinalizationRegistry.prototype.cleanupSome",
                    "Float16Array",
                    "Intl.DurationFormat",
                    "Intl.Locale-info",
                    "Math.sumPrecise",
                    "RegExp.escape",
                    "ShadowRealm",
                    "decorators",
                    "iterator-sequencing",
                    "json-parse-with-source",
                    "promise-try",
                    "uint8array-base64",
    });

    public Test262Runnable(TestSuite suite, TestFile testFile) {
        super(suite, testFile);
    }

    @Override
    public void run() {
        suite.printProgress(testFile);
        final File file = suite.resolveTestFilePath(testFile);
        String scriptCode = TestSuite.readFileContent(file);
        String frontmatter = getFrontmatter(scriptCode);
        Map<String, Object> meta = SimpleYAML.parseMap(frontmatter);
        String negativeExpectedMessage = getNegativeMessage(meta);
        boolean negative = negativeExpectedMessage != null;
        Set<String> flags = getFlags(meta);
        Set<String> features = getFeatures(meta);
        boolean runStrict = flags.contains(ONLY_STRICT_FLAG);
        boolean asyncTest = flags.contains(ASYNC_FLAG);
        boolean module = flags.contains(MODULE_FLAG);

        Map<String, String> extraOptions = new HashMap<>(4);
        if (flags.contains(CAN_BLOCK_IS_FALSE_FLAG)) {
            extraOptions.put(JSContextOptions.AGENT_CAN_BLOCK_NAME, "false");
        }
        if (features.contains("error-cause")) {
            extraOptions.put(JSContextOptions.ERROR_CAUSE_NAME, "true");
        }
        if (features.contains("import-attributes")) {
            extraOptions.put(JSContextOptions.IMPORT_ATTRIBUTES_NAME, "true");
        }
        if (features.contains("import-assertions")) {
            extraOptions.put(JSContextOptions.IMPORT_ASSERTIONS_NAME, "true");
        }
        if (features.contains("json-modules")) {
            extraOptions.put(JSContextOptions.JSON_MODULES_NAME, "true");
        }
        if (features.contains("Temporal")) {
            extraOptions.put(JSContextOptions.TEMPORAL_NAME, "true");
        }
        if (features.contains("ShadowRealm")) {
            extraOptions.put(JSContextOptions.SHADOW_REALM_NAME, "true");
        }
        if (features.contains("regexp-v-flag")) {
            extraOptions.put(JSContextOptions.REGEXP_UNICODE_SETS_NAME, "true");
        }
        if (features.contains("iterator-helpers")) {
            extraOptions.put(JSContextOptions.ITERATOR_HELPERS_NAME, "true");
        }
        if (features.contains("set-methods")) {
            extraOptions.put(JSContextOptions.NEW_SET_METHODS_NAME, "true");
        }
        if (features.contains("source-phase-imports")) {
            extraOptions.put(JSContextOptions.SOURCE_PHASE_IMPORTS_NAME, "true");
        }

        assert !asyncTest || !negative || negativeExpectedMessage.equals("SyntaxError") : "unsupported async negative test (does not expect an early SyntaxError): " + testFile.getFilePath();

        if (getConfig().isPrintScript()) {
            synchronized (suite) {
                System.out.println("================================================================");
                System.out.println("====== Testcase: " + getName());
                System.out.println("================================================================");
                System.out.println();
                System.out.println(scriptCode);
            }
        }

        Source[] harnessSources;
        if (flags.contains(RAW_FLAG)) {
            /*
             * `raw`: The test source code must not be modified in any way, files from the harness/
             * directory must not be evaluated, and the test must be executed just once (in
             * non-strict mode, only).
             */
            assert getIncludes(meta).size() == 0 && !runStrict && !asyncTest;
            harnessSources = new Source[0];
        } else {
            harnessSources = ((Test262) suite).getHarnessSources(runStrict, asyncTest, getIncludes(meta).stream(), testFile.getFilePath());
        }

        boolean supported = true;
        int minESVersion = suite.getConfig().getMinESVersion();
        int featureVersion = minESVersion;
        for (String feature : features) {
            if (SUPPORTED_FEATURES.contains(feature)) {
                assert !UNSUPPORTED_FEATURES.contains(feature) : feature;
                if (STAGING_FEATURES.contains(feature)) {
                    featureVersion = JSConfig.StagingECMAScriptVersion;
                }
            } else {
                assert UNSUPPORTED_FEATURES.contains(feature) : feature;
                supported = false;
            }
        }

        if (features.contains("source-phase-imports-module-source")) {
            assert features.contains("source-phase-imports") : "feature source-phase-imports-module-source requires source-phase-imports";
            extraOptions.put(JSContextOptions.UNHANDLED_REJECTIONS_NAME, "throw");
            // "<module source>" is provided by a WebAssembly.Module via polyglot FileSystem.
            if (getConfig().isPolyglot() && !getConfig().isExtLauncher()) {
                // enable webassembly, so that we can compile the dummy module source
                extraOptions.put(JSContextOptions.WEBASSEMBLY_NAME, "true");
            } else {
                supported = false;
            }
        }

        TestFile.EcmaVersion ecmaVersion = testFile.getEcmaVersion();
        if (ecmaVersion == null) {
            ecmaVersion = TestFile.EcmaVersion.forVersions(featureVersion);
        } else {
            ecmaVersion = ecmaVersion.filterByMinVersion(minESVersion);
        }
        String prefix = runStrict ? "\"use strict\";" : "";
        Source testSource = null;
        testSource = createSource(file, prefix, scriptCode, module);
        final Source src = testSource;
        if (supported) {
            testFile.setResult(runTest(ecmaVersion, version -> runInternal(version, file, src, negative, asyncTest, runStrict, module, negativeExpectedMessage, harnessSources, extraOptions)));
        } else {
            testFile.setStatus(TestFile.Status.SKIP);
        }
    }

    private TestFile.Result runInternal(int ecmaVersion, File file, Source testSource, boolean negative, boolean asyncTest, boolean strict, boolean module, String negativeExpectedMessage,
                    Source[] harnessSources, Map<String, String> extraOptions) {
        suite.logVerbose(getName() + ecmaVersionToString(ecmaVersion));
        TestFile.Result testResult;

        long startDate = System.currentTimeMillis();
        reportStart();

        OutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        OutputStream outputStream = byteArrayOutputStream;
        if (getConfig().isPrintFullOutput()) {
            outputStream = makeDualStream(byteArrayOutputStream, System.out);
        }
        if (suite.getConfig().isExtLauncher()) {
            testResult = runExternalLauncher(ecmaVersion, testSource, negative, asyncTest, strict, module, negativeExpectedMessage, harnessSources, extraOptions, byteArrayOutputStream, outputStream);
        } else {
            testResult = runInJVM(ecmaVersion, file, testSource, negative, asyncTest, negativeExpectedMessage, harnessSources, extraOptions, byteArrayOutputStream, outputStream);
        }

        reportEnd(startDate);

        return testResult;
    }

    private TestFile.Result runInJVM(int ecmaVersion, File file, Source testSource, boolean negative, boolean asyncTest, String negativeExpectedMessage, Source[] harnessSources,
                    Map<String, String> extraOptions, OutputStream byteArrayOutputStream, OutputStream outputStream) {
        Future<Object> future = null;
        try {
            IOAccess ioAccess = IOAccess.newBuilder().fileSystem(new ModuleSourceFS()).build();
            TestCallable tc = new TestCallable(suite, harnessSources, testSource, file, ecmaVersion, extraOptions, ioAccess);
            tc.setOutput(outputStream);
            tc.setError(outputStream);
            if (suite.executeWithSeparateThreads() && getConfig().isUseThreads()) {
                Thread t = Thread.currentThread();
                assert t instanceof TestThread;
                future = ((TestThread) t).getExecutor().submit(tc);
                future.get(getConfig().getTimeoutTest(), TimeUnit.SECONDS);
            } else {
                tc.call();
            }
            return processSuccessfulRun(ecmaVersion, negative, asyncTest, byteArrayOutputStream);
        } catch (TimeoutException e) {
            logTimeout(ecmaVersion);
            if (future != null) {
                if (!future.cancel(true)) {
                    suite.logVerbose("Could not cancel!" + getName());
                }
            }
            return TestFile.Result.timeout(e);
        } catch (Throwable cause) {
            if (suite.executeWithSeparateThreads()) {
                cause = cause.getCause() != null ? cause.getCause() : cause;
            }
            if (negative) {
                if (cause instanceof PolyglotException) {
                    return processFailedNegativeRun(ecmaVersion, negativeExpectedMessage, cause.getMessage());
                } else {
                    return failNegativeWrongException(ecmaVersion, negativeExpectedMessage, cause.toString());
                }
            } else {
                logFailure(ecmaVersion, cause);
                return TestFile.Result.failed(cause);
            }
        }
    }

    private TestFile.Result runExternalLauncher(int ecmaVersion, Source testSource, boolean negative, boolean asyncTest, boolean strict, boolean module, String negativeExpectedMessage,
                    Source[] harnessSources, Map<String, String> extraOptions, OutputStream byteArrayOutputStream, OutputStream outputStream) {
        TestExtProcessCallable tc = new TestExtProcessCallable(suite, ecmaVersion, createExtLauncherArgs(harnessSources, testSource, strict, module), extraOptions);
        tc.setOutput(outputStream);
        tc.setError(outputStream);
        try {
            switch (tc.call()) {
                case SUCCESS:
                    return processSuccessfulRun(ecmaVersion, negative, asyncTest, byteArrayOutputStream);
                case FAILURE:
                    String output = byteArrayOutputStream.toString();
                    if (negative) {
                        String errorMessage = extLauncherFindErrorMessage(output);
                        if (errorMessage != null) {
                            return processFailedNegativeRun(ecmaVersion, negativeExpectedMessage, errorMessage);
                        } else {
                            logFailure(ecmaVersion, "negative test, was expected to fail, what it did, but no error message was found in the output:\n\n\"" +
                                            output + "\"\n\n" + "expected: " + negativeExpectedMessage);
                            return TestFile.Result.failed("could not find error message of negative test" + ecmaVersionToString(ecmaVersion));
                        }
                    } else {
                        String error = extLauncherFindError(output);
                        logFailure(ecmaVersion, error);
                        return TestFile.Result.failed(error);
                    }
                case TIMEOUT:
                    logTimeout(ecmaVersion);
                    return TestFile.Result.timeout("TIMEOUT");
                default:
                    throw new IllegalStateException("should not reach here");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static List<String> createExtLauncherArgs(Source[] harnessSources, Source testSource, boolean strict, boolean module) {
        ArrayList<String> args = new ArrayList<>((strict || module ? 4 : 3) + harnessSources.length * (strict ? 2 : 1));
        args.add("--eval");
        args.add(strict ? "\"var strict_mode = true;\"" : "\"var strict_mode = false;\"");
        for (Source prequelSource : harnessSources) {
            assert prequelSource.getPath() != null;
            if (strict) {
                args.add("--strict-file");
            }
            args.add(prequelSource.getPath());
        }
        if (module) {
            args.add("--module");
        } else if (strict) {
            args.add("--strict-file");
        }
        args.add(testSource.getPath());
        return args;
    }

    private TestFile.Result processSuccessfulRun(int ecmaVersion, boolean negative, boolean asyncTest, OutputStream byteArrayOutputStream) {
        if (negative) {
            logFailure(ecmaVersion, "negative test, was expected to fail but didn't");
            return TestFile.Result.failed("negative test expected to fail" + ecmaVersionToString(ecmaVersion));
        }
        if (asyncTest) {
            String stdout = byteArrayOutputStream.toString();
            if (!stdout.contains(ASYNC_TEST_COMPLETE)) {
                logFailure(ecmaVersion, String.format("async test; expected output: '%s' actual: '%s'", ASYNC_TEST_COMPLETE, stdout));
                return TestFile.Result.failed("async test failed" + ecmaVersionToString(ecmaVersion));
            }
        }
        return TestFile.Result.PASSED;
    }

    private TestFile.Result processFailedNegativeRun(int ecmaVersion, String negativeExpectedMessage, String actualMessage) {
        if (actualMessage != null && actualMessage.startsWith(negativeExpectedMessage)) {
            return TestFile.Result.PASSED;
        } else {
            return failNegativeWrongException(ecmaVersion, negativeExpectedMessage, actualMessage);
        }
    }

    private TestFile.Result failNegativeWrongException(int ecmaVersion, String expectedMessage, String actualMessage) {
        logFailure(ecmaVersion, "negative test, was expected to fail, what it did, but for wrong reasons:\n" + actualMessage + "\n" + "expected: " + expectedMessage);
        return TestFile.Result.failed("negative test expected to fail with different reasons" + ecmaVersionToString(ecmaVersion));
    }

    private static Source createSource(File file, String prefix, String code, boolean module) {
        Source.Builder builder = Source.newBuilder("js", file).content(prefix + code);
        if (module) {
            builder.mimeType(MODULE_MIME_TYPE);
        }
        return builder.buildLiteral();
    }

    private static String getFrontmatter(String scriptCode) {
        var m = FRONTMATTER_PATTERN.matcher(scriptCode);
        return m.find() ? m.group(1).stripTrailing().stripIndent() : "";
    }

    private static String getNegativeMessage(Map<String, Object> meta) {
        Map<String, String> negative = getAsMap(meta, "negative", null);
        return negative == null ? null : negative.getOrDefault("type", "");
    }

    private static Set<String> getFlags(Map<String, Object> meta) {
        return Set.copyOf(getAsList(meta, "flags", List.of()));
    }

    private static List<String> getIncludes(Map<String, Object> meta) {
        return getAsList(meta, "includes", List.of());
    }

    private static Set<String> getFeatures(Map<String, Object> meta) {
        return Set.copyOf(getAsList(meta, "features", List.of()));
    }

    @SuppressWarnings("unchecked")
    private static <T> List<T> getAsList(Map<String, Object> meta, String key, List<T> defaultValue) {
        if (meta.get(key) instanceof List<?> list) {
            return (List<T>) list;
        } else {
            return defaultValue;
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> Map<String, T> getAsMap(Map<String, Object> meta, String key, Map<String, T> defaultValue) {
        if (meta.get(key) instanceof Map<?, ?> map) {
            return (Map<String, T>) map;
        } else {
            return defaultValue;
        }
    }

    private static final class ModuleSourceFS extends ForwardingFileSystem {
        private static final String MODULE_SOURCE_SPECIFIER = "<module source>";
        private static final byte[] MINIMAL_WASM_MODULE_BYTES = {0x00, 0x61, 0x73, 0x6d, 0x01, 0x00, 0x00, 0x00};

        @Override
        public Path parsePath(String path) {
            // Replace "<module source>" with an actual existing path.
            if (MODULE_SOURCE_SPECIFIER.equals(path)) {
                return LazyTempFile.minimalWasmModulePath;
            }
            return super.parsePath(path);
        }

        private static final class LazyTempFile {
            private static Path minimalWasmModulePath;

            static {
                try {
                    File tmpFile = File.createTempFile("minimal", ".wasm");
                    tmpFile.deleteOnExit();
                    Files.copy(new ByteArrayInputStream(MINIMAL_WASM_MODULE_BYTES), tmpFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    minimalWasmModulePath = tmpFile.toPath();
                } catch (IOException ioe) {
                    throw new UncheckedIOException(ioe);
                }
            }
        }
    }
}
