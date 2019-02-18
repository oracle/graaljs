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

import static com.oracle.truffle.js.runtime.JSTruffleOptions.JS_OPTION_PREFIX;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.nodes.InvalidAssumptionException;
import org.graalvm.options.OptionCategory;
import org.graalvm.options.OptionDescriptor;
import org.graalvm.options.OptionKey;
import org.graalvm.options.OptionType;
import org.graalvm.options.OptionValues;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;

public final class JSContextOptions {
    @CompilationFinal private ParserOptions parserOptions;
    @CompilationFinal private OptionValues optionValues;

    public static final String ECMASCRIPT_VERSION_NAME = JS_OPTION_PREFIX + "ecmascript-version";
    public static final OptionKey<Integer> ECMASCRIPT_VERSION = new OptionKey<>(JSTruffleOptions.MaxECMAScriptVersion,
                    new OptionType<>(
                                    "ecmascript-version",
                                    JSTruffleOptions.MaxECMAScriptVersion,
                                    new Function<String, Integer>() {

                                        @Override
                                        public Integer apply(String t) {
                                            try {
                                                int version = Integer.parseInt(t);
                                                if (version < 5 || version > JSTruffleOptions.MaxECMAScriptVersion) {
                                                    throw new IllegalArgumentException("Supported values are between 5 and " + JSTruffleOptions.MaxECMAScriptVersion + ".");
                                                }
                                                return version;
                                            } catch (NumberFormatException e) {
                                                throw new IllegalArgumentException(e.getMessage(), e);
                                            }
                                        }
                                    }));
    private static final String ECMASCRIPT_VERSION_HELP = "ECMAScript Version.";
    @CompilationFinal private int ecmascriptVersion;

    public static final String ANNEX_B_NAME = JS_OPTION_PREFIX + "annex-b";
    public static final OptionKey<Boolean> ANNEX_B = new OptionKey<>(JSTruffleOptions.AnnexB);
    private static final String ANNEX_B_HELP = "Enable ECMAScript Annex B features.";
    @CompilationFinal private boolean annexB;

    public static final String INTL_402_NAME = JS_OPTION_PREFIX + "intl-402";
    public static final OptionKey<Boolean> INTL_402 = new OptionKey<>(false);
    private static final String INTL_402_HELP = "Enable ECMAScript Internationalization API";
    @CompilationFinal private boolean intl402;

    public static final String REGEXP_STATIC_RESULT_NAME = JS_OPTION_PREFIX + "regexp-static-result";
    private static final OptionKey<Boolean> REGEXP_STATIC_RESULT = new OptionKey<>(true);
    private static final String REGEXP_STATIC_RESULT_HELP = "provide last RegExp match in RegExp global var, e.g. RegExp.$1";
    @CompilationFinal private boolean regexpStaticResult;

    public static final String ARRAY_SORT_INHERITED_NAME = JS_OPTION_PREFIX + "array-sort-inherited";
    public static final OptionKey<Boolean> ARRAY_SORT_INHERITED = new OptionKey<>(true);
    private static final String ARRAY_SORT_INHERITED_HELP = "implementation-defined behavior in Array.protoype.sort: sort inherited keys?";
    @CompilationFinal private Assumption arraySortInheritedAssumption = Truffle.getRuntime().createAssumption("The array-sort-inherited option is stable.");
    @CompilationFinal private boolean arraySortInherited;

    public static final String SHARED_ARRAY_BUFFER_NAME = JS_OPTION_PREFIX + "shared-array-buffer";
    private static final OptionKey<Boolean> SHARED_ARRAY_BUFFER = new OptionKey<>(true);
    private static final String SHARED_ARRAY_BUFFER_HELP = "ES2017 SharedArrayBuffer";
    @CompilationFinal private boolean sharedArrayBuffer;

    public static final String ATOMICS_NAME = JS_OPTION_PREFIX + "atomics";
    private static final OptionKey<Boolean> ATOMICS = new OptionKey<>(true);
    private static final String ATOMICS_HELP = "ES2017 Atomics";
    @CompilationFinal private boolean atomics;

    public static final String V8_COMPATIBILITY_MODE_NAME = JS_OPTION_PREFIX + "v8-compat";
    public static final OptionKey<Boolean> V8_COMPATIBILITY_MODE = new OptionKey<>(false);
    private static final String V8_COMPATIBILITY_MODE_HELP = "provide compatibility with the Google V8 engine";
    @CompilationFinal private Assumption v8CompatibilityAssumption = Truffle.getRuntime().createAssumption("The v8-compat option is stable.");
    @CompilationFinal private boolean v8CompatibilityMode;

    public static final String V8_REALM_BUILTIN_NAME = JS_OPTION_PREFIX + "v8-realm-builtin";
    private static final OptionKey<Boolean> V8_REALM_BUILTIN = new OptionKey<>(false);
    private static final String V8_REALM_BUILTIN_HELP = "Provide Realm builtin compatible with V8's d8 shell.";
    @CompilationFinal private boolean v8RealmBuiltin;

    public static final String NASHORN_COMPATIBILITY_MODE_NAME = JS_OPTION_PREFIX + "nashorn-compat";
    public static final OptionKey<Boolean> NASHORN_COMPATIBILITY_MODE = new OptionKey<>(false);
    private static final String NASHORN_COMPATIBILITY_MODE_HELP = "provide compatibility with the OpenJDK Nashorn engine";
    @CompilationFinal private boolean nashornCompatibilityMode;

    public static final String STACK_TRACE_LIMIT_NAME = JS_OPTION_PREFIX + "stack-trace-limit";
    public static final OptionKey<Integer> STACK_TRACE_LIMIT = new OptionKey<>(JSTruffleOptions.StackTraceLimit);
    private static final String STACK_TRACE_LIMIT_HELP = "number of stack frames to capture";

    public static final String DEBUG_BUILTIN_NAME = JS_OPTION_PREFIX + "debug-builtin";
    private static final OptionKey<Boolean> DEBUG_BUILTIN = new OptionKey<>(false);
    private static final String DEBUG_BUILTIN_HELP = "provide a non-API Debug builtin. Behaviour will likely change. Don't depend on this in production code.";
    @CompilationFinal private boolean debug;

    public static final String DIRECT_BYTE_BUFFER_NAME = JS_OPTION_PREFIX + "direct-byte-buffer";
    public static final OptionKey<Boolean> DIRECT_BYTE_BUFFER = new OptionKey<>(JSTruffleOptions.DirectByteBuffer);
    private static final String DIRECT_BYTE_BUFFER_HELP = "Use direct (off-heap) byte buffer for typed arrays.";
    @CompilationFinal private Assumption directByteBufferAssumption = Truffle.getRuntime().createAssumption("The direct-byte-buffer option is stable.");
    @CompilationFinal private boolean directByteBuffer;

    public static final String PARSE_ONLY_NAME = JS_OPTION_PREFIX + "parse-only";
    private static final OptionKey<Boolean> PARSE_ONLY = new OptionKey<>(false);
    private static final String PARSE_ONLY_HELP = "Only parse source code, do not run it.";
    @CompilationFinal private boolean parseOnly;

    public static final String TIME_ZONE_NAME = JS_OPTION_PREFIX + "timezone";
    public static final OptionKey<String> TIME_ZONE = new OptionKey<>("");
    private static final String TIME_ZONE_HELP = "Set custom timezone.";

    public static final String TIMER_RESOLUTION_NAME = JS_OPTION_PREFIX + "timer-resolution";
    public static final OptionKey<Long> TIMER_RESOLUTION = new OptionKey<>(1000000L);
    private static final String TIMER_RESOLUTION_HELP = "Resolution of timers (performance.now() and Date built-ins) in nanoseconds. Fuzzy time is used when set to 0.";
    @CompilationFinal private Assumption timerResolutionAssumption = Truffle.getRuntime().createAssumption("The timer-resolution option is stable.");
    @CompilationFinal private long timerResolution;

    public static final String AGENT_CAN_BLOCK_NAME = JS_OPTION_PREFIX + "agent-can-block";
    public static final OptionKey<Boolean> AGENT_CAN_BLOCK = new OptionKey<>(true);
    private static final String AGENT_CAN_BLOCK_HELP = "Determines whether agents can block or not.";
    @CompilationFinal private boolean agentCanBlock;

    public static final String JAVA_PACKAGE_GLOBALS_NAME = JS_OPTION_PREFIX + "java-package-globals";
    public static final OptionKey<Boolean> JAVA_PACKAGE_GLOBALS = new OptionKey<>(true);
    private static final String JAVA_PACKAGE_GLOBALS_HELP = "provide Java package globals: Packages, java, javafx, javax, com, org, edu.";

    public static final String GLOBAL_PROPERTY_NAME = JS_OPTION_PREFIX + "global-property";
    public static final OptionKey<Boolean> GLOBAL_PROPERTY = new OptionKey<>(true);
    private static final String GLOBAL_PROPERTY_HELP = "provide 'global' global property.";
    public static final String GLOBAL_THIS_NAME = JS_OPTION_PREFIX + "global-this";
    private static final String GLOBAL_THIS_HELP = "provide 'global' global property. Deprecated option, use " + GLOBAL_PROPERTY_NAME + " instead.";

    public static final String CONSOLE_NAME = JS_OPTION_PREFIX + "console";
    public static final OptionKey<Boolean> CONSOLE = new OptionKey<>(true);
    private static final String CONSOLE_HELP = "provide 'console' global property.";

    public static final String PERFORMANCE_NAME = JS_OPTION_PREFIX + "performance";
    public static final OptionKey<Boolean> PERFORMANCE = new OptionKey<>(true);
    private static final String PERFORMANCE_HELP = "provide 'performance' global property.";

    public static final String SHELL_NAME = JS_OPTION_PREFIX + "shell";
    public static final OptionKey<Boolean> SHELL = new OptionKey<>(false);
    private static final String SHELL_HELP = "provide global functions for js shell.";

    public static final String PRINT_NAME = JS_OPTION_PREFIX + "print";
    public static final OptionKey<Boolean> PRINT = new OptionKey<>(true);
    private static final String PRINT_HELP = "provide 'print' global method.";

    public static final String LOAD_NAME = JS_OPTION_PREFIX + "load";
    public static final OptionKey<Boolean> LOAD = new OptionKey<>(true);
    private static final String LOAD_HELP = "provide 'load' global method.";

    public static final String GRAAL_BUILTIN_NAME = JS_OPTION_PREFIX + "graal-builtin";
    public static final OptionKey<Boolean> GRAAL_BUILTIN = new OptionKey<>(true);
    private static final String GRAAL_BUILTIN_HELP = "provide 'Graal' global property.";

    public static final String POLYGLOT_BUILTIN_NAME = JS_OPTION_PREFIX + "polyglot-builtin";
    public static final OptionKey<Boolean> POLYGLOT_BUILTIN = new OptionKey<>(true);
    private static final String POLYGLOT_BUILTIN_HELP = "provide 'Polyglot' global property.";

    public static final String AWAIT_OPTIMIZATION_NAME = JS_OPTION_PREFIX + "await-optimization";
    public static final OptionKey<Boolean> AWAIT_OPTIMIZATION = new OptionKey<>(true);
    private static final String AWAIT_OPTIMIZATION_HELP = "Use PromiseResolve for Await.";
    @CompilationFinal private boolean awaitOptimization;

    public static final String DISABLE_EVAL_NAME = JS_OPTION_PREFIX + "disable-eval";
    public static final OptionKey<Boolean> DISABLE_EVAL = new OptionKey<>(false);
    private static final String DISABLE_EVAL_HELP = "User code is not allowed to parse code via e.g. eval().";
    @CompilationFinal private boolean disableEval;

    public static final String DISABLE_WITH_NAME = JS_OPTION_PREFIX + "disable-with";
    public static final OptionKey<Boolean> DISABLE_WITH = new OptionKey<>(false);
    private static final String DISABLE_WITH_HELP = "User code is not allowed to use the 'with' statement.";
    @CompilationFinal private boolean disableWith;

    public static final String REGEX_DUMP_AUTOMATA_NAME = JS_OPTION_PREFIX + "regex.dump-automata";
    private static final OptionKey<Boolean> REGEX_DUMP_AUTOMATA = new OptionKey<>(false);
    private static final String REGEX_DUMP_AUTOMATA_HELP = "Produce ASTs and automata in JSON, DOT (GraphViz) and LaTeX formats.";
    @CompilationFinal private boolean regexDumpAutomata;

    public static final String REGEX_STEP_EXECUTION_NAME = JS_OPTION_PREFIX + "regex.step-execution";
    private static final OptionKey<Boolean> REGEX_STEP_EXECUTION = new OptionKey<>(false);
    private static final String REGEX_STEP_EXECUTION_HELP = "Trace the execution of automata in JSON files.";
    @CompilationFinal private boolean regexStepExecution;

    public static final String REGEX_ALWAYS_EAGER_NAME = JS_OPTION_PREFIX + "regex.always-eager";
    private static final OptionKey<Boolean> REGEX_ALWAYS_EAGER = new OptionKey<>(false);
    private static final String REGEX_ALWAYS_EAGER_HELP = "Always match capture groups eagerly.";
    @CompilationFinal private boolean regexAlwaysEager;

    public static final String SCRIPT_ENGINE_GLOBAL_SCOPE_IMPORT_NAME = JS_OPTION_PREFIX + "script-engine-global-scope-import";
    public static final OptionKey<Boolean> SCRIPT_ENGINE_GLOBAL_SCOPE_IMPORT = new OptionKey<>(false);
    private static final String SCRIPT_ENGINE_GLOBAL_SCOPE_IMPORT_HELP = "Enable ScriptEngine-specific global scope import function.";
    @CompilationFinal private boolean scriptEngineGlobalScopeImport;

    public static final String ARRAY_LIKE_PROTOTYPE_NAME = JS_OPTION_PREFIX + "experimental-array-prototype";
    public static final OptionKey<Boolean> ARRAY_LIKE_PROTOTYPE = new OptionKey<>(false);
    private static final String ARRAY_LIKE_PROTOTYPE_HELP = "Non-JS array-like objects (like ProxyArray or java.util.List) have prototype set to Array.prototype.";
    @CompilationFinal private boolean arrayLikePrototype;

    public static final String SIMDJS_NAME = JS_OPTION_PREFIX + "simdjs";
    private static final OptionKey<Boolean> SIMDJS = new OptionKey<>(false);
    private static final String SIMDJS_HELP = "Provide implementation of the SIMD.js proposal.";
    @CompilationFinal private boolean simdjs;

    public JSContextOptions(ParserOptions parserOptions) {
        this.parserOptions = parserOptions;
        cacheOptions();
    }

    public ParserOptions getParserOptions() {
        return parserOptions;
    }

    public void setParserOptions(ParserOptions parserOptions) {
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
        this.ecmascriptVersion = readIntegerOption(ECMASCRIPT_VERSION, ECMASCRIPT_VERSION_NAME);
        this.annexB = readBooleanOption(ANNEX_B, ANNEX_B_NAME);
        this.intl402 = readBooleanOption(INTL_402, INTL_402_NAME);
        this.regexpStaticResult = readBooleanOption(REGEXP_STATIC_RESULT, REGEXP_STATIC_RESULT_NAME);
        this.arraySortInherited = patchBooleanOption(ARRAY_SORT_INHERITED, ARRAY_SORT_INHERITED_NAME, arraySortInherited, arraySortInheritedAssumption);
        this.sharedArrayBuffer = readBooleanOption(SHARED_ARRAY_BUFFER, SHARED_ARRAY_BUFFER_NAME);
        this.atomics = readBooleanOption(ATOMICS, ATOMICS_NAME);
        this.v8CompatibilityMode = patchBooleanOption(V8_COMPATIBILITY_MODE, V8_COMPATIBILITY_MODE_NAME, v8CompatibilityMode, v8CompatibilityAssumption);
        this.v8RealmBuiltin = readBooleanOption(V8_REALM_BUILTIN, V8_REALM_BUILTIN_NAME);
        this.nashornCompatibilityMode = readBooleanOption(NASHORN_COMPATIBILITY_MODE, NASHORN_COMPATIBILITY_MODE_NAME);
        this.directByteBuffer = patchBooleanOption(DIRECT_BYTE_BUFFER, DIRECT_BYTE_BUFFER_NAME, directByteBuffer, directByteBufferAssumption);
        this.parseOnly = readBooleanOption(PARSE_ONLY, PARSE_ONLY_NAME);
        this.debug = readBooleanOption(DEBUG_BUILTIN, DEBUG_BUILTIN_NAME);
        this.timerResolution = patchLongOption(TIMER_RESOLUTION, TIMER_RESOLUTION_NAME, timerResolution, timerResolutionAssumption);
        this.agentCanBlock = readBooleanOption(AGENT_CAN_BLOCK, AGENT_CAN_BLOCK_NAME);
        this.awaitOptimization = readBooleanOption(AWAIT_OPTIMIZATION, AWAIT_OPTIMIZATION_NAME);
        this.disableEval = readBooleanOption(DISABLE_EVAL, DISABLE_EVAL_NAME);
        this.disableWith = readBooleanOption(DISABLE_WITH, DISABLE_WITH_NAME);
        this.regexDumpAutomata = readBooleanOption(REGEX_DUMP_AUTOMATA, REGEX_DUMP_AUTOMATA_NAME);
        this.regexStepExecution = readBooleanOption(REGEX_STEP_EXECUTION, REGEX_STEP_EXECUTION_NAME);
        this.regexAlwaysEager = readBooleanOption(REGEX_ALWAYS_EAGER, REGEX_ALWAYS_EAGER_NAME);
        this.scriptEngineGlobalScopeImport = readBooleanOption(SCRIPT_ENGINE_GLOBAL_SCOPE_IMPORT, SCRIPT_ENGINE_GLOBAL_SCOPE_IMPORT_NAME);
        this.arrayLikePrototype = readBooleanOption(ARRAY_LIKE_PROTOTYPE, ARRAY_LIKE_PROTOTYPE_NAME);
        this.simdjs = readBooleanOption(SIMDJS, SIMDJS_NAME);
    }

    private boolean patchBooleanOption(OptionKey<Boolean> key, String name, boolean oldValue, Assumption assumption) {
        boolean newValue = readBooleanOption(key, name);
        if (oldValue != newValue) {
            assumption.invalidate(String.format("Option {0} was changed from {1} to {2}.", name, oldValue, newValue));
        }
        return newValue;
    }

    private boolean readBooleanOption(OptionKey<Boolean> key, String name) {
        if (optionValues == null) {
            return readBooleanFromSystemProperty(key, name);
        } else {
            return key.getValue(optionValues);
        }
    }

    private static boolean readBooleanFromSystemProperty(OptionKey<Boolean> key, String name) {
        String sysProp = System.getProperty("polyglot." + name);
        if (sysProp != null) {
            return sysProp.equalsIgnoreCase("true");
        }
        return key.getDefaultValue();
    }

    private int readIntegerOption(OptionKey<Integer> key, String name) {
        if (optionValues == null) {
            return readIntegerFromSystemProperty(key, name);
        } else {
            return key.getValue(optionValues);
        }
    }

    private static int readIntegerFromSystemProperty(OptionKey<Integer> key, String name) {
        return Integer.getInteger("polyglot." + name, key.getDefaultValue());
    }

    private long patchLongOption(OptionKey<Long> key, String name, long oldValue, Assumption assumption) {
        long newValue = readLongOption(key, name);
        if (oldValue != newValue) {
            assumption.invalidate(String.format("Option {0} was changed from {1} to {2}.", name, oldValue, newValue));
        }
        return newValue;
    }

    private long readLongOption(OptionKey<Long> key, String name) {
        if (optionValues == null) {
            return readLongFromSystemProperty(key, name);
        } else {
            return key.getValue(optionValues);
        }
    }

    private static long readLongFromSystemProperty(OptionKey<Long> key, String name) {
        return Long.getLong("polyglot." + name, key.getDefaultValue());
    }

    public static String helpWithDefault(String helpMessage, OptionKey<? extends Object> key) {
        return helpMessage + " (default:" + key.getDefaultValue() + ")";
    }

    public static OptionDescriptor newOptionDescriptor(OptionKey<?> key, String name, OptionCategory category, String help) {
        return OptionDescriptor.newBuilder(key, name).category(category).help(helpWithDefault(help, key)).build();
    }

    public static void describeOptions(List<OptionDescriptor> options) {
        options.add(newOptionDescriptor(ECMASCRIPT_VERSION, ECMASCRIPT_VERSION_NAME, OptionCategory.USER, ECMASCRIPT_VERSION_HELP));
        options.add(newOptionDescriptor(ANNEX_B, ANNEX_B_NAME, OptionCategory.USER, ANNEX_B_HELP));
        options.add(newOptionDescriptor(INTL_402, INTL_402_NAME, OptionCategory.USER, INTL_402_HELP));
        options.add(newOptionDescriptor(REGEXP_STATIC_RESULT, REGEXP_STATIC_RESULT_NAME, OptionCategory.USER, REGEXP_STATIC_RESULT_HELP));
        options.add(newOptionDescriptor(ARRAY_SORT_INHERITED, ARRAY_SORT_INHERITED_NAME, OptionCategory.USER, ARRAY_SORT_INHERITED_HELP));
        options.add(newOptionDescriptor(SHARED_ARRAY_BUFFER, SHARED_ARRAY_BUFFER_NAME, OptionCategory.USER, SHARED_ARRAY_BUFFER_HELP));
        options.add(newOptionDescriptor(ATOMICS, ATOMICS_NAME, OptionCategory.USER, ATOMICS_HELP));
        options.add(newOptionDescriptor(V8_COMPATIBILITY_MODE, V8_COMPATIBILITY_MODE_NAME, OptionCategory.USER, V8_COMPATIBILITY_MODE_HELP));
        options.add(newOptionDescriptor(V8_REALM_BUILTIN, V8_REALM_BUILTIN_NAME, OptionCategory.DEBUG, V8_REALM_BUILTIN_HELP));
        options.add(newOptionDescriptor(NASHORN_COMPATIBILITY_MODE, NASHORN_COMPATIBILITY_MODE_NAME, OptionCategory.USER, NASHORN_COMPATIBILITY_MODE_HELP));
        options.add(newOptionDescriptor(STACK_TRACE_LIMIT, STACK_TRACE_LIMIT_NAME, OptionCategory.USER, STACK_TRACE_LIMIT_HELP));
        options.add(newOptionDescriptor(DEBUG_BUILTIN, DEBUG_BUILTIN_NAME, OptionCategory.DEBUG, DEBUG_BUILTIN_HELP));
        options.add(newOptionDescriptor(DIRECT_BYTE_BUFFER, DIRECT_BYTE_BUFFER_NAME, OptionCategory.USER, DIRECT_BYTE_BUFFER_HELP));
        options.add(newOptionDescriptor(PARSE_ONLY, PARSE_ONLY_NAME, OptionCategory.USER, PARSE_ONLY_HELP));
        options.add(newOptionDescriptor(TIME_ZONE, TIME_ZONE_NAME, OptionCategory.USER, TIME_ZONE_HELP));
        options.add(newOptionDescriptor(TIMER_RESOLUTION, TIMER_RESOLUTION_NAME, OptionCategory.USER, TIMER_RESOLUTION_HELP));
        options.add(newOptionDescriptor(AGENT_CAN_BLOCK, AGENT_CAN_BLOCK_NAME, OptionCategory.DEBUG, AGENT_CAN_BLOCK_HELP));
        options.add(newOptionDescriptor(JAVA_PACKAGE_GLOBALS, JAVA_PACKAGE_GLOBALS_NAME, OptionCategory.USER, JAVA_PACKAGE_GLOBALS_HELP));
        options.add(newOptionDescriptor(GLOBAL_PROPERTY, GLOBAL_PROPERTY_NAME, OptionCategory.USER, GLOBAL_PROPERTY_HELP));
        options.add(newOptionDescriptor(GLOBAL_PROPERTY, GLOBAL_THIS_NAME, OptionCategory.USER, GLOBAL_THIS_HELP));
        options.add(newOptionDescriptor(CONSOLE, CONSOLE_NAME, OptionCategory.USER, CONSOLE_HELP));
        options.add(newOptionDescriptor(PERFORMANCE, PERFORMANCE_NAME, OptionCategory.USER, PERFORMANCE_HELP));
        options.add(newOptionDescriptor(SHELL, SHELL_NAME, OptionCategory.USER, SHELL_HELP));
        options.add(newOptionDescriptor(PRINT, PRINT_NAME, OptionCategory.USER, PRINT_HELP));
        options.add(newOptionDescriptor(LOAD, LOAD_NAME, OptionCategory.USER, LOAD_HELP));
        options.add(newOptionDescriptor(GRAAL_BUILTIN, GRAAL_BUILTIN_NAME, OptionCategory.USER, GRAAL_BUILTIN_HELP));
        options.add(newOptionDescriptor(POLYGLOT_BUILTIN, POLYGLOT_BUILTIN_NAME, OptionCategory.USER, POLYGLOT_BUILTIN_HELP));
        options.add(newOptionDescriptor(AWAIT_OPTIMIZATION, AWAIT_OPTIMIZATION_NAME, OptionCategory.DEBUG, AWAIT_OPTIMIZATION_HELP));
        options.add(newOptionDescriptor(DISABLE_EVAL, DISABLE_EVAL_NAME, OptionCategory.EXPERT, DISABLE_EVAL_HELP));
        options.add(newOptionDescriptor(DISABLE_WITH, DISABLE_WITH_NAME, OptionCategory.EXPERT, DISABLE_WITH_HELP));
        options.add(newOptionDescriptor(REGEX_DUMP_AUTOMATA, REGEX_DUMP_AUTOMATA_NAME, OptionCategory.DEBUG, REGEX_DUMP_AUTOMATA_HELP));
        options.add(newOptionDescriptor(REGEX_STEP_EXECUTION, REGEX_STEP_EXECUTION_NAME, OptionCategory.DEBUG, REGEX_STEP_EXECUTION_HELP));
        options.add(newOptionDescriptor(REGEX_ALWAYS_EAGER, REGEX_ALWAYS_EAGER_NAME, OptionCategory.DEBUG, REGEX_ALWAYS_EAGER_HELP));
        options.add(newOptionDescriptor(SCRIPT_ENGINE_GLOBAL_SCOPE_IMPORT, SCRIPT_ENGINE_GLOBAL_SCOPE_IMPORT_NAME, OptionCategory.EXPERT, SCRIPT_ENGINE_GLOBAL_SCOPE_IMPORT_HELP));
        options.add(newOptionDescriptor(ARRAY_LIKE_PROTOTYPE, ARRAY_LIKE_PROTOTYPE_NAME, OptionCategory.EXPERT, ARRAY_LIKE_PROTOTYPE_HELP));
        options.add(newOptionDescriptor(SIMDJS, SIMDJS_NAME, OptionCategory.EXPERT, SIMDJS_HELP));
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
            arraySortInheritedAssumption.check();
        } catch (InvalidAssumptionException e) {
            arraySortInheritedAssumption = Truffle.getRuntime().createAssumption(arraySortInheritedAssumption.getName());
            CompilerDirectives.transferToInterpreterAndInvalidate();
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
        return atomics;
    }

    public boolean isV8CompatibilityMode() {
        try {
            v8CompatibilityAssumption.check();
        } catch (InvalidAssumptionException e) {
            v8CompatibilityAssumption = Truffle.getRuntime().createAssumption(v8CompatibilityAssumption.getName());
            CompilerDirectives.transferToInterpreterAndInvalidate();
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
            directByteBufferAssumption.check();
        } catch (InvalidAssumptionException e) {
            directByteBufferAssumption = Truffle.getRuntime().createAssumption(directByteBufferAssumption.getName());
            CompilerDirectives.transferToInterpreterAndInvalidate();
        }
        return directByteBuffer;
    }

    public boolean isParseOnly() {
        return parseOnly;
    }

    public long getTimerResolution() {
        try {
            timerResolutionAssumption.check();
        } catch (InvalidAssumptionException e) {
            timerResolutionAssumption = Truffle.getRuntime().createAssumption(timerResolutionAssumption.getName());
            CompilerDirectives.transferToInterpreterAndInvalidate();
        }
        return timerResolution;
    }

    public boolean isV8RealmBuiltin() {
        return v8RealmBuiltin;
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

    public boolean isArrayLikePrototype() {
        return arrayLikePrototype;
    }

    public boolean isGlobalProperty() {
        CompilerAsserts.neverPartOfCompilation("Context patchable option global-property was assumed not to be accessed in compiled code.");
        return GLOBAL_PROPERTY.getValue(optionValues);
    }

    public boolean isConsole() {
        CompilerAsserts.neverPartOfCompilation("Context patchable option console was assumed not to be accessed in compiled code.");
        return CONSOLE.getValue(optionValues);
    }

    public boolean isPrint() {
        return PRINT.getValue(optionValues);
    }

    public boolean isLoad() {
        CompilerAsserts.neverPartOfCompilation("Context patchable option load was assumed not to be accessed in compiled code.");
        return LOAD.getValue(optionValues);
    }

    public boolean isPerformance() {
        return PERFORMANCE.getValue(optionValues);
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

    public boolean isSIMDjs() {
        return SIMDJS.getValue(optionValues);
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
        hash = 53 * hash + (this.atomics ? 1 : 0);
        hash = 53 * hash + (this.v8CompatibilityMode ? 1 : 0);
        hash = 53 * hash + (this.v8RealmBuiltin ? 1 : 0);
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
        hash = 53 * hash + (this.arrayLikePrototype ? 1 : 0);
        hash = 53 * hash + (this.simdjs ? 1 : 0);
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
        if (this.atomics != other.atomics) {
            return false;
        }
        if (this.v8CompatibilityMode != other.v8CompatibilityMode) {
            return false;
        }
        if (this.v8RealmBuiltin != other.v8RealmBuiltin) {
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
        if (this.arrayLikePrototype != other.arrayLikePrototype) {
            return false;
        }
        if (this.simdjs != other.simdjs) {
            return false;
        }
        return Objects.equals(this.parserOptions, other.parserOptions);
    }

}
