/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
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

    public synchronized void setContainsFilter(String containsFilter) {
        this.containsFilter = containsFilter;
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
