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
package com.oracle.truffle.js.test.external.suite;

import java.io.BufferedReader;
import java.io.Console;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.parser.GraalJSParserOptions;
import com.oracle.truffle.js.runtime.UserScriptException;
import com.oracle.truffle.js.runtime.objects.JSObject;

public abstract class TestSuite {

    public static final int OVERALL_TIMEOUT_SECONDS = 60 * 12;
    public static final int INDIVIDUAL_TIMEOUT_SECONDS = 30;

    private static final char LINE_SEPARATOR = '\n';
    private static final int MAX_IGNORE_TIMEOUT_COUNT = 3;
    private static final int REPORTED_STACK_TRACE_ELEMENTS = 10;
    private static final Pattern JS_FILE_PATTERN = Pattern.compile(".+\\.js$");
    private static final boolean PRINT_PROGRESS = System.console() != null;

    private final SuiteConfig config;
    private final ConsolePrintStream consolePrintStream = new ConsolePrintStream(System.out);
    private final List<TestFile> testFiles = new ArrayList<>();
    private final AtomicInteger testIndex = new AtomicInteger();

    private TestsConfig parsedConfig;
    private Map<String, TestFile> testsConfig;
    private Map<String, TestFile> runInIsolationTests = new LinkedHashMap<>();
    private Map<String, TestFile> shouldRunAndFailTests = new LinkedHashMap<>();
    private Map<String, TestFile> skippedTests = new LinkedHashMap<>();
    private Map<String, TestFile> failedTests = new LinkedHashMap<>();
    private Map<String, TestFile> ignoredTests = new LinkedHashMap<>();
    private int passedCount;
    private int timeoutCount;
    private int testCount;
    private List<String> textOutputList;
    private final List<String> htmlOutputList;
    private final List<TestRunnable> activeTests = new ArrayList<>();

    public TestSuite(SuiteConfig config) {
        assert config != null;
        this.config = config;

        System.setOut(consolePrintStream);

        this.htmlOutputList = config.isHtmlOutput() ? new ArrayList<>() : null;
        this.textOutputList = config.isTextOutput() ? new ArrayList<>() : null;
    }

    public final SuiteConfig getConfig() {
        return config;
    }

    /**
     * Get array of prequel files for the given ECMAScript version.
     *
     * @param ecmaVersion ECMAScript version
     * @return array of prequel files, never {@code null}
     */
    public abstract String[] getPrequelFiles(int ecmaVersion);

    protected abstract TestRunnable createTestRunnable(TestFile file);

    protected abstract void setupTestFile(TestFile testFile);

    protected abstract File getTestsConfigFile();

    protected abstract File getUnexpectedlyFailedTestsFile();

    /**
     * This function defines whether the actual executors are run as separate threads. This enables
     * per-thread timeouts, e.g. for testing other (non well-behaving) engines.
     */
    public boolean executeWithSeparateThreads() {
        return false;
    }

    public String getHTMLFileName() {
        throw new UnsupportedOperationException("no html file name specified for this suite");
    }

    public String getReportFileName() {
        throw new UnsupportedOperationException("no text file name specified for this suite");
    }

    protected void printSkippedFilesHTML(PrintStream stream, List<TestFile> skippedFiles) {
        printFilesHTML(stream, skippedFiles);
    }

    protected void printIgnoredFilesHTML(PrintStream stream, List<TestFile> ignoredFiles) {
        printFilesHTML(stream, ignoredFiles);
    }

    private void printFilesHTML(PrintStream stream, List<TestFile> files) {
        stream.println("<ul>");
        for (TestFile testFile : files) {
            stream.println("<li><a href=\"" + resolveTestFilePath(testFile).getAbsolutePath() + "\">" + testFile.getFilePath() + "</a></li>");
        }
        stream.println("</ul>");
    }

    private void parseTestsConfig() {
        if (parsedConfig != null) {
            return;
        }
        File testsConfigFile = getTestsConfigFile();
        if (!testsConfigFile.isFile()) {
            throw new IllegalStateException("Non-existing tests config file: " + testsConfigFile);
        }
        long start = System.currentTimeMillis();
        try {
            parsedConfig = JSONUtil.deserialize(testsConfigFile, TestsConfig.class);
            long parseTime = System.currentTimeMillis();
            logVerbose("JSON deserializing took: " + (parseTime - start) + " ms\n");
            JSONUtil.checkFormatting(testsConfigFile, parsedConfig);
            logVerbose("JSON formatting check took: " + (System.currentTimeMillis() - parseTime) + " ms\n");
        } catch (IOException e) {
            throw new IllegalStateException("Cannot deserialize tests config", e);
        } catch (JSONUtil.IncorrectFormattingException e) {
            if (askYesNoQuestion("Configuration file " + testsConfigFile.getName() + " not properly formatted. Regenerate it? [y/N]")) {
                regenerateConfig(null, null);
                System.out.println();
            } else {
                throw new IllegalStateException("Incorrect file formatting", e);
            }
        }
        verifyAndInitTestsConfig(testsConfigFile);
    }

    private void verifyAndInitTestsConfig(File testsConfigFile) {
        testsConfig = new HashMap<>();
        Set<TestFile> invalidTestFiles = new HashSet<>();
        for (TestFile testFile : parsedConfig.getTestFiles()) {
            String filePath = testFile.getFilePath();
            if (filePath == null) {
                throw new IllegalStateException("Attribute \"filePath\" must be defined for every test file! Verify '" + testsConfigFile.getName() + "' file!");
            }
            if (!resolveTestFilePath(testFile).isFile()) {
                System.out.println("Configured file '" + filePath + "' does not exist. Verify '" + testsConfigFile.getName() + "' file!");
                invalidTestFiles.add(testFile);
            } else {
                TestFile checker = testsConfig.put(filePath, testFile);
                assert checker == null : "Test file '" + filePath + "' configured more than once! Verify '" + testsConfigFile.getName() + "' file!";
            }
        }
        if (!invalidTestFiles.isEmpty()) {
            if (askYesNoQuestion("Non-existing files found in configuration. Regenerate config? [y/N]")) {
                regenerateConfig(null, invalidTestFiles);
            }
            System.out.println();
        }
    }

    /**
     * @see #relativizeTestPath(File)
     */
    public File resolveTestFilePath(TestFile testFile) {
        return resolveTestPath(testFile.getFilePath());
    }

    /**
     * @see #relativizeTestPath(File)
     */
    public File resolveTestPath(String testFile) {
        return Paths.get(config.getSuiteTestsLoc(), testFile).toFile();
    }

    /**
     * @see #resolveTestFilePath(TestFile)
     * @see #resolveTestPath(String)
     */
    public String relativizeTestPath(File testFile) {
        return Paths.get(config.getSuiteTestsLoc()).relativize(testFile.toPath()).toString().replace(File.separatorChar, '/');
    }

    private List<TestFile> getIgnoredFiles() {
        parseTestsConfig();
        return new ArrayList<>(ignoredTests.values());
    }

    private List<TestFile> getSkippedFiles() {
        parseTestsConfig();
        return new ArrayList<>(skippedTests.values());
    }

    private boolean isSkipped(TestFile testFile) {
        return skippedTests.containsKey(testFile.getFilePath());
    }

    private List<TestFile> getRunInIsolationFiles() {
        parseTestsConfig();
        return new ArrayList<>(runInIsolationTests.values());
    }

    private void findTestFiles(String[] selectedTestDirs) {
        // find all the test files
        testFiles.clear();
        for (String dir : selectedTestDirs) {
            findTests(new File(config.getSuiteTestsLoc(), dir), testFiles);
        }
        if (testFiles.isEmpty()) {
            // cannot find any file with the given filter. Maybe the filter is a valid filename?
            Stream.of(config.getEndsWithFilter(), config.getContainsFilter()).filter(Objects::nonNull).findFirst().ifPresent(filter -> findSingleFile(testFiles, filter));
        }
        if (testFiles.isEmpty()) {
            System.err.println("Cannot find test files. Suite location: " + config.getSuiteTestsLoc() + "; ends-with filter: " + config.getEndsWithFilter() + "; contains filter: " +
                            config.getContainsFilter() + "; regex filter: " +
                            config.getRegexFilter());
        }
    }

    private void findSingleFile(List<TestFile> files, String filter) {
        if (filter != null) {
            File maybeFile = new File(filter);
            if (maybeFile.isFile()) {
                files.add(createTestFile(maybeFile));
            }
        }
    }

    private void findTests(File dir, List<TestFile> list) {
        File[] directFiles = dir.listFiles();
        if (directFiles == null) {
            return;
        }
        for (File file : directFiles) {
            if (file.isDirectory()) {
                findTests(file, list);
            } else if (JS_FILE_PATTERN.matcher(file.getName()).matches() && isTestExecutable(file.getName())) {
                Path relativePath = Paths.get(config.getSuiteTestsLoc()).relativize(file.toPath());
                if ((config.getContainsFilter() == null || relativePath.toString().contains(config.getContainsFilter())) &&
                                (config.getRegexFilter() == null || relativePath.toString().matches(config.getRegexFilter())) &&
                                (config.getEndsWithFilter() == null || relativePath.endsWith(config.getEndsWithFilter()))) {
                    if ((config.getContainsFilter() != null) || config.getRegexFilter() != null) {
                        log("adding test file: " + relativePath);
                    }
                    list.add(createTestFile(file));
                }
            }
        }
    }

    private TestFile createTestFile(File file) {
        parseTestsConfig();
        // create test file
        TestFile testFile = new TestFile(relativizeTestPath(file));
        testFile.setStatus(TestFile.Status.PASS);
        testFile.setRunInIsolation(false);
        setupTestFile(testFile);
        // possibly merge with config
        final String filePath = testFile.getFilePath();
        TestFile configuredTestFile = testsConfig.get(filePath);
        if (configuredTestFile != null) {
            testFile = TestFileUtil.merge(testFile, configuredTestFile);
        }
        // fill maps
        if (testFile.getRunInIsolation()) {
            runInIsolationTests.put(filePath, testFile);
        }
        processStatus(testFile.getRealStatus(), testFile);
        return testFile;
    }

    private void processStatus(TestFile.Status status, TestFile testFile) {
        switch (status) {
            case PASS:
                // noop
                break;
            case FAIL:
                shouldRunAndFailTests.put(testFile.getFilePath(), testFile);
                break;
            case SKIP:
                skippedTests.put(testFile.getFilePath(), testFile);
                break;
            default:
                assert false : "Unknown test status: " + status;
        }
    }

    private boolean gateCheck(int unexpectedFailedTestsCount) {
        System.out.println(config.getSuiteDescription() + " failed: " + unexpectedFailedTestsCount + ", timeouts: " + timeoutCount);
        if (unexpectedFailedTestsCount > 0) {
            if (unexpectedFailedTestsCount == timeoutCount && timeoutCount <= MAX_IGNORE_TIMEOUT_COUNT) {
                // to avoid random timeouts to prevent from pushing
                System.out.println("WARNING: " + config.getSuiteDescription() + " gate had " + timeoutCount + " timeouts. Testsuite assumes that to be safe.");
            } else {
                System.out.println("FAILED the " + config.getSuiteDescription() + " gate");
                return false;
            }
        }
        System.out.println();
        System.out.println("OK, passed the " + config.getSuiteDescription() + " gate");
        return true;
    }

    private void printHTMLOutput(String result) {
        try (PrintStream htmlStream = new PrintStream(new FileOutputStream(getHTMLFileName()))) {
            htmlStream.println("<html><head><title>" + config.getSuiteDescription() + " output</title><head><body>" + (new Date()).toString() + "<br/><br/>");

            htmlStream.println("<h3>Failing Tests</h3>");
            htmlStream.println("<ul>");

            String[] outputArray = htmlOutputList.toArray(new String[0]);
            Arrays.sort(outputArray);

            for (String s : outputArray) {
                htmlStream.println("<li>" + s + "</li>");
            }

            htmlStream.println("</ul>");

            List<TestFile> skippedFiles = getSkippedFiles();
            if (!skippedFiles.isEmpty()) {
                htmlStream.println("<h3>Skipped Tests</h3>");
                printSkippedFilesHTML(htmlStream, skippedFiles);
            }

            List<TestFile> ignoredFiles = getIgnoredFiles();
            if (!ignoredFiles.isEmpty()) {
                htmlStream.println("<h3>Ignored Tests</h3>");
                printIgnoredFilesHTML(htmlStream, ignoredFiles);
            }

            htmlStream.println(result);
            htmlStream.println("</body></html>");
        } catch (Exception ex) {
            System.out.println("Cannot write HTML file: " + ex.getMessage());
        }
    }

    private void printTextOutput() {
        try (PrintStream textOutputStream = new PrintStream(new FileOutputStream(getReportFileName()))) {
            for (String line : textOutputList) {
                textOutputStream.println(line);
            }
        } catch (FileNotFoundException ex) {
            System.out.println("cannot write output file: " + ex.getMessage());
        }
    }

    public int calcNumberOfCores(int availableNumberOfCores) {
        if (config.isCompile()) {
            return Math.min(availableNumberOfCores, 2);
        } else {
            return availableNumberOfCores;
        }
    }

    private void regenerateConfig(Collection<TestFile> addTests, Collection<TestFile> removeTests) {
        long start = System.currentTimeMillis();
        File testsConfigFile = getTestsConfigFile();
        if (addTests != null) {
            List<TestFile> addTestsMerged = new ArrayList<>(addTests.size());
            for (TestFile addTest : addTests) {
                TestFile currentTest = testsConfig.get(addTest.getFilePath());
                final TestFile newTest;
                if (currentTest != null) {
                    newTest = TestFileUtil.merge(currentTest, addTest);
                } else {
                    newTest = addTest;
                }
                addTestsMerged.add(newTest);
            }
            parsedConfig.addTests(addTestsMerged);
        }
        if (removeTests != null) {
            parsedConfig.removeTests(removeTests);
        }
        try {
            JSONUtil.serialize(testsConfigFile, parsedConfig);
            logVerbose("JSON serializing took: " + (System.currentTimeMillis() - start) + " ms\n");
            System.out.println("Wrote new configuration file " + testsConfigFile.getName());
        } catch (IOException e) {
            throw new IllegalStateException("Cannot serialize tests config", e);
        }
    }

    @SuppressWarnings("unused")
    protected boolean isTestExecutable(String name) {
        return true;
    }

    public final void printProgress(TestFile testFile) {
        if (PRINT_PROGRESS) {
            // test index
            int index = testIndex.incrementAndGet();
            // percents
            int percents = index * 100 / testCount;
            String filePath = testFile.getFilePath();
            consolePrintStream.printProgress(String.format("[%3s%%] %s", percents, filePath));
        }
    }

    public final void logVerbose(String message) {
        if (config.isVerbose()) {
            log(message);
        }
    }

    public final void log(String message) {
        synchronized (System.out) {
            System.out.println(message);
            if (config.isTextOutput()) {
                textOutputList.add(message);
            }
        }
    }

    public final void logFail(TestFile testFile, String failMessage, String reason) {
        synchronized (System.out) {
            final String filename = testFile.getFilePath();
            if (!config.isRunOnGate() && (config.isVerbose() || config.isVerboseFail())) {
                System.out.println(failMessage + " " + filename + " " + escapeForSysout(reason));
            }
            if (config.isTextOutput()) {
                textOutputList.add(failMessage + " " + filename + " " + escapeForReport(reason));
            }
            if (htmlOutputList != null) {
                htmlOutputList.add(formatLogFailHTML(filename, failMessage, escapeForSysout(reason)));
            }
            if (config.isRunOnGate()) {
                if (!shouldRunAndFailTests.keySet().contains(filename)) {
                    if (!(config.isVerbose() || config.isVerboseFail())) {
                        System.out.println(failMessage + " " + filename + " " + escapeForSysout(reason));
                    }
                }
            }
        }
    }

    public final void logFail(TestFile testFile, String failMessage, Throwable cause) {
        String reason = getDetailedCause(cause);
        if (config.isPrintFullOutput()) {
            logFail(testFile, failMessage, reason);
            cause.printStackTrace();
        } else {
            logFail(testFile, failMessage, reason + " " + cause + " " + getBackTrace(cause));
        }
    }

    private static String getDetailedCause(Throwable cause) {
        if (cause instanceof UserScriptException) {
            UserScriptException use = (UserScriptException) cause;
            Object exceptionObject = use.getErrorObject();
            if (exceptionObject instanceof DynamicObject) {
                return String.valueOf(JSObject.get((DynamicObject) exceptionObject, "message"));
            }
        }
        return "";
    }

    private static String escapeForSysout(String reason) {
        return reason == null ? "null" : reason.replace("\n", " ").replace("\r", "");
    }

    private static String escapeForReport(String reason) {
        return reason == null ? "null" : reason.replace("\n", " ").replace("\r", "");
    }

    protected String formatLogFailHTML(String filename, String failMessage, String reason) {
        return failMessage + " <a href=\"" + config.getSuiteTestsLoc() + filename + "\">" + filename + "</a> " + encodeHTML(reason);
    }

    private static String encodeHTML(String reason) {
        return reason.replace("<", "&lt;");
    }

    public int runTestSuite(String[] selectedTestDirs) {
        long startTime = System.currentTimeMillis();

        deleteFiles(getReportFileName(), getHTMLFileName());

        boolean isFilterSet = config.getContainsFilter() != null || config.getEndsWithFilter() != null;
        if (config.isRunOnGate() && isFilterSet) {
            System.out.println("Warning: gate and filter mutually exclude each other!");
        }

        findTestFiles(selectedTestDirs);
        if (testFiles.isEmpty()) {
            log("Error: no test files found, exiting");
            return -1;
        }
        testCount = testFiles.size() - skippedTests.size();
        // sort files
        testFiles.sort(TestFile.COMPARATOR);
        Set<TestFile> orderedTestFiles = new LinkedHashSet<>();
        if (config.isRunOnGate() && !isFilterSet && config.isGateResume()) {
            log("Resuming from previously unexpectedly failed tests (or more precisely folders)");
            orderedTestFiles.addAll(getPreviouslyFailedTests());
            if (orderedTestFiles.isEmpty()) {
                log("No previously unexpectedly failed tests found, starting from beginning");
            }
            System.out.println();
        }
        orderedTestFiles.addAll(testFiles);

        List<TestFile> runInIsolation = getRunInIsolationFiles();
        List<Runnable> isolatedRunnables = new ArrayList<>();
        List<TestFile> skippedFiles = getSkippedFiles();
        log("Executing on JDK: " + System.getProperty("java.version") + "; Found tests: " + testFiles.size() +
                        (skippedFiles.isEmpty() ? "" : "; Skipped tests: " + skippedFiles.size()) +
                        (runInIsolation.isEmpty() ? "" : "; Isolated tests: " + runInIsolation.size()));

        // run the tests
        findAndExecute(orderedTestFiles, isolatedRunnables);
        for (Runnable runnable : isolatedRunnables) {
            runnable.run();
        }

        // clear progress
        if (PRINT_PROGRESS) {
            consolePrintStream.printProgress(null);
        }

        // analyze results
        for (TestFile testFile : testFiles) {
            if (testFile.hasRun()) {
                if (testFile.hasPassed()) {
                    passedCount++;
                } else if (testFile.isIgnored()) {
                    ignoredTests.put(testFile.getFilePath(), testFile);
                } else {
                    assert testFile.getResult().isFailure() : testFile;
                    failedTests.put(testFile.getFilePath(), testFile);
                    if (testFile.getResult().isTimeout()) {
                        timeoutCount++;
                    }
                }
            } else {
                assert isSkipped(testFile) : testFile;
            }
        }
        analyzeResult(startTime);

        if (config.isTextOutput()) {
            printTextOutput();
        }
        if (config.isRunOnGate() && !isFilterSet) {
            Map<String, TestFile> unexpectedlyPassed = checkUnexpectedlyPassedTests();
            Map<String, TestFile> unexpectedlyFailed = checkUnexpectedlyFailedTests();
            storeUnexpectedlyFailedTests(unexpectedlyFailed.keySet());
            return analyzeGateResult(unexpectedlyPassed.values(), unexpectedlyFailed.values());
        }
        return 0;
    }

    private static void deleteFiles(String... files) {
        for (String file : files) {
            File f = new File(file);
            if (f.exists()) {
                if (!f.delete()) {
                    throw new IllegalStateException("Cannot delete file: " + f);
                }
            }
        }
    }

    private Collection<TestFile> getPreviouslyFailedTests() {
        File file = getUnexpectedlyFailedTestsFile();
        if (!file.isFile()) {
            return Collections.emptySet();
        }
        List<String> lines;
        try {
            lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read unexpectedly failed tests file " + file.getName(), e);
        }
        if (lines.isEmpty()) {
            return Collections.emptySet();
        }
        List<TestFile> previouslyFailedTests = new ArrayList<>();
        int isolated = 0;
        int skipped = 0;
        boolean isVerbose = config.isVerbose();
        for (TestFile testFile : testFiles) {
            String filePath = testFile.getFilePath();
            for (String folder : lines) {
                if (filePath.startsWith(folder) && filePath.indexOf('/', folder.length()) == -1) {
                    if (isVerbose) {
                        logVerbose("Resuming test '" + filePath + "' for folder '" + folder + "'");
                    }
                    previouslyFailedTests.add(testFile);
                    if (testFile.getRunInIsolation()) {
                        isolated++;
                    } else if (isSkipped(testFile)) {
                        skipped++;
                    }
                    break;
                }
            }
        }
        if (isolated > 0 || skipped > 0) {
            log("(Skipped tests: " + skipped + "; Isolated tests: " + isolated + ")");
        }
        return previouslyFailedTests;
    }

    private void storeUnexpectedlyFailedTests(Collection<String> unexpectedlyFailed) {
        File file = getUnexpectedlyFailedTestsFile();
        if (unexpectedlyFailed.isEmpty()) {
            if (file.isFile() && !file.delete()) {
                log("Warning: Cannot delete unexpectedly failed tests file " + file.getName());
            }
            return;
        }
        Set<String> dirs = new LinkedHashSet<>();
        for (String testPath : unexpectedlyFailed) {
            int index = testPath.lastIndexOf('/');
            if (index != -1) {
                dirs.add(testPath.substring(0, index + 1));
            } else {
                dirs.add(testPath);
            }
        }
        try {
            Files.write(file.toPath(), dirs, StandardCharsets.UTF_8);
            logVerbose("Unexpectedly failed tests written to " + file.getName() + "\n");
        } catch (IOException e) {
            throw new IllegalStateException("Cannot store unexpectedly failed tests file " + file.getName(), e);
        }
    }

    private int analyzeGateResult(Collection<TestFile> unexpectedlyPassed, Collection<TestFile> unexpectedlyFailed) {
        boolean gatePassed = gateCheck(unexpectedlyFailed.size());
        if (!unexpectedlyPassed.isEmpty() || !unexpectedlyFailed.isEmpty()) {
            if (config.isRegenerateConfig() || askYesNoQuestion((gatePassed ? "" : "WARNING: GATE FAILED. ") + "Update configuration file? [y/N]")) {
                String comment = "";
                if (!unexpectedlyFailed.isEmpty()) {
                    comment = askForInput("Common comment, if any:").trim();
                }
                List<TestFile> addTests = new ArrayList<>(unexpectedlyFailed.size());
                for (TestFile testFile : unexpectedlyFailed) {
                    TestFile failingTestFile = new TestFile(testFile.getFilePath());
                    EnumMap<TestFile.Endianness, TestFile.Status> statusOverrides = testFile.getStatusOverrides();
                    if (statusOverrides != null) {
                        failingTestFile.setStatus(testFile.getStatus());
                        statusOverrides.put(TestFile.Endianness.current(), TestFile.Status.FAIL);
                        failingTestFile.setStatusOverrides(statusOverrides);
                    } else {
                        failingTestFile.setStatus(TestFile.Status.FAIL);
                    }
                    if (!comment.isEmpty()) {
                        failingTestFile.setComment(comment);
                    }
                    addTests.add(failingTestFile);
                }
                regenerateConfig(addTests, unexpectedlyPassed);
            }
        } else if (config.isRegenerateConfig() && askYesNoQuestion((gatePassed ? "" : "WARNING: GATE FAILED. ") + "Regenerate configuration file? [y/N]")) {
            regenerateConfig(null, null);
        }
        return gatePassed ? 0 : 1;
    }

    private void findAndExecute(Collection<TestFile> orderedTestFiles, List<Runnable> isolatedRunnables) {
        ExecutorService exe = null;
        List<Callable<Void>> callables = null;
        if (config.isUseThreads()) {
            callables = new ArrayList<>(orderedTestFiles.size());
            exe = initThreads();
        }
        for (TestFile testFile : orderedTestFiles) {
            if (!isSkipped(testFile)) {
                TestRunnable runnable = createTestRunnable(testFile);
                if (config.isUseThreads()) {
                    if (testFile.getRunInIsolation()) {
                        isolatedRunnables.add(runnable);
                    } else {
                        callables.add(() -> {
                            runnable.run();
                            return null;
                        });
                    }
                } else {
                    runnable.run();
                }
            }
        }
        if (config.isUseThreads()) {
            try {
                List<Future<Void>> results = exe.invokeAll(callables, config.getTimeoutOverall(), TimeUnit.SECONDS);
                checkResults(results);
            } catch (InterruptedException e) {
                log("Interrupted exception, exiting: " + e.getMessage());
                System.exit(-1);
            }
        }
    }

    private Map<String, TestFile> checkUnexpectedlyPassedTests() {
        Map<String, TestFile> unexpectedlyPassed = new LinkedHashMap<>();
        for (TestFile testFile : shouldRunAndFailTests.values()) {
            if (testFile.hasRun() && testFile.hasPassed()) {
                System.out.println("Unexpectedly passed '" + testFile.getFilePath() + "' (expected status: " + testFile.getStatus().name() + "), please update the configuration file!");
                unexpectedlyPassed.put(testFile.getFilePath(), testFile);
            }
        }
        if (!unexpectedlyPassed.isEmpty()) {
            System.out.println();
        }
        return unexpectedlyPassed;
    }

    private Map<String, TestFile> checkUnexpectedlyFailedTests() {
        Map<String, TestFile> unexpectedlyFailed = new LinkedHashMap<>();
        for (TestFile testFile : failedTests.values()) {
            assert testFile.hasRun() : testFile;
            assert !testFile.hasPassed() : testFile;
            if (testFile.getRealStatus() != TestFile.Status.FAIL) {
                System.out.println("Unexpectedly failed '" + testFile.getFilePath() + "', please update the configuration file!");
                unexpectedlyFailed.put(testFile.getFilePath(), testFile);
            }
        }
        if (!unexpectedlyFailed.isEmpty()) {
            System.out.println();
        }
        return unexpectedlyFailed;
    }

    private static boolean askYesNoQuestion(String prompt) {
        return askForInput(prompt).toLowerCase().startsWith("y");
    }

    private static String askForInput(String prompt) {
        Console console = System.console();
        if (console == null) {
            return "";
        }
        String input = console.readLine(prompt + " ");
        if (input == null) {
            return "";
        }
        return input;
    }

    private ExecutorService initThreads() {
        int numberOfCores = Runtime.getRuntime().availableProcessors();
        int usingNumberOfCores = Math.min(calcNumberOfCores(numberOfCores), 4);
        logVerbose("Number of cores available: " + numberOfCores + ", using: " + usingNumberOfCores);
        return executeWithSeparateThreads() ? Executors.newFixedThreadPool(usingNumberOfCores, TestThread::new) : Executors.newFixedThreadPool(usingNumberOfCores);
    }

    private void checkResults(List<Future<Void>> results) throws InterruptedException {
        Throwable noClassDefFoundError = null;
        for (Future<?> result : results) {
            try {
                assert result.isDone();
                if (result.isCancelled()) {
                    log("Overall TIMEOUT, exiting " + config.getSuiteDescription());
                    System.exit(-1);
                }
                result.get();
            } catch (ExecutionException e) {
                if (e.getCause() instanceof NoClassDefFoundError) {
                    // ignore this cause, there will be likely some ExceptionInInitializerError
                    // see https://bugs.openjdk.java.net/browse/JDK-8051847
                    if (noClassDefFoundError == null) {
                        noClassDefFoundError = e.getCause();
                    }
                    continue;
                }
                log("Uncaught exception, exiting: " + e.getMessage());
                e.printStackTrace();
                System.exit(-1);
            }
        }
        if (noClassDefFoundError != null) {
            log("Uncaught exception, exiting: " + noClassDefFoundError.getMessage());
            noClassDefFoundError.printStackTrace();
            System.exit(-1);
        }
    }

    private void analyzeResult(long startTime) {
        int ignoredCount = ignoredTests.size();
        int failedCount = failedTests.size();
        int skippedCount = getSkippedFiles().size();
        int totalCount = passedCount + failedCount;
        int totalWithSkippedCount = totalCount + skippedCount;
        double passedProportion = totalCount == 0 ? 0 : (double) passedCount / totalCount;
        double passedWithSkippedProportion = totalWithSkippedCount == 0 ? 0 : (double) passedCount / totalWithSkippedCount;
        String passedPercentWithSkippedFormatted = formatPercent(passedWithSkippedProportion, failedCount);
        String passedPercentFormatted = formatPercent(passedProportion, failedCount);

        String summary = "\n== SUMMARY ==\n" +
                        "Runtime: " + runtime(System.currentTimeMillis() - startTime) + "\n" +
                        "Excluding skipped: " + passedPercentFormatted + "% (" + passedCount + "/" + totalCount + ") passed\n" +
                        "Including skipped: " + passedPercentWithSkippedFormatted + "% (" + passedCount + "/" + totalWithSkippedCount + ") passed\n" +
                        (ignoredCount > 0 ? "Ignored tests:     " + ignoredCount + "\n" : "");

        if (config.isVerbose() && failedCount > 0) {
            logVerbose("");
            logVerbose("== FAILING TESTS ==");
            for (String failedTest : failedTests.keySet()) {
                logVerbose(failedTest);
            }
        }
        if (config.isVerbose() && ignoredCount > 0) {
            logVerbose("");
            logVerbose("== IGNORED TESTS ==");
            for (String ignoredTest : ignoredTests.keySet()) {
                logVerbose(ignoredTest);
            }
        }

        log(summary);

        if (config.isHtmlOutput()) {
            printHTMLOutput(summary.replace("\n", "<br/>"));
        }
    }

    private static String runtime(long millis) {
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(minutes);
        return String.format("%dm %ds %dms", minutes, seconds, millis - TimeUnit.MINUTES.toMillis(minutes) - TimeUnit.SECONDS.toMillis(seconds));
    }

    private static String formatPercent(double passedProportion, int failedCount) {
        if (passedProportion == 0.0 && failedCount == 0) {
            return "?";
        }
        DecimalFormat percentFormat = new DecimalFormat("#.#");
        String passedPercentFormatted = percentFormat.format(passedProportion * 100);
        if (passedPercentFormatted.equals("100") && failedCount > 0) {
            // rounding error; don't print "100%" when only 99.9% are passed
            passedPercentFormatted = "99.9";
        }
        return passedPercentFormatted;
    }

    public static List<String> readFileContentList(File pfile) {
        return readFileContentList(pfile, false);
    }

    public static List<String> readFileContentList(File pfile, boolean ignoreEmptyAndComments) {
        List<String> list = new ArrayList<>();
        try (FileInputStream fis = new FileInputStream(pfile)) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(fis, "UTF-8"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (ignoreEmptyAndComments && (line.equals("") || line.startsWith("#"))) {
                        // ignore empty lines of lines starting with a comment ("#")
                    } else {
                        list.add(line);
                    }
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return list;
    }

    public static String toPrintableCode(List<String> code) {
        return toPrintableCode(code, LINE_SEPARATOR);
    }

    public static String toPrintableCode(List<String> code, char separator) {
        StringBuilder text = new StringBuilder();
        for (String line : code) {
            text.append(line);
            text.append(separator);
        }
        return text.toString();
    }

    protected GraalJSParserOptions getParserOptions() {
        return new GraalJSParserOptions();
    }

    public String getBackTrace(Throwable cause) {
        return Arrays.stream(cause.getStackTrace()).limit(REPORTED_STACK_TRACE_ELEMENTS).map(StackTraceElement::toString).collect(Collectors.joining(", ", "at ", ""));
    }

    public List<TestRunnable> getActiveTests() {
        assert Thread.holdsLock(this);
        return activeTests;
    }

    public static void parseDefaultArgs(String arg, SuiteConfig config) {
        String lowercaseArg = arg.toLowerCase(Locale.US);
        if (lowercaseArg.equals("help")) {
            System.out.println("usage: " + config.getSuiteName() + " [gate [regenerateconfig] [resume]] [verbose|verbosefail] [printscript] [regression] [filter=] [single=] [nothreads]\n");
            System.out.println(" gate                   run the gate tests (checking against expected conformance)");
            System.out.println(" regenerateconfig       after running the gate, write new configuration file");
            System.out.println(" resume                 run previously failed tests first");
            System.out.println(" regression             writes " + config.getSuiteName() + ".txt and " + config.getSuiteName() + ".html result files");
            System.out.println(" filter=X               executes only tests that have X in their filename");
            System.out.println(" regex=X                executes only tests that have their filename matching given regex");
            System.out.println(" single=X               executes only tests that match filename X");
            System.out.println(" printscript            print sourcecode of all executed scripts (use in combination with \"filter\")");
            System.out.println(" verbose                print all tests");
            System.out.println(" verbosefail            print failing tests");
            System.out.println(" nothreads              run all tests in the main thread");
            System.out.println(" compile                execute the tests repeatedly so they are compiled by Graal - handle with care");
            System.out.println(" timeoutoverall=X       overall testrun aborted after X seconds");
            System.out.println(" timeouttest=X          test aborted after X seconds. Not available in all modes");
            System.out.println(" location=X             the base directory of the test suite");
            System.out.println(" config=X               the base directory of the test suite config file");
            System.out.println(" outputfilter=x         ignore a given string when comparing with the expected output");
            System.exit(-2);
        } else if (lowercaseArg.equals("nothreads")) {
            config.setUseThreads(false);
        } else if (lowercaseArg.equals("verbose")) {
            config.setVerbose(true);
        } else if (lowercaseArg.equals("verbosefail")) {
            config.setVerbose(false);
            config.setVerboseFail(true);
        } else if (lowercaseArg.equals("printscript")) {
            config.setPrintScript(true);
        } else if (lowercaseArg.equals("compile")) {
            config.setCompile(true);
        } else if (lowercaseArg.equals("regression")) {
            config.setVerbose(false);
            config.setVerboseFail(false);
            config.setHtmlOutput(true);
            config.setTextOutput(true);
        } else if (lowercaseArg.equals("gate")) {
            config.setVerbose(false);
            config.setVerboseFail(false);
            config.setHtmlOutput(true);
            config.setTextOutput(true);
            config.setRunOnGate(true);
        } else if (lowercaseArg.equals("regenerateconfig")) {
            config.setRegenerateConfig(true);
            config.setRunOnGate(true); // forcing gate
        } else if (lowercaseArg.equals("resume")) {
            config.setGateResume(true);
            config.setRunOnGate(true); // forcing gate
        } else if (lowercaseArg.startsWith("timeouttest=")) {
            int timeout = parseArgInteger(arg);
            config.setTimeoutTest(timeout);
        } else if (lowercaseArg.startsWith("timeoutoverall=")) {
            int timeout = parseArgInteger(arg);
            config.setTimeoutOverall(timeout);
        } else if (lowercaseArg.startsWith("filter=")) {
            String filter = arg.substring("filter=".length());
            config.setContainsFilter(filter);
        } else if (lowercaseArg.startsWith("regex=")) {
            String filter = arg.substring("regex=".length());
            config.setRegexFilter(filter);
        } else if (lowercaseArg.startsWith("single=")) {
            String filter = arg.substring("single=".length());
            config.setEndsWithFilter(filter);
            config.setPrintFullOutput(true);
            config.setVerboseFail(true);
        } else if (lowercaseArg.startsWith("location=")) {
            String location = arg.substring("location=".length());
            config.setSuiteLoc(location);
        } else if (lowercaseArg.startsWith("config=")) {
            String configLoc = arg.substring("config=".length());
            config.setSuiteConfigLoc(configLoc);
        } else if (lowercaseArg.startsWith("outputfilter=")) {
            String outputFilter = arg.substring("outputfilter=".length());
            config.setOutputFilter(outputFilter);
        } else {
            System.out.println("unrecognized argument: " + arg + "\nCall \"" + config.getSuiteName() + " help\" for more information.");
            System.exit(-2);
        }
    }

    private static int parseArgInteger(String arg) {
        String str = arg.substring(arg.indexOf("=") + 1);
        return Integer.parseInt(str);
    }

    public static class TestThread extends Thread {

        private ExecutorService executor = Executors.newSingleThreadExecutor();

        public TestThread(Runnable r) {
            super(r);
        }

        public ExecutorService getExecutor() {
            return executor;
        }
    }

    private static final class ConsolePrintStream extends PrintStream {

        // @GuardedBy("this")
        private String progressMessage;
        // @GuardedBy("this")
        private int delegatingDepth;

        ConsolePrintStream(OutputStream out) {
            super(out);
        }

        public synchronized void printProgress(String message) {
            clearProgress();
            progressMessage = message;
            printProgress();
        }

        @Override
        public synchronized void print(boolean b) {
            clearProgress();
            delegatingDepth++;
            super.print(b);
            delegatingDepth--;
            printProgress();
        }

        @Override
        public synchronized void print(char c) {
            clearProgress();
            delegatingDepth++;
            super.print(c);
            delegatingDepth--;
            printProgress();
        }

        @Override
        public synchronized void print(int i) {
            clearProgress();
            delegatingDepth++;
            super.print(i);
            delegatingDepth--;
            printProgress();
        }

        @Override
        public synchronized void print(long l) {
            clearProgress();
            delegatingDepth++;
            super.print(l);
            delegatingDepth--;
            printProgress();
        }

        @Override
        public synchronized void print(float f) {
            clearProgress();
            delegatingDepth++;
            super.print(f);
            delegatingDepth--;
            printProgress();
        }

        @Override
        public synchronized void print(double d) {
            clearProgress();
            delegatingDepth++;
            super.print(d);
            delegatingDepth--;
            printProgress();
        }

        @Override
        public synchronized void print(char[] s) {
            clearProgress();
            delegatingDepth++;
            super.print(s);
            delegatingDepth--;
            printProgress();
        }

        @Override
        public synchronized void print(String s) {
            clearProgress();
            delegatingDepth++;
            super.print(s);
            delegatingDepth--;
            printProgress();
        }

        @Override
        public synchronized void print(Object obj) {
            clearProgress();
            delegatingDepth++;
            super.print(obj);
            delegatingDepth--;
            printProgress();
        }

        @Override
        public synchronized void println() {
            clearProgress();
            delegatingDepth++;
            super.println();
            delegatingDepth--;
            printProgress();
        }

        @Override
        public synchronized void println(boolean x) {
            clearProgress();
            delegatingDepth++;
            super.println(x);
            delegatingDepth--;
            printProgress();
        }

        @Override
        public synchronized void println(char x) {
            clearProgress();
            delegatingDepth++;
            super.println(x);
            delegatingDepth--;
            printProgress();
        }

        @Override
        public synchronized void println(int x) {
            clearProgress();
            delegatingDepth++;
            super.println(x);
            delegatingDepth--;
            printProgress();
        }

        @Override
        public synchronized void println(long x) {
            clearProgress();
            delegatingDepth++;
            super.println(x);
            delegatingDepth--;
            printProgress();
        }

        @Override
        public synchronized void println(float x) {
            clearProgress();
            delegatingDepth++;
            super.println(x);
            delegatingDepth--;
            printProgress();
        }

        @Override
        public synchronized void println(double x) {
            clearProgress();
            delegatingDepth++;
            super.println(x);
            delegatingDepth--;
            printProgress();
        }

        @Override
        public synchronized void println(char[] x) {
            clearProgress();
            delegatingDepth++;
            super.println(x);
            delegatingDepth--;
            printProgress();
        }

        @Override
        public synchronized void println(String x) {
            clearProgress();
            delegatingDepth++;
            super.println(x);
            delegatingDepth--;
            printProgress();
        }

        @Override
        public synchronized void println(Object x) {
            clearProgress();
            delegatingDepth++;
            super.println(x);
            delegatingDepth--;
            printProgress();
        }

        private void clearProgress() {
            assert Thread.holdsLock(this);
            if (delegatingDepth == 0 && progressMessage != null) {
                int length = progressMessage.length();
                char[] backspaces = new char[length];
                Arrays.fill(backspaces, 0, length, '\b');
                char[] spaces = new char[length];
                Arrays.fill(spaces, 0, length, ' ');
                super.print(backspaces);
                super.print(spaces);
                super.print(backspaces);
            }
        }

        private void printProgress() {
            assert Thread.holdsLock(this);
            if (delegatingDepth == 0 && progressMessage != null) {
                super.print(progressMessage);
            }
        }

    }

}
