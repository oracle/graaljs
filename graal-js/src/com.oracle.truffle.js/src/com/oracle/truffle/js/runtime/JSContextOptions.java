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

import static com.oracle.truffle.js.runtime.JSTruffleOptions.JS_OPTION_PREFIX;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

import org.graalvm.options.OptionCategory;
import org.graalvm.options.OptionDescriptor;
import org.graalvm.options.OptionKey;
import org.graalvm.options.OptionStability;
import org.graalvm.options.OptionType;
import org.graalvm.options.OptionValues;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.Option;
import com.oracle.truffle.api.nodes.InvalidAssumptionException;
import com.oracle.truffle.api.utilities.CyclicAssumption;

public final class JSContextOptions {
    @CompilationFinal private JSParserOptions parserOptions;
    @CompilationFinal private OptionValues optionValues;

    public static final String ECMASCRIPT_VERSION_NAME = JS_OPTION_PREFIX + "ecmascript-version";
    @Option(name = ECMASCRIPT_VERSION_NAME, category = OptionCategory.USER, stability = OptionStability.STABLE, help = "ECMAScript Version.") //
    public static final OptionKey<Integer> ECMASCRIPT_VERSION = new OptionKey<>(
                    JSTruffleOptions.LatestECMAScriptVersion,
                    new OptionType<>(
                                    "ecmascript-version",
                                    new Function<String, Integer>() {

                                        @Override
                                        public Integer apply(String t) {
                                            try {
                                                int version = Integer.parseInt(t);

                                                int minYearVersion = JSTruffleOptions.ECMAScript6 + JSTruffleOptions.ECMAScriptNumberYearDelta;
                                                int maxYearVersion = JSTruffleOptions.MaxECMAScriptVersion + JSTruffleOptions.ECMAScriptNumberYearDelta;
                                                if (minYearVersion <= version && version <= maxYearVersion) {
                                                    version -= JSTruffleOptions.ECMAScriptNumberYearDelta;
                                                }
                                                if (version < 5 || version > JSTruffleOptions.MaxECMAScriptVersion) {
                                                    throw new IllegalArgumentException(
                                                                    "Supported values are 5 to " + JSTruffleOptions.MaxECMAScriptVersion + " or " + minYearVersion + " to " + maxYearVersion + ".");
                                                }
                                                return version;
                                            } catch (NumberFormatException e) {
                                                throw new IllegalArgumentException(e.getMessage(), e);
                                            }
                                        }
                                    }));
    @CompilationFinal private int ecmascriptVersion;

    public static final String ANNEX_B_NAME = JS_OPTION_PREFIX + "annex-b";
    @Option(name = ANNEX_B_NAME, category = OptionCategory.USER, help = "Enable ECMAScript Annex B features.") //
    public static final OptionKey<Boolean> ANNEX_B = new OptionKey<>(JSTruffleOptions.AnnexB);
    @CompilationFinal private boolean annexB;

    public static final String SYNTAX_EXTENSIONS_NAME = JS_OPTION_PREFIX + "syntax-extensions";
    @Option(name = SYNTAX_EXTENSIONS_NAME, category = OptionCategory.USER, help = "Enable Nashorn syntax extensions.") //
    public static final OptionKey<Boolean> SYNTAX_EXTENSIONS = new OptionKey<>(false);

    public static final String SCRIPTING_NAME = JS_OPTION_PREFIX + "scripting";
    @Option(name = SCRIPTING_NAME, category = OptionCategory.USER, help = "Enable scripting features (Nashorn compatibility option).") //
    public static final OptionKey<Boolean> SCRIPTING = new OptionKey<>(false);

    public static final String SHEBANG_NAME = JS_OPTION_PREFIX + "shebang";
    @Option(name = SHEBANG_NAME, category = OptionCategory.USER, help = "Allow parsing files starting with #!.") //
    public static final OptionKey<Boolean> SHEBANG = new OptionKey<>(false);

    public static final String STRICT_NAME = JS_OPTION_PREFIX + "strict";
    @Option(name = STRICT_NAME, category = OptionCategory.USER, stability = OptionStability.STABLE, help = "Enforce strict mode.") //
    public static final OptionKey<Boolean> STRICT = new OptionKey<>(false);

    public static final String CONST_AS_VAR_NAME = JS_OPTION_PREFIX + "const-as-var";
    @Option(name = CONST_AS_VAR_NAME, category = OptionCategory.EXPERT, help = "Parse const declarations as a var (legacy compatibility option).") //
    public static final OptionKey<Boolean> CONST_AS_VAR = new OptionKey<>(false);

    public static final String FUNCTION_STATEMENT_ERROR_NAME = JS_OPTION_PREFIX + "function-statement-error";
    @Option(name = FUNCTION_STATEMENT_ERROR_NAME, category = OptionCategory.EXPERT, help = "Treat hoistable function statements in blocks as an error (in ES5 mode).") //
    public static final OptionKey<Boolean> FUNCTION_STATEMENT_ERROR = new OptionKey<>(false);

    public static final String INTL_402_NAME = JS_OPTION_PREFIX + "intl-402";
    @Option(name = INTL_402_NAME, category = OptionCategory.USER, stability = OptionStability.STABLE, help = "Enable ECMAScript Internationalization API.") //
    public static final OptionKey<Boolean> INTL_402 = new OptionKey<>(false);
    @CompilationFinal private boolean intl402;

    public static final String REGEXP_STATIC_RESULT_NAME = JS_OPTION_PREFIX + "regexp-static-result";
    @Option(name = REGEXP_STATIC_RESULT_NAME, category = OptionCategory.USER, help = "Provide last RegExp match in RegExp global var, e.g. RegExp.$1.") //
    public static final OptionKey<Boolean> REGEXP_STATIC_RESULT = new OptionKey<>(true);
    @CompilationFinal private boolean regexpStaticResult;

    public static final String ARRAY_SORT_INHERITED_NAME = JS_OPTION_PREFIX + "array-sort-inherited";
    @Option(name = ARRAY_SORT_INHERITED_NAME, category = OptionCategory.USER, help = "Sort inherited keys in Array.protoype.sort.") //
    public static final OptionKey<Boolean> ARRAY_SORT_INHERITED = new OptionKey<>(true);
    private final CyclicAssumption arraySortInheritedCyclicAssumption = new CyclicAssumption("The " + ARRAY_SORT_INHERITED_NAME + " option is stable.");
    @CompilationFinal private Assumption arraySortInheritedCurrentAssumption = arraySortInheritedCyclicAssumption.getAssumption();
    @CompilationFinal private boolean arraySortInherited;

    public static final String SHARED_ARRAY_BUFFER_NAME = JS_OPTION_PREFIX + "shared-array-buffer";
    @Option(name = SHARED_ARRAY_BUFFER_NAME, category = OptionCategory.USER, help = "Enable ES2017 SharedArrayBuffer.") //
    public static final OptionKey<Boolean> SHARED_ARRAY_BUFFER = new OptionKey<>(true);
    @CompilationFinal private boolean sharedArrayBuffer;

    public static final String ATOMICS_NAME = JS_OPTION_PREFIX + "atomics";
    @Option(name = ATOMICS_NAME, category = OptionCategory.USER, help = "Enable ES2017 Atomics.") //
    public static final OptionKey<Boolean> ATOMICS = new OptionKey<>(true);

    public static final String V8_COMPATIBILITY_MODE_NAME = JS_OPTION_PREFIX + "v8-compat";
    @Option(name = V8_COMPATIBILITY_MODE_NAME, category = OptionCategory.USER, help = "Provide compatibility with the Google V8 engine.") //
    public static final OptionKey<Boolean> V8_COMPATIBILITY_MODE = new OptionKey<>(false);
    private final CyclicAssumption v8CompatibilityModeCyclicAssumption = new CyclicAssumption("The " + V8_COMPATIBILITY_MODE_NAME + " option is stable.");
    @CompilationFinal private Assumption v8CompatibilityModeCurrentAssumption = v8CompatibilityModeCyclicAssumption.getAssumption();
    @CompilationFinal private boolean v8CompatibilityMode;

    public static final String V8_REALM_BUILTIN_NAME = JS_OPTION_PREFIX + "v8-realm-builtin";
    @Option(name = V8_REALM_BUILTIN_NAME, category = OptionCategory.INTERNAL, help = "Provide Realm builtin compatible with V8's d8 shell.") //
    public static final OptionKey<Boolean> V8_REALM_BUILTIN = new OptionKey<>(false);
    @CompilationFinal private boolean v8RealmBuiltin;

    public static final String V8_LEGACY_CONST_NAME = JS_OPTION_PREFIX + "v8-legacy-const";
    @Option(name = V8_LEGACY_CONST_NAME, category = OptionCategory.INTERNAL, help = "Emulate v8 behavior when trying to mutate const variables in non-strict mode.") //
    public static final OptionKey<Boolean> V8_LEGACY_CONST = new OptionKey<>(false);
    @CompilationFinal private boolean v8LegacyConst;

    public static final String NASHORN_COMPATIBILITY_MODE_NAME = JS_OPTION_PREFIX + "nashorn-compat";
    @Option(name = NASHORN_COMPATIBILITY_MODE_NAME, category = OptionCategory.USER, help = "Provide compatibility with the OpenJDK Nashorn engine. Do not use with untrusted code.") //
    public static final OptionKey<Boolean> NASHORN_COMPATIBILITY_MODE = new OptionKey<>(false);
    @CompilationFinal private boolean nashornCompatibilityMode;

    public static final String STACK_TRACE_LIMIT_NAME = JS_OPTION_PREFIX + "stack-trace-limit";
    @Option(name = STACK_TRACE_LIMIT_NAME, category = OptionCategory.USER, help = "Number of stack frames to capture.") //
    public static final OptionKey<Integer> STACK_TRACE_LIMIT = new OptionKey<>(JSTruffleOptions.StackTraceLimit);

    public static final String DEBUG_BUILTIN_NAME = JS_OPTION_PREFIX + "debug-builtin";
    @Option(name = DEBUG_BUILTIN_NAME, category = OptionCategory.INTERNAL, help = "Provide a non-API Debug builtin. Behaviour will likely change. Don't depend on this in production code.") //
    public static final OptionKey<Boolean> DEBUG_BUILTIN = new OptionKey<>(false);
    @CompilationFinal private boolean debug;

    public static final String DIRECT_BYTE_BUFFER_NAME = JS_OPTION_PREFIX + "direct-byte-buffer";
    @Option(name = DIRECT_BYTE_BUFFER_NAME, category = OptionCategory.USER, help = "Use direct (off-heap) byte buffer for typed arrays.") //
    public static final OptionKey<Boolean> DIRECT_BYTE_BUFFER = new OptionKey<>(JSTruffleOptions.DirectByteBuffer);
    private final CyclicAssumption directByteBufferCyclicAssumption = new CyclicAssumption("The " + DIRECT_BYTE_BUFFER_NAME + " option is stable.");
    @CompilationFinal private Assumption directByteBufferCurrentAssumption = directByteBufferCyclicAssumption.getAssumption();
    @CompilationFinal private boolean directByteBuffer;

    public static final String PARSE_ONLY_NAME = JS_OPTION_PREFIX + "parse-only";
    @Option(name = PARSE_ONLY_NAME, category = OptionCategory.INTERNAL, help = "Only parse source code, do not run it.") //
    public static final OptionKey<Boolean> PARSE_ONLY = new OptionKey<>(false);
    @CompilationFinal private boolean parseOnly;

    public static final String TIME_ZONE_NAME = JS_OPTION_PREFIX + "timezone";
    @Option(name = TIME_ZONE_NAME, category = OptionCategory.USER, help = "Set custom timezone.") //
    public static final OptionKey<String> TIME_ZONE = new OptionKey<>("");

    public static final String TIMER_RESOLUTION_NAME = JS_OPTION_PREFIX + "timer-resolution";
    @Option(name = TIMER_RESOLUTION_NAME, category = OptionCategory.USER, help = "Resolution of timers (performance.now() and Date built-ins) in nanoseconds. Fuzzy time is used when set to 0.") //
    public static final OptionKey<Long> TIMER_RESOLUTION = new OptionKey<>(1000000L);
    private final CyclicAssumption timerResolutionCyclicAssumption = new CyclicAssumption("The " + TIMER_RESOLUTION_NAME + " option is stable.");
    @CompilationFinal private Assumption timerResolutionCurrentAssumption = timerResolutionCyclicAssumption.getAssumption();
    @CompilationFinal private long timerResolution;

    public static final String AGENT_CAN_BLOCK_NAME = JS_OPTION_PREFIX + "agent-can-block";
    @Option(name = AGENT_CAN_BLOCK_NAME, category = OptionCategory.INTERNAL, help = "Determines whether agents can block or not.") //
    public static final OptionKey<Boolean> AGENT_CAN_BLOCK = new OptionKey<>(true);
    @CompilationFinal private boolean agentCanBlock;

    public static final String JAVA_PACKAGE_GLOBALS_NAME = JS_OPTION_PREFIX + "java-package-globals";
    @Option(name = JAVA_PACKAGE_GLOBALS_NAME, category = OptionCategory.USER, help = "Provide Java package globals: Packages, java, javafx, javax, com, org, edu.") //
    public static final OptionKey<Boolean> JAVA_PACKAGE_GLOBALS = new OptionKey<>(true);

    public static final String GLOBAL_PROPERTY_NAME = JS_OPTION_PREFIX + "global-property";
    @Option(name = GLOBAL_PROPERTY_NAME, category = OptionCategory.USER, help = "Provide 'global' global property.") //
    public static final OptionKey<Boolean> GLOBAL_PROPERTY = new OptionKey<>(false);

    public static final String CONSOLE_NAME = JS_OPTION_PREFIX + "console";
    @Option(name = CONSOLE_NAME, category = OptionCategory.USER, help = "Provide 'console' global property.") //
    public static final OptionKey<Boolean> CONSOLE = new OptionKey<>(true);

    public static final String PERFORMANCE_NAME = JS_OPTION_PREFIX + "performance";
    @Option(name = PERFORMANCE_NAME, category = OptionCategory.USER, help = "Provide 'performance' global property.") //
    public static final OptionKey<Boolean> PERFORMANCE = new OptionKey<>(false);

    public static final String SHELL_NAME = JS_OPTION_PREFIX + "shell";
    @Option(name = SHELL_NAME, category = OptionCategory.USER, help = "Provide global functions for js shell.") //
    public static final OptionKey<Boolean> SHELL = new OptionKey<>(false);

    public static final String PRINT_NAME = JS_OPTION_PREFIX + "print";
    @Option(name = PRINT_NAME, category = OptionCategory.USER, help = "Provide 'print' global function.") //
    public static final OptionKey<Boolean> PRINT = new OptionKey<>(true);

    public static final String LOAD_NAME = JS_OPTION_PREFIX + "load";
    @Option(name = LOAD_NAME, category = OptionCategory.USER, help = "Provide 'load' global function.") //
    public static final OptionKey<Boolean> LOAD = new OptionKey<>(true);

    public static final String LOAD_FROM_URL_NAME = JS_OPTION_PREFIX + "load-from-url";
    @Option(name = LOAD_FROM_URL_NAME, category = OptionCategory.USER, help = "Allow 'load' to access URLs. Do not use with untrusted code.") //
    public static final OptionKey<Boolean> LOAD_FROM_URL = new OptionKey<>(false);

    public static final String LOAD_FROM_CLASSPATH_NAME = JS_OPTION_PREFIX + "load-from-classpath";
    @Option(name = LOAD_FROM_CLASSPATH_NAME, category = OptionCategory.USER, help = "Allow 'load' to access 'classpath:' URLs. Do not use with untrusted code.") //
    public static final OptionKey<Boolean> LOAD_FROM_CLASSPATH = new OptionKey<>(false);

    public static final String COMMONJS_REQUIRE_NAME = JS_OPTION_PREFIX + "cjs-require";
    @Option(name = COMMONJS_REQUIRE_NAME, category = OptionCategory.USER, help = "Enable CommonJS require emulation.") //
    public static final OptionKey<Boolean> COMMONJS_REQUIRE = new OptionKey<>(false);
    @CompilationFinal private boolean commonJsRequire;

    public static final String COMMONJS_REQUIRE_CWS_NAME = JS_OPTION_PREFIX + "cjs-require-cwd";
    @Option(name = COMMONJS_REQUIRE_CWS_NAME, category = OptionCategory.USER, help = "CommonJS default current working directory.") //
    public static final OptionKey<String> COMMONJS_REQUIRE_CWS = new OptionKey<>("");

    public static final String COMMONJS_REQUIRE_GLOBAL_BUILTINS_NAME = JS_OPTION_PREFIX + "cjs-global-builtins";
    @Option(name = COMMONJS_REQUIRE_GLOBAL_BUILTINS_NAME, category = OptionCategory.USER, help = "Npm packages used to replace global Node.js builtins. Syntax: <name,module-name>.") //
    public static final OptionKey<String> COMMONJS_REQUIRE_GLOBAL_BUILTINS = new OptionKey<>("");

    public static final String COMMONJS_REQUIRE_GLOBAL_PROPERTIES_NAME = JS_OPTION_PREFIX + "cjs-global-properties";
    @Option(name = COMMONJS_REQUIRE_GLOBAL_PROPERTIES_NAME, category = OptionCategory.USER, help = "Npm package used to populate Node.js global object.") //
    public static final OptionKey<String> COMMONJS_REQUIRE_GLOBAL_PROPERTIES = new OptionKey<>("");

    public static final String GRAAL_BUILTIN_NAME = JS_OPTION_PREFIX + "graal-builtin";
    @Option(name = GRAAL_BUILTIN_NAME, category = OptionCategory.USER, help = "Provide 'Graal' global property.") //
    public static final OptionKey<Boolean> GRAAL_BUILTIN = new OptionKey<>(true);

    public static final String POLYGLOT_BUILTIN_NAME = JS_OPTION_PREFIX + "polyglot-builtin";
    @Option(name = POLYGLOT_BUILTIN_NAME, category = OptionCategory.USER, help = "Provide 'Polyglot' global property.", deprecated = true) //
    public static final OptionKey<Boolean> POLYGLOT_BUILTIN = new OptionKey<>(true);

    public static final String POLYGLOT_EVALFILE_NAME = JS_OPTION_PREFIX + "polyglot-evalfile";
    @Option(name = POLYGLOT_EVALFILE_NAME, category = OptionCategory.USER, help = "Provide 'Polyglot.evalFile' function.") //
    public static final OptionKey<Boolean> POLYGLOT_EVALFILE = new OptionKey<>(true);

    public static final String AWAIT_OPTIMIZATION_NAME = JS_OPTION_PREFIX + "await-optimization";
    @Option(name = AWAIT_OPTIMIZATION_NAME, category = OptionCategory.INTERNAL, help = "Use PromiseResolve for Await.") //
    public static final OptionKey<Boolean> AWAIT_OPTIMIZATION = new OptionKey<>(true);
    @CompilationFinal private boolean awaitOptimization;

    public static final String DISABLE_EVAL_NAME = JS_OPTION_PREFIX + "disable-eval";
    @Option(name = DISABLE_EVAL_NAME, category = OptionCategory.EXPERT, help = "User code is not allowed to parse code via e.g. eval().") //
    public static final OptionKey<Boolean> DISABLE_EVAL = new OptionKey<>(false);
    @CompilationFinal private boolean disableEval;

    public static final String DISABLE_WITH_NAME = JS_OPTION_PREFIX + "disable-with";
    @Option(name = DISABLE_WITH_NAME, category = OptionCategory.EXPERT, help = "User code is not allowed to use the 'with' statement.") //
    public static final OptionKey<Boolean> DISABLE_WITH = new OptionKey<>(false);
    @CompilationFinal private boolean disableWith;

    public static final String BIGINT_NAME = JS_OPTION_PREFIX + "bigint";
    @Option(name = BIGINT_NAME, category = OptionCategory.USER, help = "Provide an implementation of the BigInt proposal.") //
    public static final OptionKey<Boolean> BIGINT = new OptionKey<>(true);

    public static final String REGEX_DUMP_AUTOMATA_NAME = JS_OPTION_PREFIX + "regex.dump-automata";
    @Option(name = REGEX_DUMP_AUTOMATA_NAME, category = OptionCategory.INTERNAL, help = "Produce ASTs and automata in JSON, DOT (GraphViz) and LaTeX formats.") //
    public static final OptionKey<Boolean> REGEX_DUMP_AUTOMATA = new OptionKey<>(false);
    @CompilationFinal private boolean regexDumpAutomata;

    public static final String REGEX_STEP_EXECUTION_NAME = JS_OPTION_PREFIX + "regex.step-execution";
    @Option(name = REGEX_STEP_EXECUTION_NAME, category = OptionCategory.INTERNAL, help = "Trace the execution of automata in JSON files.") //
    public static final OptionKey<Boolean> REGEX_STEP_EXECUTION = new OptionKey<>(false);
    @CompilationFinal private boolean regexStepExecution;

    public static final String REGEX_ALWAYS_EAGER_NAME = JS_OPTION_PREFIX + "regex.always-eager";
    @Option(name = REGEX_ALWAYS_EAGER_NAME, category = OptionCategory.INTERNAL, help = "Always match capture groups eagerly.") //
    public static final OptionKey<Boolean> REGEX_ALWAYS_EAGER = new OptionKey<>(false);
    @CompilationFinal private boolean regexAlwaysEager;

    public static final String SCRIPT_ENGINE_GLOBAL_SCOPE_IMPORT_NAME = JS_OPTION_PREFIX + "script-engine-global-scope-import";
    /*
     * The option needs to be stable it is used in our GraalJSScriptEngine implementation.
     */
    @Option(name = SCRIPT_ENGINE_GLOBAL_SCOPE_IMPORT_NAME, deprecated = true, stability = OptionStability.STABLE, category = OptionCategory.INTERNAL, help = "Enable ScriptEngine-specific global scope import function.") //
    public static final OptionKey<Boolean> SCRIPT_ENGINE_GLOBAL_SCOPE_IMPORT = new OptionKey<>(false);
    @CompilationFinal private boolean scriptEngineGlobalScopeImport;

    public static final String FOREIGN_OBJECT_PROTOTYPE_NAME = JS_OPTION_PREFIX + "experimental-foreign-object-prototype";
    @Option(name = FOREIGN_OBJECT_PROTOTYPE_NAME, category = OptionCategory.EXPERT, help = "Non-JS objects have prototype (Object/Function/Array.prototype) set.") //
    public static final OptionKey<Boolean> FOREIGN_OBJECT_PROTOTYPE = new OptionKey<>(false);
    @CompilationFinal private boolean hasForeignObjectPrototype;

    public static final String SIMDJS_NAME = JS_OPTION_PREFIX + "simdjs";
    @Option(name = SIMDJS_NAME, category = OptionCategory.EXPERT, help = "Provide an experimental implementation of the SIMD.js proposal.") //
    public static final OptionKey<Boolean> SIMDJS = new OptionKey<>(false);

    // limit originally from TestV8 regress-1122.js, regress-605470.js
    public static final String FUNCTION_ARGUMENTS_LIMIT_NAME = JS_OPTION_PREFIX + "function-arguments-limit";
    @Option(name = FUNCTION_ARGUMENTS_LIMIT_NAME, category = OptionCategory.EXPERT, help = "Maximum number of arguments for functions.") //
    public static final OptionKey<Long> FUNCTION_ARGUMENTS_LIMIT = new OptionKey<>(65535L);
    @CompilationFinal private long functionArgumentsLimit;

    public static final String TEST262_MODE_NAME = JS_OPTION_PREFIX + "test262-mode";
    @Option(name = TEST262_MODE_NAME, category = OptionCategory.INTERNAL, help = "Expose global property $262 needed to run the Test262 harness.") //
    public static final OptionKey<Boolean> TEST262_MODE = new OptionKey<>(false);
    @CompilationFinal private boolean test262Mode;

    public static final String TESTV8_MODE_NAME = JS_OPTION_PREFIX + "testV8-mode";
    @Option(name = TESTV8_MODE_NAME, category = OptionCategory.INTERNAL, help = "Expose internals needed to run the TestV8 harness.") //
    public static final OptionKey<Boolean> TESTV8_MODE = new OptionKey<>(false);
    @CompilationFinal private boolean testV8Mode;

    public static final String VALIDATE_REGEXP_LITERALS_NAME = JS_OPTION_PREFIX + "validate-regexp-literals";
    @Option(name = VALIDATE_REGEXP_LITERALS_NAME, category = OptionCategory.INTERNAL, help = "Validate regexp literals at parse time.") //
    public static final OptionKey<Boolean> VALIDATE_REGEXP_LITERALS = new OptionKey<>(true);
    @CompilationFinal private boolean validateRegExpLiterals;

    public static final String LOCALE_NAME = JS_OPTION_PREFIX + "locale";
    @Option(name = LOCALE_NAME, category = OptionCategory.EXPERT, help = "Use a specific default locale for locale-sensitive operations.") //
    public static final OptionKey<String> LOCALE = new OptionKey<>("");

    public static final String FUNCTION_CONSTRUCTOR_CACHE_SIZE_NAME = JS_OPTION_PREFIX + "function-constructor-cache-size";
    @Option(name = FUNCTION_CONSTRUCTOR_CACHE_SIZE_NAME, category = OptionCategory.EXPERT, help = "Maximum size of the parsing cache used by the Function constructor to avoid re-parsing known sources.") //
    public static final OptionKey<Integer> FUNCTION_CONSTRUCTOR_CACHE_SIZE = new OptionKey<>(32);
    @CompilationFinal private int functionConstructorCacheSize;

    public static final String STRING_LENGTH_LIMIT_NAME = JS_OPTION_PREFIX + "string-length-limit";
    @Option(name = STRING_LENGTH_LIMIT_NAME, category = OptionCategory.EXPERT, help = "Maximum string length.") //
    public static final OptionKey<Integer> STRING_LENGTH_LIMIT = new OptionKey<>(JSTruffleOptions.StringLengthLimit);
    @CompilationFinal private int stringLengthLimit;

    public static final String BIND_MEMBER_FUNCTIONS_NAME = JS_OPTION_PREFIX + "bind-member-functions";
    @Option(name = BIND_MEMBER_FUNCTIONS_NAME, category = OptionCategory.EXPERT, help = "Bind functions returned by Value.getMember to the receiver object.") //
    public static final OptionKey<Boolean> BIND_MEMBER_FUNCTIONS = new OptionKey<>(true);
    @CompilationFinal private boolean bindMemberFunctions;

    JSContextOptions(JSParserOptions parserOptions, OptionValues optionValues) {
        this.parserOptions = parserOptions;
        this.optionValues = optionValues;
        setOptionValues(optionValues);
    }

    public static JSContextOptions fromOptionValues(OptionValues optionValues) {
        return new JSContextOptions(new JSParserOptions(), optionValues);
    }

    public JSParserOptions getParserOptions() {
        return parserOptions;
    }

    public void setParserOptions(JSParserOptions parserOptions) {
        CompilerAsserts.neverPartOfCompilation();
        this.parserOptions = parserOptions;
    }

    public void setOptionValues(OptionValues newOptions) {
        CompilerAsserts.neverPartOfCompilation();
        optionValues = newOptions;
        cacheOptions();
        parserOptions = parserOptions.putOptions(newOptions);
    }

    private void cacheOptions() {
        this.ecmascriptVersion = readIntegerOption(ECMASCRIPT_VERSION);
        this.annexB = readBooleanOption(ANNEX_B);
        this.intl402 = readBooleanOption(INTL_402);
        this.regexpStaticResult = readBooleanOption(REGEXP_STATIC_RESULT);
        this.arraySortInherited = patchBooleanOption(ARRAY_SORT_INHERITED, ARRAY_SORT_INHERITED_NAME, arraySortInherited, msg -> {
            arraySortInheritedCyclicAssumption.invalidate(msg);
            arraySortInheritedCurrentAssumption = arraySortInheritedCyclicAssumption.getAssumption();
        });
        this.sharedArrayBuffer = readBooleanOption(SHARED_ARRAY_BUFFER);
        this.v8CompatibilityMode = patchBooleanOption(V8_COMPATIBILITY_MODE, V8_COMPATIBILITY_MODE_NAME, v8CompatibilityMode, msg -> {
            v8CompatibilityModeCyclicAssumption.invalidate(msg);
            v8CompatibilityModeCurrentAssumption = v8CompatibilityModeCyclicAssumption.getAssumption();
        });
        this.v8RealmBuiltin = readBooleanOption(V8_REALM_BUILTIN);
        this.v8LegacyConst = readBooleanOption(V8_LEGACY_CONST);
        this.nashornCompatibilityMode = readBooleanOption(NASHORN_COMPATIBILITY_MODE);
        this.directByteBuffer = patchBooleanOption(DIRECT_BYTE_BUFFER, DIRECT_BYTE_BUFFER_NAME, directByteBuffer, msg -> {
            directByteBufferCyclicAssumption.invalidate(msg);
            directByteBufferCurrentAssumption = directByteBufferCyclicAssumption.getAssumption();
        });
        this.parseOnly = readBooleanOption(PARSE_ONLY);
        this.debug = readBooleanOption(DEBUG_BUILTIN);
        this.timerResolution = patchLongOption(TIMER_RESOLUTION, TIMER_RESOLUTION_NAME, timerResolution, msg -> {
            timerResolutionCyclicAssumption.invalidate(msg);
            timerResolutionCurrentAssumption = timerResolutionCyclicAssumption.getAssumption();
        });
        this.agentCanBlock = readBooleanOption(AGENT_CAN_BLOCK);
        this.awaitOptimization = readBooleanOption(AWAIT_OPTIMIZATION);
        this.disableEval = readBooleanOption(DISABLE_EVAL);
        this.disableWith = readBooleanOption(DISABLE_WITH);
        this.regexDumpAutomata = readBooleanOption(REGEX_DUMP_AUTOMATA);
        this.regexStepExecution = readBooleanOption(REGEX_STEP_EXECUTION);
        this.regexAlwaysEager = readBooleanOption(REGEX_ALWAYS_EAGER);
        this.scriptEngineGlobalScopeImport = readBooleanOption(SCRIPT_ENGINE_GLOBAL_SCOPE_IMPORT);
        this.hasForeignObjectPrototype = readBooleanOption(FOREIGN_OBJECT_PROTOTYPE);
        this.functionArgumentsLimit = readLongOption(FUNCTION_ARGUMENTS_LIMIT);
        this.test262Mode = readBooleanOption(TEST262_MODE);
        this.testV8Mode = readBooleanOption(TESTV8_MODE);
        this.validateRegExpLiterals = readBooleanOption(VALIDATE_REGEXP_LITERALS);
        this.functionConstructorCacheSize = readIntegerOption(FUNCTION_CONSTRUCTOR_CACHE_SIZE);
        this.stringLengthLimit = readIntegerOption(STRING_LENGTH_LIMIT);
        this.bindMemberFunctions = readBooleanOption(BIND_MEMBER_FUNCTIONS);
        this.commonJsRequire = readBooleanOption(COMMONJS_REQUIRE);
    }

    private boolean patchBooleanOption(OptionKey<Boolean> key, String name, boolean oldValue, Consumer<String> invalidate) {
        boolean newValue = readBooleanOption(key);
        if (oldValue != newValue) {
            invalidate.accept(String.format("Option %s was changed from %b to %b.", name, oldValue, newValue));
        }
        return newValue;
    }

    private boolean readBooleanOption(OptionKey<Boolean> key) {
        return key.getValue(optionValues);
    }

    private int readIntegerOption(OptionKey<Integer> key) {
        return key.getValue(optionValues);
    }

    private long patchLongOption(OptionKey<Long> key, String name, long oldValue, Consumer<String> invalidate) {
        long newValue = readLongOption(key);
        if (oldValue != newValue) {
            invalidate.accept(String.format("Option %s was changed from %d to %d.", name, oldValue, newValue));
        }
        return newValue;
    }

    private long readLongOption(OptionKey<Long> key) {
        return key.getValue(optionValues);
    }

    public static String helpWithDefault(String helpMessage, OptionKey<? extends Object> key) {
        return helpMessage + " (default:" + key.getDefaultValue() + ")";
    }

    public static OptionDescriptor newOptionDescriptor(OptionKey<?> key, String name, OptionCategory category, OptionStability stability, String help) {
        return OptionDescriptor.newBuilder(key, name).category(category).help(helpWithDefault(help, key)).stability(stability).build();
    }

    public static void describeOptions(List<OptionDescriptor> options) {
        for (OptionDescriptor desc : new JSContextOptionsOptionDescriptors()) {
            options.add(newOptionDescriptor(desc.getKey(), desc.getName(), desc.getCategory(), desc.getStability(), desc.getHelp()));
        }
    }

    public <T> boolean optionWillChange(OptionKey<T> option, OptionValues newOptionValues) {
        return !option.getValue(this.optionValues).equals(option.getValue(newOptionValues));
    }

    public int getEcmaScriptVersion() {
        return ecmascriptVersion;
    }

    public boolean isAnnexB() {
        return annexB;
    }

    public boolean isIntl402() {
        CompilerAsserts.neverPartOfCompilation("Patchable option intl-402 should never be accessed in compiled code.");
        return intl402;
    }

    public boolean isRegexpStaticResult() {
        return regexpStaticResult;
    }

    public boolean isArraySortInherited() {
        try {
            arraySortInheritedCurrentAssumption.check();
        } catch (InvalidAssumptionException e) {
        }
        return arraySortInherited;
    }

    public boolean isSharedArrayBuffer() {
        if (getEcmaScriptVersion() < 8) {
            return false;
        }
        return sharedArrayBuffer;
    }

    public boolean isAtomics() {
        if (getEcmaScriptVersion() < 8) {
            return false;
        }
        return ATOMICS.getValue(optionValues);
    }

    public boolean isV8CompatibilityMode() {
        try {
            v8CompatibilityModeCurrentAssumption.check();
        } catch (InvalidAssumptionException e) {
        }
        return v8CompatibilityMode;
    }

    public boolean isNashornCompatibilityMode() {
        return nashornCompatibilityMode;
    }

    public boolean isDebugBuiltin() {
        return debug;
    }

    public boolean isDirectByteBuffer() {
        try {
            directByteBufferCurrentAssumption.check();
        } catch (InvalidAssumptionException e) {
        }
        return directByteBuffer;
    }

    public boolean isParseOnly() {
        return parseOnly;
    }

    public long getTimerResolution() {
        try {
            timerResolutionCurrentAssumption.check();
        } catch (InvalidAssumptionException e) {
        }
        return timerResolution;
    }

    public boolean isV8RealmBuiltin() {
        return v8RealmBuiltin;
    }

    public boolean isV8LegacyConst() {
        return v8LegacyConst;
    }

    public boolean canAgentBlock() {
        return agentCanBlock;
    }

    public boolean isAwaitOptimization() {
        return awaitOptimization;
    }

    public boolean isDisableEval() {
        return disableEval;
    }

    public boolean isDisableWith() {
        return disableWith;
    }

    public boolean isRegexDumpAutomata() {
        return regexDumpAutomata;
    }

    public boolean isRegexStepExecution() {
        return regexStepExecution;
    }

    public boolean isRegexAlwaysEager() {
        return regexAlwaysEager;
    }

    public boolean isScriptEngineGlobalScopeImport() {
        return scriptEngineGlobalScopeImport;
    }

    public boolean hasForeignObjectPrototype() {
        return hasForeignObjectPrototype;
    }

    public boolean isGlobalProperty() {
        CompilerAsserts.neverPartOfCompilation("Context patchable option global-property was assumed not to be accessed in compiled code.");
        return GLOBAL_PROPERTY.getValue(optionValues);
    }

    public boolean isConsole() {
        CompilerAsserts.neverPartOfCompilation("Context patchable option console was assumed not to be accessed in compiled code.");
        return CONSOLE.getValue(optionValues) || (!CONSOLE.hasBeenSet(optionValues) && isShell());
    }

    public boolean isPrint() {
        CompilerAsserts.neverPartOfCompilation("Context patchable option print was assumed not to be accessed in compiled code.");
        return PRINT.getValue(optionValues) || (!PRINT.hasBeenSet(optionValues) && (isShell() || isNashornCompatibilityMode()));
    }

    public boolean isLoad() {
        CompilerAsserts.neverPartOfCompilation("Context patchable option load was assumed not to be accessed in compiled code.");
        return LOAD.getValue(optionValues) || (!LOAD.hasBeenSet(optionValues) && (isShell() || isNashornCompatibilityMode()));
    }

    public boolean isCommonJsRequire() {
        return commonJsRequire;
    }

    public Map<String, String> getCommonJsRequireBuiltins() {
        CompilerAsserts.neverPartOfCompilation("Context patchable option load was assumed not to be accessed in compiled code.");
        String value = COMMONJS_REQUIRE_GLOBAL_BUILTINS.getValue(optionValues);
        Map<String, String> map = new HashMap<>();
        if ("".equals(value)) {
            return map;
        }
        String[] options = value.split(",");
        for (String s : options) {
            String[] builtin = s.split(":");
            if (builtin.length != 2) {
                throw new IllegalArgumentException("Unexpected builtin arguments: " + s);
            }
            String key = builtin[0];
            String val = builtin[1];
            map.put(key, val);
        }
        return map;
    }

    public String getCommonJsRequireGlobals() {
        CompilerAsserts.neverPartOfCompilation("Context patchable option load was assumed not to be accessed in compiled code.");
        return COMMONJS_REQUIRE_GLOBAL_PROPERTIES.getValue(optionValues);
    }

    public String getRequireCwd() {
        CompilerAsserts.neverPartOfCompilation("Context patchable option load was assumed not to be accessed in compiled code.");
        return COMMONJS_REQUIRE_CWS.getValue(optionValues);
    }

    public boolean isPerformance() {
        CompilerAsserts.neverPartOfCompilation("Context patchable option performance was assumed not to be accessed in compiled code.");
        return PERFORMANCE.getValue(optionValues) || (!PERFORMANCE.hasBeenSet(optionValues) && isShell());
    }

    public boolean isShell() {
        CompilerAsserts.neverPartOfCompilation("Context patchable option shell was assumed not to be accessed in compiled code.");
        return SHELL.getValue(optionValues);
    }

    public boolean isGraalBuiltin() {
        return GRAAL_BUILTIN.getValue(optionValues);
    }

    public boolean isPolyglotBuiltin() {
        return POLYGLOT_BUILTIN.getValue(optionValues);
    }

    public boolean isPolyglotEvalFile() {
        return POLYGLOT_EVALFILE.getValue(optionValues);
    }

    public boolean isSIMDjs() {
        return SIMDJS.getValue(optionValues);
    }

    public boolean isLoadFromURL() {
        return LOAD_FROM_URL.getValue(optionValues);
    }

    public boolean isLoadFromClasspath() {
        return LOAD_FROM_CLASSPATH.getValue(optionValues);
    }

    public boolean isBigInt() {
        if (getEcmaScriptVersion() < JSTruffleOptions.ECMAScript2019) {
            return false;
        }
        return BIGINT.getValue(optionValues);
    }

    public long getFunctionArgumentsLimit() {
        return functionArgumentsLimit;
    }

    public boolean isTest262Mode() {
        return test262Mode;
    }

    public boolean isTestV8Mode() {
        return testV8Mode;
    }

    public boolean isValidateRegExpLiterals() {
        return validateRegExpLiterals;
    }

    public String getLocale() {
        return LOCALE.getValue(optionValues);
    }

    public int getFunctionConstructorCacheSize() {
        return functionConstructorCacheSize;
    }

    public int getStringLengthLimit() {
        return stringLengthLimit;
    }

    public boolean bindMemberFunctions() {
        return bindMemberFunctions;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 53 * hash + Objects.hashCode(this.parserOptions);
        hash = 53 * hash + this.ecmascriptVersion;
        hash = 53 * hash + (this.annexB ? 1 : 0);
        hash = 53 * hash + (this.intl402 ? 1 : 0);
        hash = 53 * hash + (this.regexpStaticResult ? 1 : 0);
        hash = 53 * hash + (this.arraySortInherited ? 1 : 0);
        hash = 53 * hash + (this.sharedArrayBuffer ? 1 : 0);
        hash = 53 * hash + (this.v8CompatibilityMode ? 1 : 0);
        hash = 53 * hash + (this.v8RealmBuiltin ? 1 : 0);
        hash = 53 * hash + (this.v8LegacyConst ? 1 : 0);
        hash = 53 * hash + (this.nashornCompatibilityMode ? 1 : 0);
        hash = 53 * hash + (this.debug ? 1 : 0);
        hash = 53 * hash + (this.directByteBuffer ? 1 : 0);
        hash = 53 * hash + (this.parseOnly ? 1 : 0);
        hash = 53 * hash + (int) this.timerResolution;
        hash = 53 * hash + (this.agentCanBlock ? 1 : 0);
        hash = 53 * hash + (this.awaitOptimization ? 1 : 0);
        hash = 53 * hash + (this.disableEval ? 1 : 0);
        hash = 53 * hash + (this.disableWith ? 1 : 0);
        hash = 53 * hash + (this.regexDumpAutomata ? 1 : 0);
        hash = 53 * hash + (this.regexStepExecution ? 1 : 0);
        hash = 53 * hash + (this.regexAlwaysEager ? 1 : 0);
        hash = 53 * hash + (this.scriptEngineGlobalScopeImport ? 1 : 0);
        hash = 53 * hash + (this.hasForeignObjectPrototype ? 1 : 0);
        hash = 53 * hash + (int) this.functionArgumentsLimit;
        hash = 53 * hash + (this.test262Mode ? 1 : 0);
        hash = 53 * hash + (this.testV8Mode ? 1 : 0);
        hash = 53 * hash + (this.validateRegExpLiterals ? 1 : 0);
        hash = 53 * hash + this.functionConstructorCacheSize;
        hash = 53 * hash + this.stringLengthLimit;
        hash = 53 * hash + (this.bindMemberFunctions ? 1 : 0);
        hash = 53 * hash + (this.commonJsRequire ? 1 : 0);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final JSContextOptions other = (JSContextOptions) obj;
        if (this.ecmascriptVersion != other.ecmascriptVersion) {
            return false;
        }
        if (this.annexB != other.annexB) {
            return false;
        }
        if (this.intl402 != other.intl402) {
            return false;
        }
        if (this.regexpStaticResult != other.regexpStaticResult) {
            return false;
        }
        if (this.arraySortInherited != other.arraySortInherited) {
            return false;
        }
        if (this.sharedArrayBuffer != other.sharedArrayBuffer) {
            return false;
        }
        if (this.v8CompatibilityMode != other.v8CompatibilityMode) {
            return false;
        }
        if (this.v8RealmBuiltin != other.v8RealmBuiltin) {
            return false;
        }
        if (this.v8LegacyConst != other.v8LegacyConst) {
            return false;
        }
        if (this.nashornCompatibilityMode != other.nashornCompatibilityMode) {
            return false;
        }
        if (this.debug != other.debug) {
            return false;
        }
        if (this.directByteBuffer != other.directByteBuffer) {
            return false;
        }
        if (this.parseOnly != other.parseOnly) {
            return false;
        }
        if (this.timerResolution != other.timerResolution) {
            return false;
        }
        if (this.agentCanBlock != other.agentCanBlock) {
            return false;
        }
        if (this.awaitOptimization != other.awaitOptimization) {
            return false;
        }
        if (this.disableEval != other.disableEval) {
            return false;
        }
        if (this.disableWith != other.disableWith) {
            return false;
        }
        if (this.regexDumpAutomata != other.regexDumpAutomata) {
            return false;
        }
        if (this.regexStepExecution != other.regexStepExecution) {
            return false;
        }
        if (this.regexAlwaysEager != other.regexAlwaysEager) {
            return false;
        }
        if (this.scriptEngineGlobalScopeImport != other.scriptEngineGlobalScopeImport) {
            return false;
        }
        if (this.hasForeignObjectPrototype != other.hasForeignObjectPrototype) {
            return false;
        }
        if (this.functionArgumentsLimit != other.functionArgumentsLimit) {
            return false;
        }
        if (this.test262Mode != other.test262Mode) {
            return false;
        }
        if (this.testV8Mode != other.testV8Mode) {
            return false;
        }
        if (this.validateRegExpLiterals != other.validateRegExpLiterals) {
            return false;
        }
        if (this.functionConstructorCacheSize != other.functionConstructorCacheSize) {
            return false;
        }
        if (this.stringLengthLimit != other.stringLengthLimit) {
            return false;
        }
        if (this.bindMemberFunctions != other.bindMemberFunctions) {
            return false;
        }
        if (this.commonJsRequire != other.commonJsRequire) {
            return false;
        }
        return Objects.equals(this.parserOptions, other.parserOptions);
    }
}
