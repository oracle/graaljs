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
package com.oracle.truffle.js.runtime;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.oracle.truffle.api.TruffleOptions;

public final class JSTruffleOptions {

    private JSTruffleOptions() {
        // should not be constructed
    }

    public static final String TRUFFLE_JS_OPTION_PREFIX = "truffle.js.";
    public static final String JS_OPTION_PREFIX = "js.";
    private static final String PARSER_OPTION_PREFIX = "parser.";

    // exported option names
    public static final String ProfileTimeKey = "ProfileTime";

    private static Map<String, String> graaljsOptions = readGraaljsOptions();

    public static final boolean SubstrateVM = booleanOption("SubstrateVM", TruffleOptions.AOT);

    /** Parser options. */
    public static final boolean ReturnOptimizer = booleanOption("ReturnOptimizer", true);
    public static final boolean ReturnValueInFrame = booleanOption("ReturnValueInFrame", true);
    public static final boolean LocalVarIncDecNode = booleanOption("LocalVarIncDecNode", true);
    public static final boolean OptimizeApplyArguments = booleanOption("OptimizeApplyArguments", true);
    public static final boolean PrintAst = booleanOption("PrintAst", false);
    public static final boolean PrintParse = booleanOption("PrintParse", false);
    public static final boolean OptimizeNoFallthroughSwitch = booleanOption("OptimizeNoFallthroughSwitch", true);
    public static final boolean ManyBlockScopes = booleanOption("ManyBlockScopes", false);
    public static final boolean YieldResultInFrame = booleanOption("YieldResultInFrame", true);

    // Inline cache configuration
    public static int PropertyCacheLimit = integerOption("PropertyCacheLimit", 5);
    public static int FunctionCacheLimit = integerOption("FunctionCacheLimit", 4);

    public static final boolean AssertFinalPropertySpecialization = booleanOption("AssertFinalPropertySpecialization", false);
    /** Try to cache by function object instead of call target. */
    public static final boolean FunctionCacheOnInstance = booleanOption("FunctionCacheOnInstance", true);
    public static final boolean DictionaryObject = booleanOption("DictionaryObject", true);
    /** Migrate objects to dictionary mode when the number of properties exceeds this threshold. */
    public static final int DictionaryObjectThreshold = integerOption("DictionaryObjectThreshold", 256);
    public static final int DictionaryObjectTransitionThreshold = integerOption("DictionaryObjectThreshold", 1024);
    public static final boolean TraceDictionaryObject = booleanOption("TraceDictionaryObject", false);
    public static final boolean MergeShapes = booleanOption("MergeShapes", true);

    // Shape check elision
    public static final boolean SkipPrototypeShapeCheck = booleanOption("SkipPrototypeShapeCheck", true);
    public static final boolean SkipGlobalShapeCheck = booleanOption("SkipGlobalShapeCheck", true);
    public static final boolean SkipFinalShapeCheck = booleanOption("SkipFinalShapeCheck", true);
    public static final boolean LeafShapeAssumption = booleanOption("LeafShapeAssumption", true);

    // Runtime options
    public static final boolean LazyStrings = booleanOption("LazyStrings", true);
    public static final boolean RestrictForceSplittingBuiltins = booleanOption("RestrictForceSplittingBuiltins", true);
    public static final int MinLazyStringLength = integerOption("MinLazyStringLength", 20);
    public static final int ConcatToLeafLimit = integerOption("ConcatToLeafLimit", MinLazyStringLength / 2);
    public static final int MaxCompiledRegexCacheLength = integerOption("MaxCompiledRegexCacheLength", 4);
    public static final boolean TrimCompiledRegexCache = booleanOption("TrimCompiledRegexCache", true);
    public static final int StackTraceLimit = integerOption("StackTraceLimit", 10);
    public static final int StringLengthLimit = integerOption("StringLengthLimit", (1 << 30) - 1 - 24); // v8::String::kMaxLength
    public static final int MaxTypedArrayLength = integerOption("MaxTypedArrayLength", 0x3fff_ffff);
    public static final int MaxApplyArgumentLength = integerOption("MaxApplyArgumentLength", 10_000_000);
    public static final int MaxExpectedPrototypeChainLength = integerOption("MaxExpectedPrototypeChainLength", 32766); // regress-578775.js
    public static final boolean UseSuperOperations = booleanOption("UseSuperOperations", true);
    public static final boolean FastOwnKeys = booleanOption("FastOwnKeys", true);

    // should Graal.js Exceptions use the default Exception.fillInStackTrace? Turning it off might
    // hide Java frames (causing problems with interop, debugger), but increase performance around
    // fast-path exceptions.
    public static final boolean FillExceptionStack = booleanOption("FillExceptionStack", true);
    /** Always capture stack trace eagerly. */
    public static final boolean EagerStackTrace = booleanOption("EagerStackTrace", false);

    // Array options
    public static final int InitialArraySize = integerOption("array.InitialArraySize", 8);
    public static final int MaxArrayHoleSize = integerOption("array.MaxArrayHoleSize", 5000);
    public static final int MaxFlatArraySize = integerOption("array.MaxFlatArraySize", 1000000);
    public static final boolean TrackArrayAllocationSites = booleanOption("array.TrackAllocationSites", false);
    public static final int BigArrayThreshold = integerOption("array.BigArrayThreshold", 10000);
    public static final boolean MarkElementsNonNull = booleanOption("array.MarkElementsNonNull", true);
    /** Use DirectByteBuffer for typed arrays by default. */
    public static final boolean DirectByteBuffer = booleanOption("DirectByteBuffer", false);

    // ECMAScript specification options
    public static final int ECMAScript5 = 5;
    public static final int ECMAScript6 = 6;
    public static final int ECMAScript2015 = 6;
    public static final int ECMAScript2016 = 7;
    public static final int ECMAScript2017 = 8;
    public static final int ECMAScript2018 = 9;
    public static final int ECMAScript2019 = 10;
    public static final int ECMAScript2020 = 11;
    public static final int ECMAScriptNumberYearDelta = 2009; // ES6==ES2015
    public static final int LatestECMAScriptVersion = ECMAScript2020;
    public static final int MaxECMAScriptVersion = integerOption("ECMAScriptVersion", ECMAScript2020);
    /** Enable Annex B "Additional ECMAScript Features for Web Browsers". */
    public static final boolean AnnexB = booleanOption("AnnexB", true);

    // Nashorn extensions
    public static final boolean NashornCompatibilityMode = booleanOption("NashornCompatibilityMode", false);

    public static final boolean U180EWhitespace = booleanOption("U180EWhitespace", MaxECMAScriptVersion <= 6);

    // JSON options
    public static final boolean TruffleJSONParser = booleanOption("TruffleJSONParser", true);

    // Engine options
    public static final boolean DumpHeapOnExit = booleanOption("DumpHeapOnExit", false);
    public static final String HeapDumpFileName = stringOption("HeapDumpFileName", null);

    // Java Interop options
    public static final boolean SingleThreaded = booleanOption("SingleThreaded", false);

    // Tracing
    public static final boolean TracePolymorphicPropertyAccess = booleanOption("TracePolymorphicPropertyAccess", false);
    public static final boolean TraceMegamorphicPropertyAccess = booleanOption("TraceMegamorphicPropertyAccess", false);
    public static final boolean TraceFunctionCache = booleanOption("TraceFunctionCache", false);
    /** Traces transitions between dynamic array types. */
    public static final boolean TraceArrayTransitions = booleanOption("TraceArrayTransitions", false);
    /** Traces all array writes with their access mode. */
    public static final boolean TraceArrayWrites = booleanOption("TraceArrayWrites", false);

    // Profiling
    public static final boolean ProfileTime = booleanOption(ProfileTimeKey, false);
    public static final boolean PrintCumulativeTime = booleanOption("PrintCumulativeTime", false);

    // Debug options
    /** Expose {@code Debug} built-in object with a custom property name. */
    public static final String DebugPropertyName = stringOption("DebugPropertyName", JSRealm.DEBUG_CLASS_NAME);
    public static final boolean DebugCounters = booleanOption("DebugCounters", false);
    /** Load per-function data lazily. */
    public static final boolean LazyFunctionData = booleanOption("LazyFunctionData", true);
    /** Translate function bodies lazily. */
    public static final boolean LazyTranslation = booleanOption("LazyTranslation", false);
    /** AST-level inlining of trivial built-in functions (e.g. String.prototype.charAt). */
    public static final boolean InlineTrivialBuiltins = booleanOption("InlineTrivialBuiltins", true);
    /** [Construct] as part of the CallTarget names. Off by default (footprint). */
    public static final boolean DetailedCallTargetNames = booleanOption("DetailedCallTargetNames", false);
    public static final boolean TestCloneUninitialized = booleanOption("TestCloneUninitialized", false);
    /** When printing to console/inspector, only print that many properties. */
    public static final int MaxConsolePrintProperties = 20;

    // Spreading options
    public static final int SpreadArgumentPlaceholderCount = integerOption("SpreadArgumentPlaceholderCount", 3);

    // Truffle Interop options
    public static final boolean BindProgramResult = booleanOption("BindProgramResult", true);

    public static final boolean UseTRegex = booleanOption("UseTRegex", true);
    public static final boolean RegexRegressionTestMode = booleanOption("RegexRegressionTestMode", false);

    /** ECMA Promises are automatically resolved or rejected when crossing an interop boundary. */
    public static final boolean InteropCompletePromises = booleanOption("InteropCompletePromises", false);

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

    private static boolean booleanOption(String name, boolean defaultValue) {
        String value = readProperty(name);
        return value == null ? defaultValue : value.equalsIgnoreCase("true");
    }

    private static Integer integerOption(String name, int defaultValue) {
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

    private static String stringOption(String name, String defaultValue) {
        String value = readProperty(name);
        return value == null ? defaultValue : value;
    }
}
