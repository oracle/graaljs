/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.test.suite;

import static com.oracle.truffle.js.lang.JavaScriptLanguage.APPLICATION_MIME_TYPE;
import static com.oracle.truffle.js.lang.JavaScriptLanguage.ID;
import static com.oracle.truffle.js.lang.JavaScriptLanguage.MODULE_MIME_TYPE;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.junit.internal.TextListener;
import org.junit.runner.Description;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.manipulation.Filter;
import org.junit.runner.manipulation.NoTestsRemainException;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.ParentRunner;
import org.junit.runners.model.InitializationError;

import com.oracle.truffle.js.runtime.JSContextOptions;
import com.oracle.truffle.js.test.polyglot.PolyglotBuiltinTest;
import com.oracle.truffle.js.test.suite.JSTestRunner.TestCase;

public final class JSTestRunner extends ParentRunner<TestCase> {

    private static final String FIXTURE_DIR = File.separator + "fixtures" + File.separator;
    private static final String SCRIPT_SUFFIX = ".js";
    private static final String MODULE_SUFFIX = ".mjs";

    private static final String OPTION_REGEX = "@option\\s+([^=\\s]+)(?:\\s*=\\s*(\\S+))?$";
    private static final String ARGUMENT_REGEX = "@argument\\s+([^=\\s]+)$";

    private static final String LF = System.getProperty("line.separator");
    private static final boolean USE_NASHORN_COMPAT_MODE = Boolean.getBoolean("polyglot.js.nashorn-compat");

    static class TestCase {
        protected final Description testName;
        protected final String sourceName;
        protected final Path sourceFile;

        protected TestCase(Class<?> testClass, String baseName, String sourceName, Path sourceFile) {
            this.testName = Description.createTestDescription(testClass, baseName);
            this.sourceName = sourceName;
            this.sourceFile = sourceFile;
        }
    }

    private final List<TestCase> testCases;

    public JSTestRunner(Class<?> runningClass) throws InitializationError {
        super(runningClass);
        try {
            testCases = createTests(runningClass);
        } catch (IOException e) {
            throw new InitializationError(e);
        }
    }

    @Override
    protected Description describeChild(TestCase child) {
        return child.testName;
    }

    @Override
    protected List<TestCase> getChildren() {
        return testCases;
    }

    protected static List<TestCase> createTests(final Class<?> c) throws IOException, InitializationError {
        JSTestSuite suite = c.getAnnotation(JSTestSuite.class);
        if (suite == null) {
            throw new InitializationError(String.format("@%s annotation required on class '%s' to run with '%s'.", JSTestSuite.class.getSimpleName(), c.getName(), JSTestRunner.class.getSimpleName()));
        }

        Path root = Paths.get(suite.value());
        boolean pathExists = Files.exists(root);

        if (!pathExists) {
            return new ArrayList<>();
        }

        final List<TestCase> foundCases = new ArrayList<>();
        Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path sourceFile, BasicFileAttributes attrs) throws IOException {
                String sourceFilePath = sourceFile.toString();
                String sourceName = sourceFile.getFileName().toString();
                String suffix = findSuffix(sourceName);
                boolean isFixture = sourceFilePath.contains(FIXTURE_DIR);
                if (suffix != null && !isFixture) {
                    String baseName = sourceName.substring(0, sourceName.length() - suffix.length());
                    foundCases.add(new TestCase(c, baseName, sourceName, sourceFile));
                }
                return FileVisitResult.CONTINUE;
            }
        });
        return foundCases;
    }

    private static String findSuffix(String fileName) {
        if (fileName.endsWith(SCRIPT_SUFFIX)) {
            return SCRIPT_SUFFIX;
        } else if (fileName.endsWith(MODULE_SUFFIX)) {
            return MODULE_SUFFIX;
        } else {
            return null;
        }
    }

    public static String readAllLines(Path file) throws IOException {
        // fix line feeds for non unix os
        StringBuilder outFile = new StringBuilder();
        for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            outFile.append(line).append(LF);
        }
        return outFile.toString();
    }

    @Override
    protected void runChild(TestCase testCase, RunNotifier notifier) {
        try {
            String sourceLines = readAllLines(testCase.sourceFile);

            if (hasOption(sourceLines, "ignore-test")) {
                notifier.fireTestIgnored(testCase.testName);
                return;
            }

            notifier.fireTestStarted(testCase.testName);

            Map<String, String> options = parseOptions(sourceLines);

            boolean optionNashornCompat = Boolean.parseBoolean(options.getOrDefault(JSContextOptions.NASHORN_COMPATIBILITY_MODE_NAME, "false"));
            boolean optionV8Compat = Boolean.parseBoolean(options.getOrDefault(JSContextOptions.V8_COMPATIBILITY_MODE_NAME, "false"));

            if (USE_NASHORN_COMPAT_MODE && (optionV8Compat || !optionNashornCompat)) {
                // bailout, don't run in both nashorn-compat mode
                // when not expected by test or when test expects v8 mode
                return;
            }

            options.put(JSContextOptions.DEBUG_BUILTIN_NAME, "true");
            options.put(JSContextOptions.SHARED_ARRAY_BUFFER_NAME, "true");
            options.put(JSContextOptions.INTL_402_NAME, "true");

            String[] args = parseArgs(sourceLines);
            // allowHostAccess, allowIO, allowHostReflection
            Context engineContext = Context.newBuilder().allowExperimentalOptions(true).options(options).allowAllAccess(true).arguments(ID, args).build();

            engineContext.enter();
            setTestGlobals(engineContext, optionNashornCompat || USE_NASHORN_COMPAT_MODE);
            engineContext.leave();
            String mimeType = testCase.sourceName.endsWith(MODULE_SUFFIX) ? MODULE_MIME_TYPE : APPLICATION_MIME_TYPE;
            Source source = Source.newBuilder(ID, testCase.sourceFile.toFile()).name(testCase.sourceName).content(sourceLines).mimeType(mimeType).build();
            engineContext.eval(source);
        } catch (Throwable ex) {
            notifier.fireTestFailure(new Failure(testCase.testName, ex));
        } finally {
            notifier.fireTestFinished(testCase.testName);
        }
    }

    private static Map<String, String> parseOptions(String sourceLines) {
        Map<String, String> options = new LinkedHashMap<>();
        Pattern patternOptions = Pattern.compile(OPTION_REGEX, Pattern.MULTILINE);
        Matcher matcher = patternOptions.matcher(sourceLines);
        String optionName;
        String optionValue;
        while (matcher.find()) {
            optionName = matcher.group(1);  // retrieve only option name
            optionValue = matcher.group(2); // retrieve only option value
            if (!optionName.startsWith(JSContextOptions.JS_OPTION_PREFIX)) {
                optionName = JSContextOptions.JS_OPTION_PREFIX + optionName;
            }
            if (optionValue == null) {  // check for the optional true/false after "="
                optionValue = "true";
            }
            options.put(optionName, optionValue);
        }
        return options;
    }

    private static String[] parseArgs(String sourceLines) {
        Pattern patternArguments = Pattern.compile(ARGUMENT_REGEX, Pattern.MULTILINE);
        Matcher matcher = patternArguments.matcher(sourceLines);
        List<String> args = new LinkedList<>();
        while (matcher.find()) {
            args.add(matcher.group(1));
        }
        return args.toArray(new String[0]);
    }

    private static boolean hasOption(String sourceLines, String optionName) {
        return sourceLines.contains("@option " + optionName);
    }

    private static void setTestGlobals(Context context, boolean inNashornMode) {
        Value globalBindings = context.getBindings("js");
        globalBindings.putMember("OptionNashornCompat", inNashornMode);
        PolyglotBuiltinTest.addTestPolyglotBuiltins(context);
    }

    public static void runInMain(Class<?> testClass, String[] args) throws InitializationError, NoTestsRemainException {
        JUnitCore core = new JUnitCore();
        core.addListener(new TextListener(System.out));
        JSTestRunner suite = new JSTestRunner(testClass);
        if (args.length > 0) {
            suite.filter(new NameFilter(args[0]));
        }
        Result r = core.run(suite);
        if (!r.wasSuccessful()) {
            System.exit(1);
        }
    }

    private static final class NameFilter extends Filter {
        private final String pattern;

        private NameFilter(String pattern) {
            this.pattern = pattern.toLowerCase();
        }

        @Override
        public boolean shouldRun(Description description) {
            return description.getMethodName().toLowerCase().contains(pattern);
        }

        @Override
        public String describe() {
            return "Filter contains " + pattern;
        }
    }

}
