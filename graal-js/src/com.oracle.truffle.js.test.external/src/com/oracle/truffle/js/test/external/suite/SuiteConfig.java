/*
 * Copyright (c) 2018, 2022, Oracle and/or its affiliates. All rights reserved.
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
import java.util.Map;

import com.oracle.truffle.js.runtime.JSConfig;

public class SuiteConfig {

    // config: main
    private final String suiteName;
    private final String suiteDescription;

    private final String suiteLoc;
    private final String suiteTestsLoc;
    private final String suiteHarnessLoc;
    private final String suiteConfigLoc;

    // config: options
    private final boolean useThreads;
    private final boolean verbose;
    private final boolean verboseFail;
    private final boolean runOnGate;
    private final boolean gateResume;
    private final boolean printCommand;
    private final boolean printScript;
    private final boolean saveOutput;
    private final boolean compile;
    private final boolean instrument;
    private final boolean snapshot;
    private final boolean polyglot;
    private final boolean htmlOutput;
    private final boolean textOutput;
    private final boolean regenerateConfig;
    private final boolean shareEngine;
    private final int minESVersion;
    private final int timeoutTest; // individual timeouts not supported by all engines
    private final int timeoutOverall;
    private final String containsFilter;
    private final String regexFilter;
    private final String endsWithFilter;
    private final boolean printFullOutput;
    private final String outputFilter;

    private final String extLauncher;

    SuiteConfig(String suiteName, String suiteDescription,
                    String suiteLoc, String suiteTestsLoc, String suiteHarnessLoc, String suiteConfigLoc,
                    boolean useThreads, boolean verbose, boolean verboseFail, boolean runOnGate, boolean gateResume, boolean printCommand, boolean printScript, boolean saveOutput, boolean compile,
                    boolean instrument, boolean snapshot, boolean polyglot, boolean htmlOutput, boolean textOutput, boolean regenerateConfig, int timeoutTest, int timeoutOverall,
                    String containsFilter, String regexFilter, String endsWithFilter, boolean printFullOutput, String outputFilter, String extLauncher, boolean shareEngine, int minESVersion) {
        this.suiteName = suiteName;
        this.suiteDescription = suiteDescription;
        this.suiteLoc = suiteLoc;
        this.suiteTestsLoc = suiteTestsLoc;
        this.suiteHarnessLoc = suiteHarnessLoc;
        this.suiteConfigLoc = suiteConfigLoc;
        this.useThreads = useThreads;
        this.verbose = verbose;
        this.verboseFail = verboseFail;
        this.runOnGate = runOnGate;
        this.gateResume = gateResume;
        this.printCommand = printCommand;
        this.printScript = printScript;
        this.saveOutput = saveOutput;
        this.compile = compile;
        this.instrument = instrument;
        this.snapshot = snapshot;
        this.polyglot = polyglot;
        this.htmlOutput = htmlOutput;
        this.textOutput = textOutput;
        this.regenerateConfig = regenerateConfig;
        this.timeoutTest = timeoutTest;
        this.timeoutOverall = timeoutOverall;
        this.containsFilter = containsFilter;
        this.regexFilter = regexFilter;
        this.endsWithFilter = endsWithFilter;
        this.printFullOutput = printFullOutput;
        this.outputFilter = outputFilter;
        this.extLauncher = extLauncher;
        this.shareEngine = shareEngine;
        this.minESVersion = minESVersion;
    }

    public String getSuiteName() {
        return suiteName;
    }

    public String getSuiteDescription() {
        return suiteDescription;
    }

    public String getSuiteLoc() {
        return suiteLoc;
    }

    public String getSuiteConfigLoc() {
        return suiteConfigLoc;
    }

    public String getSuiteTestsLoc() {
        return suiteTestsLoc;
    }

    public String getSuiteHarnessLoc() {
        return suiteHarnessLoc;
    }

    public boolean isUseThreads() {
        return useThreads;
    }

    public boolean isVerbose() {
        return verbose;
    }

    public boolean isVerboseFail() {
        return verboseFail;
    }

    public boolean isRunOnGate() {
        return runOnGate;
    }

    public boolean isGateResume() {
        return gateResume;
    }

    public boolean isPrintCommand() {
        return printCommand;
    }

    public boolean isPrintScript() {
        return printScript;
    }

    public boolean isSaveOutput() {
        return saveOutput;
    }

    public boolean isCompile() {
        return compile;
    }

    public boolean isInstrument() {
        return instrument;
    }

    public boolean useSnapshots() {
        return snapshot;
    }

    public boolean isPolyglot() {
        return polyglot;
    }

    public boolean isHtmlOutput() {
        return htmlOutput;
    }

    public boolean isTextOutput() {
        return textOutput;
    }

    public boolean isRegenerateConfig() {
        return regenerateConfig;
    }

    public boolean isShareEngine() {
        return shareEngine;
    }

    public int getMinESVersion() {
        return minESVersion;
    }

    /**
     * Define the timeout of a single test. Used only in combination with
     * executeWithSeparateThreads() == true;
     */
    public int getTimeoutTest() {
        return timeoutTest;
    }

    public int getTimeoutOverall() {
        return timeoutOverall;
    }

    public String getContainsFilter() {
        return containsFilter;
    }

    public String getRegexFilter() {
        return regexFilter;
    }

    public String getEndsWithFilter() {
        return endsWithFilter;
    }

    public boolean isPrintFullOutput() {
        return printFullOutput;
    }

    public String getOutputFilter() {
        return outputFilter;
    }

    public boolean isExtLauncher() {
        return extLauncher != null;
    }

    public String getExtLauncher() {
        return extLauncher;
    }

    public void addCommonOptions(Map<String, String> options) {
        if (isCompile()) {
            options.put("engine.CompileImmediately", "true");
            options.put("engine.BackgroundCompilation", "false");
        }
        if (isInstrument()) {
            options.put("TestInstrument", "true");
        }
    }

    public static class Builder {
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
        private boolean printCommand;
        private boolean printScript;
        private boolean saveOutput;
        private boolean compile;
        private boolean instrument;
        private boolean snapshot;
        private boolean polyglot;
        private boolean htmlOutput;
        private boolean textOutput;
        private boolean regenerateConfig;
        private boolean shareEngine;
        private int timeoutTest; // individual timeouts not supported by all engines
        private int timeoutOverall;
        private String containsFilter;
        private String regexFilter;
        private String endsWithFilter;
        private boolean printFullOutput;
        private String outputFilter;
        private int minESVersion = JSConfig.LatestECMAScriptVersion;

        private String extLauncher;

        public Builder(String suiteName, String suiteDescription, String defaultSuiteLoc, String defaultSuiteConfigLoc, String suiteTestsRelLoc, String suiteHarnessRelLoc) {
            this.suiteName = suiteName;
            this.suiteDescription = suiteDescription;
            this.suiteTestsRelLoc = suiteTestsRelLoc;
            this.suiteHarnessRelLoc = suiteHarnessRelLoc;
            this.suiteConfigLoc = defaultSuiteConfigLoc;
            this.timeoutTest = TestSuite.INDIVIDUAL_TIMEOUT_SECONDS;
            this.timeoutOverall = TestSuite.OVERALL_TIMEOUT_SECONDS;
            this.useThreads = true;
            setSuiteLoc(defaultSuiteLoc);
        }

        public String getSuiteName() {
            return suiteName;
        }

        public Object getEndsWithFilter() {
            return endsWithFilter;
        }

        public void setSuiteLoc(String suiteLoc) {
            this.suiteLoc = suiteLoc;
            this.suiteTestsLoc = Paths.get(this.suiteLoc, this.suiteTestsRelLoc).toString();
            this.suiteHarnessLoc = Paths.get(this.suiteLoc, this.suiteHarnessRelLoc).toString();
        }

        public void setSuiteConfigLoc(String suiteConfigLoc) {
            this.suiteConfigLoc = suiteConfigLoc;
        }

        public void setUseThreads(boolean useThreads) {
            this.useThreads = useThreads;
        }

        public void setVerbose(boolean verbose) {
            this.verbose = verbose;
        }

        public void setVerboseFail(boolean verboseFail) {
            this.verboseFail = verboseFail;
        }

        public void setRunOnGate(boolean runOnGate) {
            this.runOnGate = runOnGate;
        }

        public void setGateResume(boolean gateResume) {
            this.gateResume = gateResume;
        }

        public void setPrintCommand(boolean printCommand) {
            this.printCommand = printCommand;
        }

        public void setPrintScript(boolean printScript) {
            this.printScript = printScript;
        }

        public void setSaveOutput(boolean saveOutput) {
            this.saveOutput = saveOutput;
        }

        public void setCompile(boolean compile) {
            this.compile = compile;
        }

        public void setInstrument(boolean instrument) {
            this.instrument = instrument;
        }

        public void setSnapshot(boolean snapshot) {
            this.snapshot = snapshot;
        }

        public void setPolyglot(boolean polyglot) {
            this.polyglot = polyglot;
        }

        public void setHtmlOutput(boolean htmlOutput) {
            this.htmlOutput = htmlOutput;
        }

        public void setTextOutput(boolean textOutput) {
            this.textOutput = textOutput;
        }

        public void setRegenerateConfig(boolean regenerateConfig) {
            this.regenerateConfig = regenerateConfig;
        }

        public void setTimeoutTest(int timeout) {
            this.timeoutTest = timeout;
        }

        public void setTimeoutOverall(int timeoutOverall) {
            this.timeoutOverall = timeoutOverall;
        }

        public void setContainsFilter(String containsFilter) {
            this.containsFilter = containsFilter;
        }

        public void setRegexFilter(String regexFilter) {
            this.regexFilter = regexFilter;
        }

        public void setEndsWithFilter(String endsWithFilter) {
            this.endsWithFilter = endsWithFilter;
        }

        public void setPrintFullOutput(boolean fullOutput) {
            this.printFullOutput = fullOutput;
        }

        public void setOutputFilter(String outputFilter) {
            this.outputFilter = outputFilter;
        }

        public void setExtLauncher(String extLauncher) {
            this.extLauncher = extLauncher;
        }

        public void setShareEngine(boolean shareEngine) {
            this.shareEngine = shareEngine;
        }

        public void setMinESVersion(int minESVersion) {
            this.minESVersion = minESVersion;
        }

        public SuiteConfig build() {
            return new SuiteConfig(suiteName, suiteDescription, suiteLoc, suiteTestsLoc, suiteHarnessLoc, suiteConfigLoc, useThreads, verbose, verboseFail, runOnGate, gateResume, printCommand,
                            printScript, saveOutput, compile, instrument, snapshot, polyglot, htmlOutput, textOutput, regenerateConfig, timeoutTest, timeoutOverall, containsFilter, regexFilter,
                            endsWithFilter, printFullOutput, outputFilter, extLauncher, shareEngine, minESVersion);
        }
    }
}
