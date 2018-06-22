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
package com.oracle.truffle.js.runtime;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.oracle.truffle.api.TruffleOptions;
import com.oracle.truffle.js.runtime.builtins.JSDebug;

public class JSTruffleOptions {
    public static final String TRUFFLE_JS_OPTION_PREFIX = "truffle.js.";
    public static final String JS_OPTION_PREFIX = "js.";
    private static final String PARSER_OPTION_PREFIX = "parser.";

    // option is private, will not be printed in "help"
    public static final int OPTION_PRIVATE = 1 << 0;
    // option that will (likely) influence performance.
    public static final int OPTION_PERFORMANCE = 1 << 1;
    // option that enables/disables a user-observable feature of the language
    public static final int OPTION_LANGUAGE_FEATURE = 1 << 2;
    // option represents an (arbitrary) limit of some kind
    public static final int OPTION_LIMIT = 1 << 3;
    // option relevant for engine setup, tooling, etc.
    public static final int OPTION_SETUP = 1 << 4;

    // exported option names
    public static final String ProfileTimeKey = "ProfileTime";

    private static Map<String, String> graaljsOptions = readGraaljsOptions();
    private static List<Option> availableOptions = new ArrayList<>();

    public static final boolean SubstrateVM = booleanOption("SubstrateVM", TruffleOptions.AOT, OPTION_PRIVATE | OPTION_SETUP, "Current execution happening on SubstrateVM");

    /** Parser options. */
    public static final boolean ReturnOptimizer = booleanOption("ReturnOptimizer", true, OPTION_PERFORMANCE);
    public static final boolean ReturnValueInFrame = booleanOption("ReturnValueInFrame", true, OPTION_PERFORMANCE);
    public static final boolean LocalVarIncDecNode = booleanOption("LocalVarIncDecNode", true, OPTION_PERFORMANCE);
    public static final boolean OptimizeApplyArguments = booleanOption("OptimizeApplyArguments", true, OPTION_PERFORMANCE);
    public static final boolean PrintAst = booleanOption("PrintAst", false, OPTION_SETUP);
    public static final boolean PrintParse = booleanOption("PrintParse", false, OPTION_SETUP);
    public static final boolean OptimizeNoFallthroughSwitch = booleanOption("OptimizeNoFallthroughSwitch", true, OPTION_PERFORMANCE);
    public static final boolean ManyBlockScopes = booleanOption("ManyBlockScopes", false, OPTION_PERFORMANCE);
    public static final boolean YieldResultInFrame = booleanOption("YieldResultInFrame", true, OPTION_PERFORMANCE);

    // Inline cache configuration
    public static int PropertyCacheLimit = integerOption("PropertyCacheLimit", 5, OPTION_PERFORMANCE | OPTION_LIMIT);
    public static int FunctionCacheLimit = integerOption("FunctionCacheLimit", 4, OPTION_PERFORMANCE | OPTION_LIMIT);

    public static final boolean AssertFinalPropertySpecialization = booleanOption("AssertFinalPropertySpecialization", false, 0);
    /** Try to cache by function object instead of call target. */
    public static final boolean FunctionCacheOnInstance = booleanOption("FunctionCacheOnInstance", true, 0);
    public static final boolean DictionaryObject = booleanOption("DictionaryObject", true, OPTION_PERFORMANCE);
    public static final boolean TraceDictionaryObject = booleanOption("TraceDictionaryObject", false, 0);
    public static final boolean MergeShapes = booleanOption("MergeShapes", true, 0);

    // Shape check elision
    public static final boolean SkipPrototypeShapeCheck = booleanOption("SkipPrototypeShapeCheck", true, 0);
    public static final boolean SkipGlobalShapeCheck = booleanOption("SkipGlobalShapeCheck", true, 0);
    public static final boolean SkipFinalShapeCheck = booleanOption("SkipFinalShapeCheck", true, 0);

    // Runtime options
    public static final boolean LazyStrings = booleanOption("LazyStrings", true, OPTION_PERFORMANCE);
    public static final int MinLazyStringLength = integerOption("MinLazyStringLength", 20, OPTION_LIMIT);
    public static final int MaxLoadCacheLength = integerOption("MaxLoadCacheLength", 0, OPTION_LIMIT);
    public static final int MaxCompiledRegexCacheLength = integerOption("MaxCompiledRegexCacheLength", 4, OPTION_LIMIT);
    public static final boolean TrimLoadCache = booleanOption("TrimLoadCache", false, 0);
    public static final boolean TrimCompiledRegexCache = booleanOption("TrimCompiledRegexCache", true, 0);
    public static final int StackTraceLimit = integerOption("StackTraceLimit", 10, OPTION_LIMIT);
    public static final int StringLengthLimit = integerOption("StringLengthLimit", (1 << 28) - 16, OPTION_LIMIT);
    public static final int MaxTypedArrayLength = integerOption("MaxTypedArrayLength", 0x3fff_ffff, OPTION_LIMIT);
    public static final int MaxApplyArgumentLength = integerOption("MaxApplyArgumentLength", 10_000_000, OPTION_LIMIT);
    // regress-1122.js, regress-605470.js
    public static final int MaxFunctionArgumentsLength = integerOption("MaxFunctionArgumentsLength", 65535, OPTION_LIMIT);
    public static final int MaxExpectedPrototypeChainLength = integerOption("MaxExpectedPrototypeChainLength", 32766, OPTION_LIMIT); // regress-578775.js
    public static final boolean UseSuperOperations = booleanOption("UseSuperOperations", true, OPTION_PERFORMANCE);
    public static final boolean FastOwnKeys = booleanOption("FastOwnKeys", true, OPTION_PERFORMANCE);

    // should Graal.js Exceptions use the default Exception.fillInStackTrace? Turning it off might
    // hide Java frames (causing problems with interop, debugger), but increase performance around
    // fast-path exceptions.
    public static final boolean FillExceptionStack = booleanOption("FillExceptionStack", true, OPTION_PERFORMANCE);
    /** Always capture stack trace eagerly. */
    public static final boolean EagerStackTrace = booleanOption("EagerStackTrace", false, OPTION_PERFORMANCE);

    // Array options
    public static final int InitialArraySize = integerOption("array.InitialArraySize", 8, OPTION_LIMIT);
    public static final int MaxArrayHoleSize = integerOption("array.MaxArrayHoleSize", 5000, OPTION_LIMIT);
    public static final int MaxFlatArraySize = integerOption("array.MaxFlatArraySize", 1000000, OPTION_LIMIT);
    public static final boolean TrackArrayAllocationSites = booleanOption("array.TrackAllocationSites", false, OPTION_PERFORMANCE);
    public static final int BigArrayThreshold = integerOption("array.BigArrayThreshold", 10000, OPTION_LIMIT);
    public static final boolean MarkElementsNonNull = booleanOption("array.MarkElementsNonNull", true, OPTION_PERFORMANCE);
    /** Use DirectByteBuffer for typed arrays by default. */
    public static final boolean DirectByteBuffer = booleanOption("DirectByteBuffer", false, OPTION_PERFORMANCE);

    // ECMAScript specification options
    public static final int ECMAScript2017 = 8;
    public static final int ECMAScript2018 = 9;
    private static final int LatestECMAScriptVersion = ECMAScript2018;
    public static final int MaxECMAScriptVersion = integerOption("ECMAScriptVersion", LatestECMAScriptVersion, OPTION_LANGUAGE_FEATURE);
    /** Enable Annex B "Additional ECMAScript Features for Web Browsers". */
    public static final boolean AnnexB = booleanOption("AnnexB", true, OPTION_LANGUAGE_FEATURE);

    /**
     * Enable ES.next features according to the
     * <a href="https://tc39.github.io/process-document">TC39 process</a> maturity stages. Stage 4
     * features are going to be included in the next ECMAScript standard.
     */
    public static final boolean Stage4 = booleanOption("Stage4", MaxECMAScriptVersion >= LatestECMAScriptVersion, OPTION_LANGUAGE_FEATURE); // Finished
    public static final boolean Stage3 = booleanOption("Stage3", Stage4, OPTION_LANGUAGE_FEATURE); // Candidate
    public static final boolean Stage2 = booleanOption("Stage2", Stage3, OPTION_LANGUAGE_FEATURE); // Draft
    public static final boolean Stage1 = booleanOption("Stage1", Stage2, OPTION_LANGUAGE_FEATURE); // Proposal
    public static final boolean Stage0 = booleanOption("Stage0", Stage1, OPTION_LANGUAGE_FEATURE); // Strawman

    /** Enable non-standard extensions. */
    public static final boolean Extensions = booleanOption("Extensions", true, OPTION_LANGUAGE_FEATURE);
    /** Timestamp resolution of performance.now() in nanoseconds. */
    public static final long TimestampResolution = integerOption("TimestampResolution", 1_000_000, OPTION_LIMIT);
    /** Java implementation of SIMD.js. */
    public static final boolean SIMDJS = booleanOption("SIMDJS", false, OPTION_LANGUAGE_FEATURE);

    // Nashorn extensions
    public static final boolean NashornCompatibilityMode = booleanOption("NashornCompatibilityMode", false, OPTION_LANGUAGE_FEATURE);
    public static final boolean NashornExtensions = booleanOption("NashornExtensions", true, OPTION_LANGUAGE_FEATURE);

    public static final boolean V8LegacyConst = booleanOption("V8LegacyConst", false, OPTION_LANGUAGE_FEATURE);

    public static final boolean U180EWhitespace = booleanOption("U180EWhitespace", MaxECMAScriptVersion <= 6, OPTION_LANGUAGE_FEATURE);

    // Test engine options. Used to expose internal behavior only in these test setups.
    public static final boolean Test262Mode = booleanOption("Test262Mode", false, OPTION_SETUP);
    public static final boolean TestV8Mode = booleanOption("TestV8Mode", false, OPTION_SETUP);

    public static final boolean ValidateRegExpLiterals = booleanOption("ValidateRegExpLiterals", !TestV8Mode, OPTION_LANGUAGE_FEATURE);

    // JSON options
    public static final boolean TruffleJSONParser = booleanOption("TruffleJSONParser", true, OPTION_LANGUAGE_FEATURE);

    // Engine options
    public static final boolean DumpHeapOnExit = booleanOption("DumpHeapOnExit", false, OPTION_SETUP);
    public static final String HeapDumpFileName = stringOption("HeapDumpFileName", null, OPTION_SETUP);

    // Java Interop options
    public static final boolean NashornJavaInterop = !SubstrateVM && booleanOption("NashornJavaInterop", false, OPTION_LANGUAGE_FEATURE);
    public static final boolean JavaCallCache = booleanOption("JavaCallCache", true, OPTION_PERFORMANCE);
    public static final boolean SingleThreaded = booleanOption("SingleThreaded", false, OPTION_SETUP);
    public static final boolean JavaConvertersAsMethodHandles = booleanOption("JavaConvertersAsMethodHandles", false, OPTION_PERFORMANCE);

    // Tracing
    public static final boolean TracePolymorphicPropertyAccess = booleanOption("TracePolymorphicPropertyAccess", false, OPTION_SETUP);
    public static final boolean TraceMegamorphicPropertyAccess = booleanOption("TraceMegamorphicPropertyAccess", false, OPTION_SETUP);
    public static final boolean TraceFunctionCache = booleanOption("TraceFunctionCache", false, OPTION_SETUP);
    /** Traces transitions between dynamic array types. */
    public static final boolean TraceArrayTransitions = booleanOption("TraceArrayTransitions", false, OPTION_SETUP);
    /** Traces all array writes with their access mode. */
    public static final boolean TraceArrayWrites = booleanOption("TraceArrayWrites", false, OPTION_SETUP);

    // Profiling
    public static final boolean ProfileTime = booleanOption(ProfileTimeKey, false, OPTION_SETUP);
    public static final boolean PrintCumulativeTime = booleanOption("PrintCumulativeTime", false, OPTION_SETUP);

    // Debug options
    /** Expose {@code Debug} built-in object with a custom property name. */
    public static final String DebugPropertyName = stringOption("DebugPropertyName", JSDebug.CLASS_NAME, OPTION_LANGUAGE_FEATURE);
    public static final boolean DebugCounters = booleanOption("DebugCounters", false, OPTION_SETUP);
    /** Load per-function data lazily. */
    public static final boolean LazyFunctionData = booleanOption("LazyFunctionData", true, OPTION_SETUP | OPTION_PERFORMANCE);
    /** Translate function bodies lazily. */
    public static final boolean LazyTranslation = booleanOption("LazyTranslation", false, OPTION_SETUP | OPTION_PERFORMANCE);
    /** AST-level inlining of trivial built-in functions (e.g. String.prototype.charAt). */
    public static final boolean InlineTrivialBuiltins = booleanOption("InlineTrivialBuiltins", true, OPTION_SETUP | OPTION_PERFORMANCE);
    /** [Construct] as part of the CallTarget names. Off by default (footprint). */
    public static final boolean DetailedCallTargetNames = booleanOption("DetailedCallTargetNames", false, OPTION_SETUP);
    public static final boolean TestCloneUninitialized = booleanOption("TestCloneUninitialized", false, OPTION_SETUP);
    /** When printing to console/inspector, only print that many properties. */
    public static final int MaxConsolePrintProperties = 20;

    // Truffle Interop options
    public static final boolean TruffleInterop = booleanOption("TruffleInterop", true, OPTION_LANGUAGE_FEATURE);
    public static final boolean BindProgramResult = booleanOption("BindProgramResult", true, OPTION_SETUP);

    public static final boolean UseTRegex = booleanOption("UseTRegex", true, OPTION_SETUP | OPTION_PERFORMANCE);
    public static final boolean RegexRegressionTestMode = booleanOption("RegexRegressionTestMode", false, OPTION_SETUP);

    public static final boolean GraalBuiltin = booleanOption("GraalBuiltin", true, OPTION_SETUP);

    /** ECMA Promises are automatically resolved or rejected when crossing an interop boundary. */
    public static final boolean InteropCompletePromises = booleanOption("InteropCompletePromises", false, OPTION_SETUP);

    static {
        checkUnknownOptions();
    }

    // -------------------------------------------------------------------------------------------------------------------//

    private static Object checkUnknownOptions() {
        boolean unknownOptions = false;
        if (graaljsOptions.size() > 0) {
            for (String key : graaljsOptions.keySet()) {
                if (key.startsWith(PARSER_OPTION_PREFIX)) {
                    continue;
                }
                System.out.println("unknown option: " + TRUFFLE_JS_OPTION_PREFIX + key);
                unknownOptions = true;
            }
        }
        if (unknownOptions) {
            throw new RuntimeException("exit due to unknown options");
        }
        graaljsOptions = null; // table not necessary any more
        return null;
    }

    private static Map<String, String> readGraaljsOptions() {
        Map<String, String> options = new HashMap<>();
        for (Entry<Object, Object> entry : System.getProperties().entrySet()) {
            String strKey = entry.getKey().toString();
            if (strKey.startsWith(TRUFFLE_JS_OPTION_PREFIX)) {
                options.put(strKey.substring(TRUFFLE_JS_OPTION_PREFIX.length()), entry.getValue().toString());
            }
        }
        return options;
    }

    private static String readProperty(String name) {
        return graaljsOptions.remove(name);
    }

    public static String getOptionName(String key) {
        return TRUFFLE_JS_OPTION_PREFIX + key;
    }

    private static boolean booleanOption(String name, boolean defaultValue, int flags) {
        return booleanOption(name, defaultValue, flags, null);
    }

    private static boolean booleanOption(String name, boolean defaultValue, int flags, String description) {
        logOption(name, "Boolean", defaultValue, flags, description);
        String value = readProperty(name);
        return value == null ? defaultValue : value.equalsIgnoreCase("true");
    }

    private static Integer integerOption(String name, int defaultValue, int flags) {
        return integerOption(name, defaultValue, flags, null);
    }

    private static Integer integerOption(String name, int defaultValue, int flags, String description) {
        logOption(name, "Integer", defaultValue, flags, description);
        try {
            String prop = readProperty(name);
            if (prop == null) {
                return defaultValue;
            }
            return Integer.decode(prop);
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    private static String stringOption(String name, String defaultValue, int flags) {
        return stringOption(name, defaultValue, flags, null);
    }

    private static String stringOption(String name, String defaultValue, int flags, String description) {
        logOption(name, "String", defaultValue, flags, description);
        String value = readProperty(name);
        return value == null ? defaultValue : value;
    }

    private static void logOption(String name, String type, Object defaultValue, int flags, String description) {
        availableOptions.add(new Option(name, type, description, defaultValue, flags));
    }

    public static String getAllOptionDescriptions() {
        StringBuilder allOptions = new StringBuilder();
        for (Option opt : availableOptions) {
            if ((opt.getFlags() & OPTION_PRIVATE) == 0) {
                String flagsString = addFlag(opt, OPTION_PERFORMANCE, "PERFORMANCE,") + addFlag(opt, OPTION_LANGUAGE_FEATURE, "FEATURE,") + addFlag(opt, OPTION_LIMIT, "LIMIT,") +
                                addFlag(opt, OPTION_SETUP, "SETUP,");
                allOptions.append(String.format("%-40s %-10s (default: %s) %s\n", opt.getName(), opt.getType(), opt.getDefaultValue(), flagsString));
                if (opt.getDescription() != null) {
                    String shortDesc = opt.getDescription().length() < 50 ? opt.getDescription() : (opt.getDescription().substring(0, 47) + "...");
                    allOptions.append(String.format("     %s\n", shortDesc));
                }
            }
        }
        return allOptions.toString();
    }

    private static String addFlag(Option opt, int optionPerformance, String flagString) {
        return (opt.getFlags() & optionPerformance) != 0 ? flagString : "";
    }

    private static class Option {
        private final String name;
        private final String type;
        private final String description;
        private final Object defaultValue;
        private final int flags;

        Option(String name, String type, String description, Object defaultValue, int flags) {
            this.name = name;
            this.type = type;
            this.description = description;
            this.defaultValue = defaultValue;
            this.flags = flags;
        }

        public String getName() {
            return name;
        }

        public String getType() {
            return type;
        }

        public String getDescription() {
            return description;
        }

        public Object getDefaultValue() {
            return defaultValue;
        }

        public int getFlags() {
            return flags;
        }
    }
}
