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

import java.nio.file.Paths;

public class SuiteConfig {
    // config: main
    private final String suiteName;
    private final String suiteDescription;

    // config: files and paths
    private final String suiteTestsRelLoc;
    private final String suiteHarnessRelLoc;
    private String suiteLoc;
    private String suiteTestsLoc;
    private String suiteHarnessLoc;
    private String suiteConfigLoc;

    // config: options
    private boolean useThreads;
    private boolean verbose;
    private boolean verboseFail;
    private boolean runOnGate;
    private boolean gateResume;
    private boolean printScript;
    private boolean saveOutput;
    private boolean compile;
    private boolean htmlOutput;
    private boolean textOutput;
    private boolean regenerateConfig;
    private int compileCount;
    private int timeoutTest; // individual timeouts not supported by all engines
    private int timeoutOverall;
    private String containsFilter;
    private String regexFilter;
    private String endsWithFilter;
    private boolean printFullOutput;
    private String outputFilter;

    public SuiteConfig(String suiteName, String suiteDescription, String defaultSuiteLoc, String defaultSuiteConfigLoc, String suiteTestsRelLoc, String suiteHarnessRelLoc) {
        this.suiteName = suiteName;
        this.suiteDescription = suiteDescription;
        this.suiteTestsRelLoc = suiteTestsRelLoc;
        this.suiteHarnessRelLoc = suiteHarnessRelLoc;
        this.suiteConfigLoc = defaultSuiteConfigLoc;
        this.compileCount = 100;
        this.timeoutTest = TestSuite.INDIVIDUAL_TIMEOUT_SECONDS;
        this.timeoutOverall = TestSuite.OVERALL_TIMEOUT_SECONDS;
        this.useThreads = true;
        setSuiteLoc(defaultSuiteLoc);
    }

    public synchronized String getSuiteName() {
        return suiteName;
    }

    public synchronized String getSuiteDescription() {
        return suiteDescription;
    }

    public synchronized String getSuiteLoc() {
        return suiteLoc;
    }

    public synchronized String getSuiteConfigLoc() {
        return suiteConfigLoc;
    }

    public synchronized String getSuiteTestsLoc() {
        return suiteTestsLoc;
    }

    public synchronized String getSuiteHarnessLoc() {
        return suiteHarnessLoc;
    }

    public synchronized boolean isUseThreads() {
        return useThreads;
    }

    public synchronized boolean isVerbose() {
        return verbose;
    }

    public synchronized boolean isVerboseFail() {
        return verboseFail;
    }

    public synchronized boolean isRunOnGate() {
        return runOnGate;
    }

    public synchronized boolean isGateResume() {
        return gateResume;
    }

    public synchronized boolean isPrintScript() {
        return printScript;
    }

    public synchronized boolean isSaveOutput() {
        return saveOutput;
    }

    public synchronized boolean isCompile() {
        return compile;
    }

    public synchronized boolean isHtmlOutput() {
        return htmlOutput;
    }

    public synchronized boolean isTextOutput() {
        return textOutput;
    }

    public synchronized boolean isRegenerateConfig() {
        return regenerateConfig;
    }

    public synchronized int getCompileCount() {
        return compileCount;
    }

    public synchronized void setSuiteLoc(String suiteLoc) {
        this.suiteLoc = suiteLoc;
        this.suiteTestsLoc = Paths.get(this.suiteLoc, this.suiteTestsRelLoc).toString();
        this.suiteHarnessLoc = Paths.get(this.suiteLoc, this.suiteHarnessRelLoc).toString();
    }

    public synchronized void setSuiteConfigLoc(String suiteConfigLoc) {
        this.suiteConfigLoc = suiteConfigLoc;
    }

    public synchronized void setUseThreads(boolean useThreads) {
        this.useThreads = useThreads;
    }

    public synchronized void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    public synchronized void setVerboseFail(boolean verboseFail) {
        this.verboseFail = verboseFail;
    }

    public synchronized void setRunOnGate(boolean runOnGate) {
        this.runOnGate = runOnGate;
    }

    public synchronized void setGateResume(boolean gateResume) {
        this.gateResume = gateResume;
    }

    public synchronized void setPrintScript(boolean printScript) {
        this.printScript = printScript;
    }

    public synchronized void setSaveOutput(boolean saveOutput) {
        this.saveOutput = saveOutput;
    }

    public synchronized void setCompile(boolean compile) {
        this.compile = compile;
    }

    public synchronized void setHtmlOutput(boolean htmlOutput) {
        this.htmlOutput = htmlOutput;
    }

    public synchronized void setTextOutput(boolean textOutput) {
        this.textOutput = textOutput;
    }

    public synchronized void setRegenerateConfig(boolean regenerateConfig) {
        this.regenerateConfig = regenerateConfig;
    }

    public synchronized void setCompileCount(int compileCount) {
        this.compileCount = compileCount;
    }

    /**
     * Define the timeout of a single test. Used only in combination with
     * executeWithSeparateThreads() == true;
     */
    public synchronized int getTimeoutTest() {
        return timeoutTest;
    }

    public synchronized void setTimeoutTest(int timeout) {
        this.timeoutTest = timeout;
    }

    public synchronized int getTimeoutOverall() {
        return timeoutOverall;
    }

    public synchronized void setTimeoutOverall(int timeoutOverall) {
        this.timeoutOverall = timeoutOverall;
    }

    public synchronized String getContainsFilter() {
        return containsFilter;
    }

    public synchronized String getRegexFilter() {
        return regexFilter;
    }

    public synchronized void setContainsFilter(String containsFilter) {
        this.containsFilter = containsFilter;
    }

    public synchronized void setRegexFilter(String regexFilter) {
        this.regexFilter = regexFilter;
    }

    public synchronized String getEndsWithFilter() {
        return endsWithFilter;
    }

    public synchronized void setEndsWithFilter(String endsWithFilter) {
        this.endsWithFilter = endsWithFilter;
    }

    public synchronized boolean isPrintFullOutput() {
        return printFullOutput;
    }

    public synchronized void setPrintFullOutput(boolean fullOutput) {
        this.printFullOutput = fullOutput;
    }

    public synchronized String getOutputFilter() {
        return outputFilter;
    }

    public synchronized void setOutputFilter(String outputFilter) {
        this.outputFilter = outputFilter;
    }
}
