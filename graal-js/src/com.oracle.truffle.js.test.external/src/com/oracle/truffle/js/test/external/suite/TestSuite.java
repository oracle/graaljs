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
package com.oracle.truffle.js.test.external.suite;

import java.io.Console;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
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

import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Value;

import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.JSContextOptions;

public abstract class TestSuite {

    public static final int OVERALL_TIMEOUT_SECONDS = 30 * 60;
    public static final int INDIVIDUAL_TIMEOUT_SECONDS = 30;

    private static final char LINE_SEPARATOR = '\n';
    private static final int REPORTED_STACK_TRACE_ELEMENTS = 10;
    private static final Pattern JS_FILE_PATTERN = Pattern.compile(".+\\.m?js$");
    private final boolean printProgress = ConsoleHelper.isTTY();

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
    private final Set<TestRunnable> activeTests = new HashSet<>();
    private final ExecutorService extLauncherPipePool;
    private final Engine sharedEngine;

    @SuppressWarnings("this-escape")
    public TestSuite(SuiteConfig config) {
        assert config != null;
        this.config = config;

        System.setOut(consolePrintStream);

        this.htmlOutputList = config.isHtmlOutput() ? new ArrayList<>() : null;
        this.textOutputList = config.isTextOutput() ? new ArrayList<>() : null;
        this.extLauncherPipePool = config.isExtLauncher() ? Executors.newCachedThreadPool() : null;
        this.sharedEngine = config.isShareEngine() ? createSharedEngine() : null;
    }

    protected Engine createSharedEngine() {
        return Engine.newBuilder().allowExperimentalOptions(true).option("engine.WarnInterpreterOnly", Boolean.toString(false)).build();
    }

    public final SuiteConfig getConfig() {
        return config;
    }

    public ExecutorService getExtLauncherPipePool() {
        assert config.isExtLauncher() && extLauncherPipePool != null;
        return extLauncherPipePool;
    }

    public Engine getSharedEngine() {
        return sharedEngine;
    }

    /**
     * Get array of prequel files for the given ECMAScript version.
     *
     * @param ecmaVersion ECMAScript version
     * @return array of prequel files, never {@code null}
     */
    public abstract String[] getPrequelFiles(int ecmaVersion);

    protected abstract TestRunnable createTestRunnable(TestFile file);

    protected abstract File getTestsConfigFile();

    protected abstract File getUnexpectedlyFailedTestsFile();

    public abstract Map<String, String> getCommonOptions();

    public abstract List<String> getCommonExtLauncherOptions();

    protected static List<String> optionsToExtLauncherOptions(Map<String, String> options) {
        List<String> ret = new ArrayList<>(options.size());
        for (Map.Entry<String, String> entry : options.entrySet()) {
            ret.add(TestExtProcessCallable.optionToString(entry.getKey(), entry.getValue()));
        }
        return Collections.unmodifiableList(ret);
    }

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

    protected boolean isSkipped(TestFile testFile) {
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
        processStatus(testFile.getRealStatus(config), testFile);
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
            System.out.println("FAILED the " + config.getSuiteDescription() + " gate");
            return false;
        }
        System.out.println();
        System.out.println("OK, passed the " + config.getSuiteDescription() + " gate");
        return true;
    }

    private void printHTMLOutput(String result) {
        try (PrintStream htmlStream = new PrintStream(new FileOutputStream(getHTMLFileName()))) {
            htmlStream.println("<html><head><title>" + config.getSuiteDescription() + " output</title></head>");
            htmlStream.println("<body style=\"font-family: monospace;\">");
            htmlStream.println(DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.now()) + "<br/><br/>");

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
        if (printProgress) {
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
        if (cause instanceof PolyglotException polyglotException) {
            Value guestObject = polyglotException.getGuestObject();
            if (guestObject != null) {
                try {
                    if (guestObject.hasMembers()) {
                        Value message = guestObject.getMember("message");
                        if (message != null) {
                            return message.toString();
                        }
                    }
                } catch (Exception ignored) {
                }
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
        return failMessage + " <a href=\"" + config.getSuiteTestsLoc() + "/" + filename + "\">" + filename + "</a> " + encodeHTML(reason);
    }

    private static String encodeHTML(String reason) {
        return reason.replace("<", "&lt;");
    }

    public int runTestSuite(String[] selectedTestDirs) throws InterruptedException {
        Thread activeTestsHook = addActiveTestsShutdownHook();
        try {
            return runTestSuiteImpl(selectedTestDirs);
        } finally {
            Runtime.getRuntime().removeShutdownHook(activeTestsHook);
        }
    }

    private int runTestSuiteImpl(String[] selectedTestDirs) throws InterruptedException {
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
        if (config.isShareEngine()) {
            sharedEngine.close();
        }

        // clear progress
        if (printProgress) {
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
            } else if (!isSkipped(testFile)) {
                // skipped by TestRunnable
                assert (testFile.getStatus() == TestFile.Status.SKIP) : testFile;
                skippedTests.put(testFile.getFilePath(), testFile);
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
                    if (config.isPolyglot()) {
                        EnumMap<TestFile.StatusOverrideCondition, TestFile.Status> statusOverrides = new EnumMap<>(TestFile.StatusOverrideCondition.class);
                        statusOverrides.put(TestFile.StatusOverrideCondition.POLYGLOT, TestFile.Status.FAIL);
                        failingTestFile.setStatusOverrides(statusOverrides);
                        failingTestFile.setStatus(TestFile.Status.SKIP);
                    } else {
                        failingTestFile.setStatus(TestFile.Status.FAIL);
                    }
                    if (!comment.isEmpty()) {
                        failingTestFile.setComment(comment);
                    }
                    addTests.add(failingTestFile);
                }
                if (config.isPolyglot()) {
                    Iterator<TestFile> iter = unexpectedlyPassed.iterator();
                    while (iter.hasNext()) {
                        TestFile testFile = iter.next();
                        TestFile currentTestFile = testsConfig.get(testFile.getFilePath());
                        EnumMap<TestFile.StatusOverrideCondition, TestFile.Status> currentOverrides = currentTestFile.getStatusOverrides();
                        if (currentOverrides != null && currentOverrides.containsKey(TestFile.StatusOverrideCondition.POLYGLOT)) {
                            // This test had POLYGLOT status override => update just this override
                            TestFile passingTestFile = new TestFile(testFile.getFilePath());
                            EnumMap<TestFile.StatusOverrideCondition, TestFile.Status> statusOverrides = new EnumMap<>(currentOverrides);
                            statusOverrides.put(TestFile.StatusOverrideCondition.POLYGLOT, TestFile.Status.PASS);
                            passingTestFile.setStatusOverrides(statusOverrides);
                            addTests.add(passingTestFile);
                            iter.remove();
                        }
                    }
                }
                regenerateConfig(addTests, unexpectedlyPassed);
            }
        } else if (config.isRegenerateConfig() && askYesNoQuestion((gatePassed ? "" : "WARNING: GATE FAILED. ") + "Regenerate configuration file? [y/N]")) {
            regenerateConfig(null, null);
        }
        return gatePassed ? 0 : 1;
    }

    private void findAndExecute(Collection<TestFile> orderedTestFiles, List<Runnable> isolatedRunnables) throws InterruptedException {
        if (config.isUseThreads()) {
            ExecutorService exe = initThreads();
            List<Callable<Void>> callables = new ArrayList<>(orderedTestFiles.size());

            for (TestFile testFile : orderedTestFiles) {
                if (!isSkipped(testFile)) {
                    TestRunnable runnable = createTestRunnable(testFile);
                    if (testFile.getRunInIsolation()) {
                        isolatedRunnables.add(runnable);
                    } else {
                        callables.add(() -> {
                            runnable.run();
                            return null;
                        });
                    }
                }
            }

            List<Future<Void>> results = exe.invokeAll(callables, config.getTimeoutOverall(), TimeUnit.SECONDS);
            checkResults(results);
        } else {
            for (TestFile testFile : orderedTestFiles) {
                if (!isSkipped(testFile)) {
                    createTestRunnable(testFile).run();
                }
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
            if (testFile.getRealStatus(config) != TestFile.Status.FAIL) {
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
        int usingNumberOfCores = Math.min(numberOfCores, 4);
        logVerbose("Number of cores available: " + numberOfCores + ", using: " + usingNumberOfCores);
        return executeWithSeparateThreads() ? Executors.newFixedThreadPool(usingNumberOfCores, TestThread::new) : Executors.newFixedThreadPool(usingNumberOfCores);
    }

    private void checkResults(List<Future<Void>> results) throws InterruptedException {
        Throwable noClassDefFoundError = null;
        for (Future<?> result : results) {
            try {
                assert result.isDone();
                if (result.isCancelled()) {
                    log("Overall TIMEOUT (" + config.getTimeoutOverall() + "s), exiting " + config.getSuiteDescription());
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
        return String.format(Locale.ROOT, "%dm %ds %dms", minutes, seconds, millis - TimeUnit.MINUTES.toMillis(minutes) - TimeUnit.SECONDS.toMillis(seconds));
    }

    private static String formatPercent(double passedProportion, int failedCount) {
        if (passedProportion == 0.0 && failedCount == 0) {
            return "?";
        }
        DecimalFormat percentFormat = new DecimalFormat("#.##");
        String passedPercentFormatted = percentFormat.format(passedProportion * 100);
        if (passedPercentFormatted.equals("100") && failedCount > 0) {
            // rounding error; don't print "100%" when only 99.99% are passed
            passedPercentFormatted = "99.99";
        }
        return passedPercentFormatted;
    }

    public static String readFileContent(File pfile) {
        try {
            byte[] bytes = Files.readAllBytes(pfile.toPath());
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    public static List<String> readFileContentList(File pfile) {
        return readFileContentList(pfile, true);
    }

    public static List<String> readFileContentList(File pfile, boolean keepCRfromCRLF) {
        List<String> list = new ArrayList<>();
        String content = readFileContent(pfile);
        int lastIndex = 0;
        while (lastIndex < content.length()) {
            int index = content.indexOf('\n', lastIndex);
            if (index == -1) {
                list.add(content.substring(lastIndex));
                break;
            } else {
                String line = content.substring(lastIndex, index);
                if (!keepCRfromCRLF && line.endsWith("\r")) {
                    line = line.substring(0, line.length() - 1);
                }
                list.add(line);
                lastIndex = index + 1;
            }
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

    public String getBackTrace(Throwable cause) {
        return Arrays.stream(cause.getStackTrace()).limit(REPORTED_STACK_TRACE_ELEMENTS).map(StackTraceElement::toString).collect(Collectors.joining(", ", "at ", ""));
    }

    public Set<TestRunnable> getActiveTests() {
        assert Thread.holdsLock(this);
        return activeTests;
    }

    private Thread addActiveTestsShutdownHook() {
        Thread hook = new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (TestSuite.this) {
                    System.out.println("ACTIVE TESTS:");
                    for (TestRunnable test : getActiveTests()) {
                        System.out.println(test.getName());
                    }
                }
            }
        });
        Runtime.getRuntime().addShutdownHook(hook);
        return hook;
    }

    public static void parseDefaultArgs(String[] args, SuiteConfig.Builder builder) {
        for (String rawArg : args) {
            String key;
            String value = null;
            int equalsPos = rawArg.indexOf('=');
            if (equalsPos > 0) {
                key = rawArg.substring(0, equalsPos).toLowerCase(Locale.US);
                value = rawArg.substring(equalsPos + 1);
            } else {
                key = rawArg.toLowerCase(Locale.US);
            }
            switch (key) {
                case "help":
                    System.out.println("usage: " + builder.getSuiteName() +
                                    " [gate [regenerateconfig] [resume]] [verbose|verbosefail] [printscript] [regression] [filter=] [single=] [nothreads] [externallauncher=X [compile]]\n");
                    System.out.println(" gate                   run the gate tests (checking against expected conformance)");
                    System.out.println(" regenerateconfig       after running the gate, write new configuration file");
                    System.out.println(" resume                 run previously failed tests first");
                    System.out.println(" regression             writes " + builder.getSuiteName() + ".txt and " + builder.getSuiteName() + ".html result files");
                    System.out.println(" filter=X               executes only tests that have X in their filename");
                    System.out.println(" regex=X                executes only tests that have their filename matching given regex");
                    System.out.println(" single=X               executes only tests that match filename X");
                    System.out.println(" printcommand           print command line of all executed scripts (use in combination with \"filter\")");
                    System.out.println(" printscript            print sourcecode of all executed scripts (use in combination with \"filter\")");
                    System.out.println(" verbose                print all tests");
                    System.out.println(" verbosefail            print failing tests");
                    System.out.println(" nothreads              run all tests in the main thread");
                    System.out.println(" timeoutoverall=X       overall testrun aborted after X seconds");
                    System.out.println(" timeouttest=X          test aborted after X seconds. Not available in all modes");
                    System.out.println(" location=X             the base directory of the test suite");
                    System.out.println(" config=X               the base directory of the test suite config file");
                    System.out.println(" outputfilter=X         ignore a given string when comparing with the expected output");
                    System.out.println(" externallauncher=X     run tests by invoking a given native image of JSLauncher");
                    System.out.println(" compile                run with TruffleCompileImmediately");
                    System.out.println(" instrument             run with a dummy instrument that materializes all nodes");
                    System.out.println(" snapshot               execute tests through their snapshots");
                    System.out.println(" polyglot               run with polyglot access allowed");
                    System.out.println(" shareengine            use shared Engine for all tests");
                    System.out.println(" minesversion           minimal ECMAScript version used for test execution");
                    System.exit(-2);
                    break;
                case "nothreads":
                    builder.setUseThreads(false);
                    break;
                case "verbose":
                    builder.setVerbose(true);
                    break;
                case "verbosefail":
                    builder.setVerbose(false);
                    builder.setVerboseFail(true);
                    break;
                case "printcommand":
                    builder.setPrintCommand(true);
                    break;
                case "printscript":
                    builder.setPrintScript(true);
                    break;
                case "compile":
                    builder.setCompile(true);
                    break;
                case "instrument":
                    builder.setInstrument(true);
                    break;
                case "snapshot":
                    builder.setSnapshot(true);
                    break;
                case "polyglot":
                    builder.setPolyglot(true);
                    break;
                case "regression":
                    builder.setVerbose(false);
                    builder.setVerboseFail(false);
                    builder.setHtmlOutput(true);
                    builder.setTextOutput(true);
                    break;
                case "gate":
                    builder.setVerbose(false);
                    builder.setVerboseFail(false);
                    builder.setHtmlOutput(true);
                    builder.setTextOutput(true);
                    builder.setRunOnGate(true);
                    break;
                case "regenerateconfig":
                    builder.setRegenerateConfig(true);
                    builder.setRunOnGate(true); // forcing gate
                    break;
                case "resume":
                    builder.setGateResume(true);
                    builder.setRunOnGate(true); // forcing gate
                    break;
                case "timeouttest":
                    builder.setTimeoutTest(Integer.parseInt(value));
                    break;
                case "timeoutoverall":
                    builder.setTimeoutOverall(Integer.parseInt(value));
                    break;
                case "filter":
                    builder.setContainsFilter(value);
                    break;
                case "regex":
                    builder.setRegexFilter(value);
                    break;
                case "single":
                    builder.setEndsWithFilter(value);
                    builder.setPrintFullOutput(true);
                    builder.setVerboseFail(true);
                    break;
                case "location":
                    builder.setSuiteLoc(value);
                    break;
                case "config":
                    builder.setSuiteConfigLoc(value);
                    break;
                case "outputfilter":
                    builder.setOutputFilter(value);
                    break;
                case "saveoutput":
                    builder.setSaveOutput(true);
                    builder.setHtmlOutput(true);
                    break;
                case "externallauncher":
                    builder.setExtLauncher(value);
                    break;
                case "shareengine":
                    builder.setShareEngine(true);
                    break;
                case "minesversion":
                    int minESVersion;
                    if (JSContextOptions.ECMASCRIPT_VERSION_STAGING.equals(value)) {
                        minESVersion = JSConfig.StagingECMAScriptVersion;
                    } else {
                        minESVersion = Integer.parseInt(value);
                        if (minESVersion > JSConfig.ECMAScriptVersionYearDelta) {
                            minESVersion -= JSConfig.ECMAScriptVersionYearDelta;
                        }
                    }
                    builder.setMinESVersion(minESVersion);
                    break;
                default:
                    System.out.println("unrecognized argument: " + key + "\nCall \"" + builder.getSuiteName() + " help\" for more information.");
                    System.exit(-2);
            }
        }
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
