/*
 * Copyright (c) 2018, 2024, Oracle and/or its affiliates. All rights reserved.
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

import org.graalvm.options.OptionValues;
import org.graalvm.polyglot.SandboxPolicy;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.js.runtime.JSContextOptions.UnhandledRejectionsTrackingMode;

/**
 * Cached option values of per-language (Engine) options to allow for fast access in the interpreter
 * and constant folding in compiled code. Generally speaking, the language instance (including
 * parsed and compiled code, object shapes, etc.) cannot be shared if any of these options differ.
 *
 * @see JSContextOptions
 * @see JSParserOptions
 */
public record JSLanguageOptions(
                int ecmaScriptVersion,
                boolean strict,
                boolean annexB,
                boolean intl402,
                boolean regexpMatchIndices,
                boolean regexpUnicodeSets,
                boolean sharedArrayBuffer,
                boolean v8RealmBuiltin,
                boolean nashornCompatibilityMode,
                int stackTraceLimit,
                boolean parseOnly,
                boolean zoneRulesBasedTimeZones,
                boolean agentCanBlock,
                boolean printNoNewline,
                boolean commonJSRequire,
                boolean awaitOptimization,
                boolean allowEval,
                boolean disableWith,
                boolean regexDumpAutomata,
                boolean regexStepExecution,
                boolean regexAlwaysEager,
                boolean scriptEngineGlobalScopeImport,
                boolean hasForeignObjectPrototype,
                boolean hasForeignHashProperties,
                long functionArgumentsLimit,
                boolean test262Mode,
                boolean testV8Mode,
                boolean validateRegExpLiterals,
                int functionConstructorCacheSize,
                int regexCacheSize,
                int stringLengthLimit,
                boolean stringLazySubstrings,
                boolean bindMemberFunctions,
                boolean regexRegressionTestMode,
                boolean testCloneUninitialized,
                boolean lazyTranslation,
                int maxTypedArrayLength,
                int maxApplyArgumentLength,
                int maxPrototypeChainLength,
                boolean asyncStackTraces,
                int propertyCacheLimit,
                int functionCacheLimit,
                short frequencyBasedPropertyCacheLimit,
                boolean topLevelAwait,
                boolean useUTCForLegacyDates,
                boolean webAssembly,
                boolean newSetMethods,
                boolean temporal,
                boolean iteratorHelpers,
                boolean asyncIteratorHelpers,
                boolean shadowRealm,
                boolean asyncContext,
                boolean v8Intrinsics,
                UnhandledRejectionsTrackingMode unhandledRejectionsMode,
                boolean operatorOverloading,
                boolean errorCause,
                boolean importAttributes,
                boolean importAssertions,
                boolean jsonModules,
                boolean sourcePhaseImports,
                boolean wasmBigInt,
                boolean esmEvalReturnsExports,
                boolean isMLEMode,
                boolean privateFieldsIn,
                boolean esmBareSpecifierRelativeLookup,
                boolean scopeOptimization,
                boolean bigInt,
                boolean classFields,
                boolean shebang,
                boolean syntaxExtensions,
                boolean scripting,
                boolean functionStatementError,
                boolean constAsVar,
                boolean profileTime,
                boolean arrayElementsAmongMembers,
                boolean stackTraceAPI,
                boolean worker,
                String locale) {

    public static JSLanguageOptions fromOptionValues(SandboxPolicy sandboxPolicy, OptionValues optionValues) {
        return fromContextOptions(JSContextOptions.fromOptionValues(sandboxPolicy, optionValues));
    }

    public static JSLanguageOptions fromContextOptions(JSContextOptions options) {
        CompilerAsserts.neverPartOfCompilation();
        boolean nashornCompatibilityMode = options.isNashornCompatibilityMode();
        int ecmascriptVersion = options.getEcmaScriptVersion();
        boolean strict = options.isStrict();
        boolean annexB = options.isAnnexB();
        boolean intl402 = options.isIntl402();
        boolean regexpMatchIndices = options.isRegexpMatchIndices();
        boolean regexpUnicodeSets = options.isRegexpUnicodeSets();
        boolean sharedArrayBuffer = options.isSharedArrayBuffer();
        boolean v8RealmBuiltin = options.isV8RealmBuiltin();
        boolean parseOnly = options.isParseOnly();
        boolean zoneRulesBasedTimeZones = options.hasZoneRulesBasedTimeZones();
        boolean agentCanBlock = options.canAgentBlock();
        boolean awaitOptimization = options.isAwaitOptimization();
        boolean allowEval = options.allowEval();
        boolean disableWith = options.isDisableWith();
        boolean regexDumpAutomata = options.isRegexDumpAutomata();
        boolean regexStepExecution = options.isRegexStepExecution();
        boolean regexAlwaysEager = options.isRegexAlwaysEager();
        boolean scriptEngineGlobalScopeImport = options.isScriptEngineGlobalScopeImport();
        boolean hasForeignObjectPrototype = options.hasForeignObjectPrototype();
        boolean hasForeignHashProperties = options.hasForeignHashProperties();
        long functionArgumentsLimit = options.getFunctionArgumentsLimit();
        boolean test262Mode = options.isTest262Mode();
        boolean testV8Mode = options.isTestV8Mode();
        boolean validateRegExpLiterals = options.isValidateRegExpLiterals();
        int functionConstructorCacheSize = options.getFunctionConstructorCacheSize();
        int regexCacheSize = options.getRegexCacheSize();
        int stringLengthLimit = options.getStringLengthLimit();
        boolean stringLazySubstrings = options.isStringLazySubstrings();
        boolean bindMemberFunctions = options.bindMemberFunctions();
        boolean commonJSRequire = options.isCommonJSRequire();
        boolean regexRegressionTestMode = options.isRegexRegressionTestMode();
        boolean testCloneUninitialized = options.isTestCloneUninitialized();
        boolean lazyTranslation = options.isLazyTranslation();
        int stackTraceLimit = options.getStackTraceLimit();
        int maxTypedArrayLength = options.getMaxTypedArrayLength();
        int maxApplyArgumentLength = options.getMaxApplyArgumentLength();
        int maxPrototypeChainLength = options.getMaxPrototypeChainLength();
        boolean asyncStackTraces = options.isAsyncStackTraces();
        boolean topLevelAwait = options.isTopLevelAwait();
        boolean useUTCForLegacyDates = options.shouldUseUTCForLegacyDates();
        boolean webAssembly = options.isWebAssembly();
        UnhandledRejectionsTrackingMode unhandledRejectionsMode = options.getUnhandledRejectionsMode();
        boolean newSetMethods = options.isNewSetMethods();
        boolean iteratorHelpers = options.isIteratorHelpers();
        boolean asyncIteratorHelpers = options.isAsyncIteratorHelpers();
        boolean shadowRealm = options.isShadowRealm();
        boolean asyncContext = options.isAsyncContext();
        boolean operatorOverloading = options.isOperatorOverloading();
        boolean errorCause = options.isErrorCauseEnabled();
        boolean importAttributes = options.isImportAttributes();
        boolean importAssertions = options.isImportAssertions();
        boolean jsonModules = options.isJsonModules();
        boolean sourcePhaseImports = options.isSourcePhaseImports();
        boolean wasmBigInt = options.isWasmBigInt();
        boolean esmEvalReturnsExports = options.isEsmEvalReturnsExports();
        boolean printNoNewline = options.isPrintNoNewline();
        boolean mleMode = options.isMLEMode();
        boolean privateFieldsIn = options.isPrivateFieldsIn();
        boolean esmBareSpecifierRelativeLookup = options.isEsmBareSpecifierRelativeLookup();
        boolean temporal = options.isTemporal();
        int propertyCacheLimit = options.getPropertyCacheLimit();
        int functionCacheLimit = options.getFunctionCacheLimit();
        short frequencyBasedPropertyCacheLimit = options.getFrequencyBasedPropertyCacheLimit();
        boolean scopeOptimization = options.isScopeOptimization();
        boolean v8Intrinsics = options.isV8Intrinsics();
        boolean bigInt = options.isBigInt();
        boolean classFields = options.isClassFields();
        boolean shebang = options.isShebang();
        boolean syntaxExtensions = options.isSyntaxExtensions();
        boolean scripting = options.isScripting();
        boolean functionStatementError = options.isFunctionStatementError();
        boolean constAsVar = options.isConstAsVar();
        boolean profileTime = options.isProfileTime();
        boolean arrayElementsAmongMembers = options.isArrayElementsAmongMembers();
        boolean stackTraceAPI = options.isStackTraceAPI();
        boolean worker = options.isWorker();
        String locale = options.getLocale();

        return new JSLanguageOptions(
                        ecmascriptVersion,
                        strict,
                        annexB,
                        intl402,
                        regexpMatchIndices,
                        regexpUnicodeSets,
                        sharedArrayBuffer,
                        v8RealmBuiltin,
                        nashornCompatibilityMode,
                        stackTraceLimit,
                        parseOnly,
                        zoneRulesBasedTimeZones,
                        agentCanBlock,
                        printNoNewline,
                        commonJSRequire,
                        awaitOptimization,
                        allowEval,
                        disableWith,
                        regexDumpAutomata,
                        regexStepExecution,
                        regexAlwaysEager,
                        scriptEngineGlobalScopeImport,
                        hasForeignObjectPrototype,
                        hasForeignHashProperties,
                        functionArgumentsLimit,
                        test262Mode,
                        testV8Mode,
                        validateRegExpLiterals,
                        functionConstructorCacheSize,
                        regexCacheSize,
                        stringLengthLimit,
                        stringLazySubstrings,
                        bindMemberFunctions,
                        regexRegressionTestMode,
                        testCloneUninitialized,
                        lazyTranslation,
                        maxTypedArrayLength,
                        maxApplyArgumentLength,
                        maxPrototypeChainLength,
                        asyncStackTraces,
                        propertyCacheLimit,
                        functionCacheLimit,
                        frequencyBasedPropertyCacheLimit,
                        topLevelAwait,
                        useUTCForLegacyDates,
                        webAssembly,
                        newSetMethods,
                        temporal,
                        iteratorHelpers,
                        asyncIteratorHelpers,
                        shadowRealm,
                        asyncContext,
                        v8Intrinsics,
                        unhandledRejectionsMode,
                        operatorOverloading,
                        errorCause,
                        importAttributes,
                        importAssertions,
                        jsonModules,
                        sourcePhaseImports,
                        wasmBigInt,
                        esmEvalReturnsExports,
                        mleMode,
                        privateFieldsIn,
                        esmBareSpecifierRelativeLookup,
                        scopeOptimization,
                        bigInt,
                        classFields,
                        shebang,
                        syntaxExtensions,
                        scripting,
                        functionStatementError,
                        constAsVar,
                        profileTime,
                        arrayElementsAmongMembers,
                        stackTraceAPI,
                        worker,
                        locale);
    }

}
