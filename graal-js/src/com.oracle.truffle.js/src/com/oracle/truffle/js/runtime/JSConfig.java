/*
 * Copyright (c) 2020, 2024, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.TruffleOptions;

/**
 * This class stores magic numbers and other configuration values.
 *
 */
public final class JSConfig {

    /** See jdk.internal.util.ArraysSupport#SOFT_MAX_ARRAY_LENGTH. */
    public static final int SOFT_MAX_ARRAY_LENGTH = Integer.MAX_VALUE - 8;

    /** When printing to console/inspector, only print that many properties. */
    public static final int MaxConsolePrintProperties = 20;

    // Array options
    public static final int InitialArraySize = 8;
    public static final int MaxArrayHoleSize = 5000;
    public static final int MaxFlatArraySize = 1000000;
    public static final boolean TrackArrayAllocationSites = Boolean.FALSE;
    public static final int BigArrayThreshold = 10000;
    public static final boolean MarkElementsNonNull = true;

    // Debug options
    public static final boolean DebugCounters = Boolean.getBoolean("truffle.js.DebugCounters");

    // ECMAScript specification options
    public static final int ECMAScript5 = 5;
    public static final int ECMAScript6 = 6;
    public static final int ECMAScript2015 = 6;
    public static final int ECMAScript2016 = 7;
    public static final int ECMAScript2017 = 8;
    public static final int ECMAScript2018 = 9;
    public static final int ECMAScript2019 = 10;
    public static final int ECMAScript2020 = 11;
    public static final int ECMAScript2021 = 12;
    public static final int ECMAScript2022 = 13;
    public static final int ECMAScript2023 = 14;
    public static final int ECMAScript2024 = 15;
    public static final int ECMAScript2025 = 16;
    public static final int ECMAScriptVersionYearDelta = 2009; // ES6==ES2015
    public static final int LatestECMAScriptVersion = ECMAScript2024;
    public static final int StagingECMAScriptVersion = ECMAScript2025;
    /** Enable Annex B "Additional ECMAScript Features for Web Browsers". */
    public static final boolean AnnexB = true;

    // Inline Cache options
    /** Default cache limit for dispatched InteropLibrary. */
    public static final int InteropLibraryLimit = 5;
    public static final int PropertyCacheLimit = 5;
    public static final int FunctionCacheLimit = 4;
    public static final boolean AssertFinalPropertySpecialization = false;
    /** Try to cache by function object instead of call target. */
    public static final boolean FunctionCacheOnInstance = true;
    /** Maximum bound function nesting level to unpack in specialization. */
    public static final int BoundFunctionUnpackLimit = 10;
    public static final boolean DictionaryObject = true;
    /** Migrate objects to dictionary mode when the number of properties exceeds this threshold. */
    public static final int DictionaryObjectThreshold = 256;
    public static final int DictionaryObjectTransitionThreshold = 1024;
    public static final boolean MergeShapes = true;
    // GR-31859
    public static boolean MergeCompatibleLocations = true;

    // LazyString options
    public static final boolean LazyStrings = true;
    public static final int MinLazyStringLength = 20;

    // Parser options
    public static final boolean ReturnOptimizer = true;
    public static final boolean ReturnValueInFrame = true;
    public static final boolean LocalVarIncDecNode = true;
    public static final boolean OptimizeApplyArguments = true;
    public static boolean OptimizeNoFallthroughSwitch = false;
    public static final boolean ManyBlockScopes = false;
    public static final boolean YieldResultInFrame = true;
    public static final boolean LazyFunctionData = true;
    public static final boolean SplitModuleRoot = true;
    public static final boolean PrintAst = false;
    public static final boolean PrintParse = false;

    // Regex options
    public static final int MaxCompiledRegexCacheLength = 4;
    public static final boolean TrimCompiledRegexCache = true;

    // Runtime options
    public static final boolean RestrictForceSplittingBuiltins = true;
    public static final boolean UseSuperOperations = true;
    public static final boolean FastOwnKeys = true;
    /** AST-level inlining of trivial built-in functions (e.g. String.prototype.charAt). */
    public static final boolean InlineTrivialBuiltins = true;
    /** [Construct] as part of the CallTarget names. */
    public static final boolean DetailedCallTargetNames = Boolean.parseBoolean(System.getProperty("truffle.js.DetailedCallTargetNames", "true"));
    public static final int SpreadArgumentPlaceholderCount = 3;
    /** Always capture stack trace eagerly. */
    public static final boolean EagerStackTrace = false;
    static final int StackTraceLimit = 10;
    static final int StringLengthLimit = (1 << 30) - 1 - 24; // v8::String::kMaxLength
    /** Limited by Java array length. */
    static final int MaxTypedArrayLength = SOFT_MAX_ARRAY_LENGTH;
    static final int MaxApplyArgumentLength = 10_000_000;
    static final int MaxPrototypeChainLength = 32766; // regress-578775.js

    // Shape check elision options
    public static final boolean SkipPrototypeShapeCheck = true;
    public static final boolean SkipGlobalShapeCheck = true;
    public static final boolean SkipFinalShapeCheck = true;
    public static final boolean LeafShapeAssumption = true;
    public static final boolean PropertyAssumption = true;

    // SubstrateVM
    public static final boolean SubstrateVM = TruffleOptions.AOT;

    // Tracing
    public static final boolean TracePolymorphicPropertyAccess = Boolean.FALSE; // Unreachability
    public static final boolean TraceMegamorphicPropertyAccess = false;
    public static final boolean TraceFunctionCache = false;
    /** Traces transitions between dynamic array types. */
    public static final boolean TraceArrayTransitions = Boolean.FALSE; // Unreachability
    /** Traces all array writes with their access mode. */
    public static final boolean TraceArrayWrites = false;
    public static final boolean TraceDictionaryObject = false;

    // Symbol singletons: always return the same Symbol instance if Symbols with the same name are
    // globally unique.
    public static final boolean UseSingletonSymbols = true;

    private JSConfig() {
    }
}
