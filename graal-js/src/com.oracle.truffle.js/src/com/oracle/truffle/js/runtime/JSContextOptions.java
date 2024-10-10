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

import java.nio.charset.Charset;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.graalvm.options.OptionCategory;
import org.graalvm.options.OptionDescriptor;
import org.graalvm.options.OptionDescriptors;
import org.graalvm.options.OptionKey;
import org.graalvm.options.OptionStability;
import org.graalvm.options.OptionType;
import org.graalvm.options.OptionValues;
import org.graalvm.polyglot.SandboxPolicy;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.Option;
import com.oracle.truffle.api.TruffleOptionDescriptors;
import com.oracle.truffle.js.lang.JavaScriptLanguage;

/**
 * Defines, and provides access to, all JS (context and language) options.
 *
 * Option values are per-context, and cached values may be constant-folded when running with a bound
 * Engine, as long as no other contexts have been spawned, but have to be reread every time from the
 * context in "multi-context mode", i.e., if code sharing is enabled via a shared Engine.
 *
 * {@link JSLanguageOptions} captures a subset of these options that are immutable and shared per
 * language instance (polyglot sharing layer) and always treated as constant in compiled code, but
 * will prevent code sharing between contexts that differ in these options.
 *
 * {@link JSParserOptions} captures the subset of both {@link JSContextOptions} and
 * {@link JSLanguageOptions} that is used by the parser.
 *
 * Select context options are treated as stable and/or patchable in the preinitialized context.
 * Stable options will be read from the language and treated as constant while stable but once the
 * option's value has changed, it has to be looked up from the context. This gives fast access by
 * default without preventing code sharing. Similarly, patchable options allow code and the
 * preinitialized context to be reused even if the option changes.
 *
 * @see JSLanguageOptions
 * @see JSParserOptions
 * @see com.oracle.truffle.js.runtime.util.StableContextOptionValue
 */
public final class JSContextOptions {

    public static final String JS_OPTION_PREFIX = JavaScriptLanguage.ID + ".";

    @CompilationFinal private JSParserOptions parserOptions;
    @CompilationFinal private OptionValues optionValues;

    public static final String ECMASCRIPT_VERSION_LATEST = "latest";
    public static final String ECMASCRIPT_VERSION_STAGING = "staging";
    public static final String ECMASCRIPT_VERSION_NAME = JS_OPTION_PREFIX + "ecmascript-version";
    @Option(name = ECMASCRIPT_VERSION_NAME, category = OptionCategory.USER, stability = OptionStability.STABLE, sandbox = SandboxPolicy.UNTRUSTED, //
                    usageSyntax = ECMASCRIPT_VERSION_LATEST + "|" + ECMASCRIPT_VERSION_STAGING + "|" +
                                    "[5, " + JSConfig.LatestECMAScriptVersion + "]|" +
                                    "[2015, " + (JSConfig.LatestECMAScriptVersion + JSConfig.ECMAScriptVersionYearDelta) + "]", //
                    help = "ECMAScript version to be compatible with. Default is '" + ECMASCRIPT_VERSION_LATEST + "' (latest supported version), staged features are in '" +
                                    ECMASCRIPT_VERSION_STAGING + "'.") //
    public static final OptionKey<Integer> ECMASCRIPT_VERSION = new OptionKey<>(JSConfig.LatestECMAScriptVersion, new OptionType<>("ecmascript-version", new Function<String, Integer>() {

        @Override
        public Integer apply(String in) {
            if (ECMASCRIPT_VERSION_LATEST.equals(in)) {
                return JSConfig.LatestECMAScriptVersion;
            } else if (ECMASCRIPT_VERSION_STAGING.equals(in)) {
                return JSConfig.StagingECMAScriptVersion;
            }
            try {
                final int minVersion = 5;
                final int maxVersion = JSConfig.LatestECMAScriptVersion;
                final int minYearVersion = JSConfig.ECMAScript6 + JSConfig.ECMAScriptVersionYearDelta;
                final int maxYearVersion = maxVersion + JSConfig.ECMAScriptVersionYearDelta;
                int version = Integer.parseInt(in);
                if (minYearVersion <= version && version <= maxYearVersion) {
                    version -= JSConfig.ECMAScriptVersionYearDelta;
                }
                if (version < minVersion || version > maxVersion) {
                    throw new IllegalArgumentException("Supported values are " + minVersion + " to " + maxVersion + " or " + minYearVersion + " to " + maxYearVersion + ".");
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
    public static final OptionKey<Boolean> ANNEX_B = new OptionKey<>(JSConfig.AnnexB);
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
    @Option(name = STRICT_NAME, category = OptionCategory.USER, stability = OptionStability.STABLE, sandbox = SandboxPolicy.UNTRUSTED, help = "Enforce strict mode.") //
    public static final OptionKey<Boolean> STRICT = new OptionKey<>(false);

    public static final String CONST_AS_VAR_NAME = JS_OPTION_PREFIX + "const-as-var";
    @Option(name = CONST_AS_VAR_NAME, category = OptionCategory.EXPERT, help = "Parse const declarations as a var (legacy compatibility option).") //
    public static final OptionKey<Boolean> CONST_AS_VAR = new OptionKey<>(false);

    public static final String FUNCTION_STATEMENT_ERROR_NAME = JS_OPTION_PREFIX + "function-statement-error";
    @Option(name = FUNCTION_STATEMENT_ERROR_NAME, category = OptionCategory.EXPERT, help = "Treat hoistable function statements in blocks as an error (in ES5 mode).") //
    public static final OptionKey<Boolean> FUNCTION_STATEMENT_ERROR = new OptionKey<>(false);

    public static final String INTL_402_NAME = JS_OPTION_PREFIX + "intl-402";
    @Option(name = INTL_402_NAME, category = OptionCategory.USER, stability = OptionStability.STABLE, help = "Enable ECMAScript Internationalization API.") //
    public static final OptionKey<Boolean> INTL_402 = new OptionKey<>(true);
    @CompilationFinal private boolean intl402;

    public static final String REGEXP_MATCH_INDICES_NAME = JS_OPTION_PREFIX + "regexp-match-indices";
    @Option(name = REGEXP_MATCH_INDICES_NAME, category = OptionCategory.USER, help = "Enable RegExp Match Indices property.", deprecated = true) //
    public static final OptionKey<Boolean> REGEXP_MATCH_INDICES = new OptionKey<>(false);
    @CompilationFinal private boolean regexpMatchIndices;

    public static final String REGEXP_UNICODE_SETS_NAME = JS_OPTION_PREFIX + "regexp-unicode-sets";
    @Option(name = REGEXP_UNICODE_SETS_NAME, category = OptionCategory.USER, help = "Enable RegExp Unicode sets proposal (v flag).") //
    public static final OptionKey<Boolean> REGEXP_UNICODE_SETS = new OptionKey<>(false);
    @CompilationFinal private boolean regexpUnicodeSets;

    public static final String REGEXP_STATIC_RESULT_NAME = JS_OPTION_PREFIX + "regexp-static-result";
    @Option(name = REGEXP_STATIC_RESULT_NAME, category = OptionCategory.USER, help = "Provide last RegExp match in RegExp global var, e.g. RegExp.$1.") //
    public static final OptionKey<Boolean> REGEXP_STATIC_RESULT = new OptionKey<>(true);
    @CompilationFinal private boolean regexpStaticResult;

    public static final String SHARED_ARRAY_BUFFER_NAME = JS_OPTION_PREFIX + "shared-array-buffer";
    @Option(name = SHARED_ARRAY_BUFFER_NAME, category = OptionCategory.USER, help = "Enable ECMAScript SharedArrayBuffer.") //
    public static final OptionKey<Boolean> SHARED_ARRAY_BUFFER = new OptionKey<>(true);
    @CompilationFinal private boolean sharedArrayBuffer;

    public static final String ATOMICS_NAME = JS_OPTION_PREFIX + "atomics";
    @Option(name = ATOMICS_NAME, category = OptionCategory.USER, help = "Enable ECMAScript Atomics.") //
    public static final OptionKey<Boolean> ATOMICS = new OptionKey<>(true);

    public static final String V8_COMPATIBILITY_MODE_NAME = JS_OPTION_PREFIX + "v8-compat";
    @Option(name = V8_COMPATIBILITY_MODE_NAME, category = OptionCategory.USER, help = "Provide compatibility with the Google V8 engine.") //
    public static final OptionKey<Boolean> V8_COMPATIBILITY_MODE = new OptionKey<>(false);
    @CompilationFinal private boolean v8CompatibilityMode;

    public static final String V8_REALM_BUILTIN_NAME = JS_OPTION_PREFIX + "v8-realm-builtin";
    @Option(name = V8_REALM_BUILTIN_NAME, category = OptionCategory.INTERNAL, help = "Provide Realm builtin compatible with V8's d8 shell.") //
    public static final OptionKey<Boolean> V8_REALM_BUILTIN = new OptionKey<>(false);
    @CompilationFinal private boolean v8RealmBuiltin;

    public static final String NASHORN_COMPATIBILITY_MODE_NAME = JS_OPTION_PREFIX + "nashorn-compat";
    @Option(name = NASHORN_COMPATIBILITY_MODE_NAME, category = OptionCategory.USER, help = "Provide compatibility with the OpenJDK Nashorn engine. Do not use with untrusted code.") //
    public static final OptionKey<Boolean> NASHORN_COMPATIBILITY_MODE = new OptionKey<>(false);
    @CompilationFinal private boolean nashornCompatibilityMode;

    public static final String STACK_TRACE_LIMIT_NAME = JS_OPTION_PREFIX + "stack-trace-limit";
    @Option(name = STACK_TRACE_LIMIT_NAME, category = OptionCategory.USER, usageSyntax = "[0, inf)", help = "Number of stack frames to capture.") //
    public static final OptionKey<Integer> STACK_TRACE_LIMIT = new OptionKey<>(JSConfig.StackTraceLimit);
    @CompilationFinal private int stackTraceLimit;

    public static final String DEBUG_BUILTIN_NAME = JS_OPTION_PREFIX + "debug-builtin";
    @Option(name = DEBUG_BUILTIN_NAME, category = OptionCategory.INTERNAL, help = "Provide a non-API Debug builtin. Behaviour will likely change. Don't depend on this in production code.") //
    public static final OptionKey<Boolean> DEBUG_BUILTIN = new OptionKey<>(false);
    @CompilationFinal private boolean debug;

    public static final String DIRECT_BYTE_BUFFER_NAME = JS_OPTION_PREFIX + "direct-byte-buffer";
    @Option(name = DIRECT_BYTE_BUFFER_NAME, category = OptionCategory.USER, help = "Use direct (off-heap) byte buffer for typed arrays.") //
    public static final OptionKey<Boolean> DIRECT_BYTE_BUFFER = new OptionKey<>(false);
    @CompilationFinal private boolean directByteBuffer;

    public static final String PARSE_ONLY_NAME = JS_OPTION_PREFIX + "parse-only";
    @Option(name = PARSE_ONLY_NAME, category = OptionCategory.INTERNAL, help = "Only parse source code, do not run it.") //
    public static final OptionKey<Boolean> PARSE_ONLY = new OptionKey<>(false);
    @CompilationFinal private boolean parseOnly;

    public static final String TIME_ZONE_NAME = JS_OPTION_PREFIX + "timezone";
    @Option(name = TIME_ZONE_NAME, category = OptionCategory.USER, usageSyntax = "<TimeZoneID>", help = "Set custom time zone ID.") //
    public static final OptionKey<String> TIME_ZONE = new OptionKey<>("", new OptionType<>("ZoneId", new Function<String, String>() {
        @Override
        public String apply(String tz) {
            if (tz.isEmpty()) {
                return "";
            }
            // Validate the time zone ID and convert legacy short IDs to long IDs.
            try {
                return ZoneId.of(tz, ZoneId.SHORT_IDS).getId();
            } catch (DateTimeException e) {
                throw new IllegalArgumentException(e);
            }
        }
    }));

    public static final String ZONE_RULES_BASED_TIME_ZONES_NAME = JS_OPTION_PREFIX + "zone-rules-based-time-zones";
    @Option(name = ZONE_RULES_BASED_TIME_ZONES_NAME, category = OptionCategory.EXPERT, help = "Use ZoneRulesProvider instead of time-zone data from ICU4J.") //
    public static final OptionKey<Boolean> ZONE_RULES_BASED_TIME_ZONES = new OptionKey<>(false);
    @CompilationFinal private boolean zoneRulesBasedTimeZones;

    public static final String TIMER_RESOLUTION_NAME = JS_OPTION_PREFIX + "timer-resolution";
    @Option(name = TIMER_RESOLUTION_NAME, category = OptionCategory.USER, stability = OptionStability.STABLE, sandbox = SandboxPolicy.UNTRUSTED, usageSyntax = "<nanoseconds>", //
                    help = "Resolution of timers (performance.now() and Date built-ins) in nanoseconds. Fuzzy time is used when set to 0.") //
    public static final OptionKey<Long> TIMER_RESOLUTION = new OptionKey<>(1000000L);
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

    public static final String GLOBAL_ARGUMENTS_NAME = JS_OPTION_PREFIX + "global-arguments";
    @Option(name = GLOBAL_ARGUMENTS_NAME, category = OptionCategory.USER, help = "Provide 'arguments' global property.") //
    public static final OptionKey<Boolean> GLOBAL_ARGUMENTS = new OptionKey<>(true);

    public static final String CONSOLE_NAME = JS_OPTION_PREFIX + "console";
    @Option(name = CONSOLE_NAME, category = OptionCategory.USER, stability = OptionStability.STABLE, sandbox = SandboxPolicy.UNTRUSTED, help = "Provide 'console' global property.") //
    public static final OptionKey<Boolean> CONSOLE = new OptionKey<>(true);

    public static final String PERFORMANCE_NAME = JS_OPTION_PREFIX + "performance";
    @Option(name = PERFORMANCE_NAME, category = OptionCategory.USER, help = "Provide 'performance' global property.") //
    public static final OptionKey<Boolean> PERFORMANCE = new OptionKey<>(false);

    public static final String SHELL_NAME = JS_OPTION_PREFIX + "shell";
    @Option(name = SHELL_NAME, category = OptionCategory.USER, help = "Provide global functions for js shell.") //
    public static final OptionKey<Boolean> SHELL = new OptionKey<>(false);

    public static final String PRINT_NAME = JS_OPTION_PREFIX + "print";
    @Option(name = PRINT_NAME, category = OptionCategory.USER, stability = OptionStability.STABLE, sandbox = SandboxPolicy.UNTRUSTED, help = "Provide 'print' global function.") //
    public static final OptionKey<Boolean> PRINT = new OptionKey<>(true);

    public static final String PRINT_NO_NEWLINE_NAME = JS_OPTION_PREFIX + "print-no-newline";
    @Option(name = PRINT_NO_NEWLINE_NAME, category = OptionCategory.USER, help = "Print function will not print new line char.") //
    public static final OptionKey<Boolean> PRINT_NO_NEWLINE = new OptionKey<>(false);
    @CompilationFinal private boolean printNoNewline;

    public static final String LOAD_NAME = JS_OPTION_PREFIX + "load";
    @Option(name = LOAD_NAME, category = OptionCategory.USER, stability = OptionStability.STABLE, sandbox = SandboxPolicy.UNTRUSTED, help = "Provide 'load' global function.") //
    public static final OptionKey<Boolean> LOAD = new OptionKey<>(true);

    public static final String LOAD_FROM_URL_NAME = JS_OPTION_PREFIX + "load-from-url";
    @Option(name = LOAD_FROM_URL_NAME, category = OptionCategory.USER, help = "Allow 'load' to access URLs. Do not use with untrusted code.") //
    public static final OptionKey<Boolean> LOAD_FROM_URL = new OptionKey<>(false);

    public static final String LOAD_FROM_CLASSPATH_NAME = JS_OPTION_PREFIX + "load-from-classpath";
    @Option(name = LOAD_FROM_CLASSPATH_NAME, category = OptionCategory.USER, help = "Allow 'load' to access 'classpath:' URLs. Do not use with untrusted code.") //
    public static final OptionKey<Boolean> LOAD_FROM_CLASSPATH = new OptionKey<>(false);

    public static final String COMMONJS_REQUIRE_NAME = JS_OPTION_PREFIX + "commonjs-require";
    @Option(name = COMMONJS_REQUIRE_NAME, category = OptionCategory.USER, help = "Enable CommonJS require emulation.") //
    public static final OptionKey<Boolean> COMMONJS_REQUIRE = new OptionKey<>(false);
    @CompilationFinal private boolean commonJSRequire;

    public static final String COMMONJS_REQUIRE_CWD_NAME = JS_OPTION_PREFIX + "commonjs-require-cwd";
    @Option(name = COMMONJS_REQUIRE_CWD_NAME, category = OptionCategory.USER, usageSyntax = "<path>", help = "CommonJS default current working directory.") //
    public static final OptionKey<String> COMMONJS_REQUIRE_CWD = new OptionKey<>("");

    public static final String COMMONJS_CORE_MODULES_REPLACEMENTS_NAME = JS_OPTION_PREFIX + "commonjs-core-modules-replacements";
    @Option(name = COMMONJS_CORE_MODULES_REPLACEMENTS_NAME, category = OptionCategory.USER, usageSyntax = "<name>:<module>,...", help = "Npm packages used to replace global Node.js builtins.") //
    public static final OptionKey<Map<String, String>> COMMONJS_CORE_MODULES_REPLACEMENTS = new OptionKey<>(Collections.emptyMap(),
                    new OptionType<>("commonjs-require-globals", new Function<String, Map<String, String>>() {
                        @Override
                        public Map<String, String> apply(String value) {
                            if (value.isEmpty()) {
                                return Collections.emptyMap();
                            }
                            Map<String, String> map = new HashMap<>();
                            String[] options = value.split(",");
                            for (String s : options) {
                                String[] builtin = s.split(":", 2);
                                if (builtin.length == 2) {
                                    String key = builtin[0];
                                    String val = builtin[1];
                                    if (!key.isEmpty() && !val.isEmpty()) {
                                        map.put(key, val);
                                        continue;
                                    }
                                }
                                throw new IllegalArgumentException("Unexpected builtin arguments: " + s);
                            }
                            return map;
                        }
                    }));

    public static final String GRAAL_BUILTIN_NAME = JS_OPTION_PREFIX + "graal-builtin";
    @Option(name = GRAAL_BUILTIN_NAME, category = OptionCategory.USER, stability = OptionStability.STABLE, sandbox = SandboxPolicy.UNTRUSTED, help = "Provide 'Graal' global property.") //
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
    @Option(name = DISABLE_EVAL_NAME, category = OptionCategory.EXPERT, deprecated = true, deprecationMessage = "Use js.allow-eval=false instead.", help = "Disallow code generation from strings, e.g. using eval().") //
    public static final OptionKey<Boolean> DISABLE_EVAL = new OptionKey<>(false);

    public static final String ALLOW_EVAL_NAME = JS_OPTION_PREFIX + "allow-eval";
    @Option(name = ALLOW_EVAL_NAME, category = OptionCategory.EXPERT, stability = OptionStability.STABLE, sandbox = SandboxPolicy.UNTRUSTED, help = "Allow or disallow code generation from strings, e.g. using eval().") //
    public static final OptionKey<Boolean> ALLOW_EVAL = new OptionKey<>(true);
    @CompilationFinal private boolean allowEval;

    public static final String DISABLE_WITH_NAME = JS_OPTION_PREFIX + "disable-with";
    @Option(name = DISABLE_WITH_NAME, category = OptionCategory.EXPERT, help = "User code is not allowed to use the 'with' statement.") //
    public static final OptionKey<Boolean> DISABLE_WITH = new OptionKey<>(false);
    @CompilationFinal private boolean disableWith;

    public static final String BIGINT_NAME = JS_OPTION_PREFIX + "bigint";
    @Option(name = BIGINT_NAME, category = OptionCategory.USER, help = "Provide an implementation of the BigInt proposal.") //
    public static final OptionKey<Boolean> BIGINT = new OptionKey<>(true);

    public static final String CLASS_FIELDS_NAME = JS_OPTION_PREFIX + "class-fields";
    @Option(name = CLASS_FIELDS_NAME, category = OptionCategory.USER, help = "Enable the class public and private fields proposal.") //
    public static final OptionKey<Boolean> CLASS_FIELDS = new OptionKey<>(false);
    public static final int CLASS_FIELDS_ES_VERSION = JSConfig.ECMAScript2021;

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

    public static final String FOREIGN_OBJECT_PROTOTYPE_NAME = JS_OPTION_PREFIX + "foreign-object-prototype";
    @Option(name = FOREIGN_OBJECT_PROTOTYPE_NAME, category = OptionCategory.EXPERT, stability = OptionStability.STABLE, help = "Non-JS objects have prototype (Object/Function/Array.prototype) set.") //
    public static final OptionKey<Boolean> FOREIGN_OBJECT_PROTOTYPE = new OptionKey<>(true);
    @CompilationFinal private boolean hasForeignObjectPrototype;

    public static final String FOREIGN_HASH_PROPERTIES_NAME = JS_OPTION_PREFIX + "foreign-hash-properties";
    @Option(name = FOREIGN_HASH_PROPERTIES_NAME, category = OptionCategory.EXPERT, help = "Allow getting/setting non-JS hash entries using the `[]` and `.` operators.") //
    public static final OptionKey<Boolean> FOREIGN_HASH_PROPERTIES = new OptionKey<>(true);
    @CompilationFinal private boolean hasForeignHashProperties;

    // limit originally from TestV8 regress-1122.js, regress-605470.js
    public static final String FUNCTION_ARGUMENTS_LIMIT_NAME = JS_OPTION_PREFIX + "function-arguments-limit";
    @Option(name = FUNCTION_ARGUMENTS_LIMIT_NAME, category = OptionCategory.EXPERT, usageSyntax = "<int>", help = "Maximum number of arguments for functions.") //
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
    @Option(name = LOCALE_NAME, category = OptionCategory.EXPERT, usageSyntax = "<locale>", help = "Use a specific default locale for locale-sensitive operations.") //
    public static final OptionKey<String> LOCALE = new OptionKey<>("");

    public static final String FUNCTION_CONSTRUCTOR_CACHE_SIZE_NAME = JS_OPTION_PREFIX + "function-constructor-cache-size";
    @Option(name = FUNCTION_CONSTRUCTOR_CACHE_SIZE_NAME, category = OptionCategory.EXPERT, usageSyntax = "<int>", help = "Maximum size of the parsing cache used by the Function constructor to avoid re-parsing known sources.") //
    public static final OptionKey<Integer> FUNCTION_CONSTRUCTOR_CACHE_SIZE = new OptionKey<>(256);
    @CompilationFinal private int functionConstructorCacheSize;

    public static final String REGEX_CACHE_SIZE_NAME = JS_OPTION_PREFIX + "regex-cache-size";
    @Option(name = REGEX_CACHE_SIZE_NAME, category = OptionCategory.EXPERT, usageSyntax = "<int>", help = "Maximum size of the regex cache used by the RegExp constructor to avoid re-parsing known sources.") //
    public static final OptionKey<Integer> REGEX_CACHE_SIZE = new OptionKey<>(128);
    @CompilationFinal private int regexCacheSize;

    public static final String STRING_LENGTH_LIMIT_NAME = JS_OPTION_PREFIX + "string-length-limit";
    @Option(name = STRING_LENGTH_LIMIT_NAME, category = OptionCategory.EXPERT, usageSyntax = "<chars>", help = "Maximum string length.") //
    public static final OptionKey<Integer> STRING_LENGTH_LIMIT = new OptionKey<>(JSConfig.StringLengthLimit);
    @CompilationFinal private int stringLengthLimit;

    public static final String STRING_LAZY_SUBSTRINGS_NAME = JS_OPTION_PREFIX + "string-lazy-substrings";
    @Option(name = STRING_LAZY_SUBSTRINGS_NAME, category = OptionCategory.EXPERT, help = "Allow lazy substrings.") //
    public static final OptionKey<Boolean> STRING_LAZY_SUBSTRINGS = new OptionKey<>(true);
    @CompilationFinal private boolean stringLazySubstrings;

    public static final String BIND_MEMBER_FUNCTIONS_NAME = JS_OPTION_PREFIX + "bind-member-functions";
    @Option(name = BIND_MEMBER_FUNCTIONS_NAME, category = OptionCategory.EXPERT, help = "Bind functions returned by Value.getMember to the receiver object.") //
    public static final OptionKey<Boolean> BIND_MEMBER_FUNCTIONS = new OptionKey<>(true);
    @CompilationFinal private boolean bindMemberFunctions;

    public static final String REGEX_REGRESSION_TEST_MODE_NAME = JS_OPTION_PREFIX + "regex-regression-test-mode";
    @Option(name = REGEX_REGRESSION_TEST_MODE_NAME, category = OptionCategory.INTERNAL, help = "Test mode for TRegex.") //
    public static final OptionKey<Boolean> REGEX_REGRESSION_TEST_MODE = new OptionKey<>(false);
    @CompilationFinal private boolean regexRegressionTestMode;

    public static final String INTEROP_COMPLETE_PROMISES_NAME = JS_OPTION_PREFIX + "interop-complete-promises";
    @Option(name = INTEROP_COMPLETE_PROMISES_NAME, category = OptionCategory.EXPERT, help = "Resolve promises when crossing a polyglot language boundary.") //
    public static final OptionKey<Boolean> INTEROP_COMPLETE_PROMISES = new OptionKey<>(false);

    public static final String DEBUG_PROPERTY_NAME_NAME = JS_OPTION_PREFIX + "debug-property-name";
    @Option(name = DEBUG_PROPERTY_NAME_NAME, category = OptionCategory.EXPERT, usageSyntax = "<name>", help = "The name used for the Graal.js debug builtin.") //
    public static final OptionKey<String> DEBUG_PROPERTY_NAME = new OptionKey<>(Strings.toJavaString(JSRealm.DEBUG_CLASS_NAME));

    public static final String PROFILE_TIME_NAME = JS_OPTION_PREFIX + "profile-time";
    @Option(name = PROFILE_TIME_NAME, category = OptionCategory.INTERNAL, help = "Enable time profiling.") //
    public static final OptionKey<Boolean> PROFILE_TIME = new OptionKey<>(false);

    public static final String PROFILE_TIME_PRINT_CUMULATIVE_NAME = JS_OPTION_PREFIX + "profile-time-print-cumulative";
    @Option(name = PROFILE_TIME_PRINT_CUMULATIVE_NAME, category = OptionCategory.INTERNAL, help = "Print cumulative time when time profiling is enabled.") //
    public static final OptionKey<Boolean> PROFILE_TIME_PRINT_CUMULATIVE = new OptionKey<>(false);

    public static final String TEST_CLONE_UNINITIALIZED_NAME = JS_OPTION_PREFIX + "test-clone-uninitialized";
    @Option(name = TEST_CLONE_UNINITIALIZED_NAME, category = OptionCategory.INTERNAL, help = "Test uninitialized cloning.") //
    public static final OptionKey<Boolean> TEST_CLONE_UNINITIALIZED = new OptionKey<>(false);
    @CompilationFinal private boolean testCloneUninitialized;

    public static final String LAZY_TRANSLATION_NAME = JS_OPTION_PREFIX + "lazy-translation";
    @Option(name = LAZY_TRANSLATION_NAME, category = OptionCategory.INTERNAL, help = "Translate function bodies lazily.") //
    public static final OptionKey<Boolean> LAZY_TRANSLATION = new OptionKey<>(false);
    @CompilationFinal private boolean lazyTranslation;

    public static final String MAX_TYPED_ARRAY_LENGTH_NAME = JS_OPTION_PREFIX + "max-typed-array-length";
    @Option(name = MAX_TYPED_ARRAY_LENGTH_NAME, category = OptionCategory.EXPERT, usageSyntax = "<int>", help = "Maximum allowed length for TypedArrays.") //
    public static final OptionKey<Integer> MAX_TYPED_ARRAY_LENGTH = new OptionKey<>(JSConfig.MaxTypedArrayLength);
    @CompilationFinal private int maxTypedArrayLength;

    public static final String MAX_APPLY_ARGUMENT_LENGTH_NAME = JS_OPTION_PREFIX + "max-apply-argument-length";
    @Option(name = MAX_APPLY_ARGUMENT_LENGTH_NAME, category = OptionCategory.EXPERT, usageSyntax = "<int>", help = "Maximum allowed number of arguments allowed in an apply function.") //
    public static final OptionKey<Integer> MAX_APPLY_ARGUMENT_LENGTH = new OptionKey<>(JSConfig.MaxApplyArgumentLength);
    @CompilationFinal private int maxApplyArgumentLength;

    public static final String MAX_PROTOTYPE_CHAIN_LENGTH_NAME = JS_OPTION_PREFIX + "max-prototype-chain-length";
    @Option(name = MAX_PROTOTYPE_CHAIN_LENGTH_NAME, category = OptionCategory.EXPERT, usageSyntax = "<int>", help = "Maximum allowed length of a prototype chain.") //
    public static final OptionKey<Integer> MAX_PROTOTYPE_CHAIN_LENGTH = new OptionKey<>(JSConfig.MaxPrototypeChainLength);
    @CompilationFinal private int maxPrototypeChainLength;

    public static final String ASYNC_STACK_TRACES_NAME = JS_OPTION_PREFIX + "async-stack-traces";
    @Option(name = ASYNC_STACK_TRACES_NAME, category = OptionCategory.EXPERT, help = "Include async function frames in stack traces.") //
    public static final OptionKey<Boolean> ASYNC_STACK_TRACES = new OptionKey<>(true);
    @CompilationFinal private boolean asyncStackTraces;

    public static final String PROPERTY_CACHE_LIMIT_NAME = JS_OPTION_PREFIX + "property-cache-limit";
    @Option(name = PROPERTY_CACHE_LIMIT_NAME, category = OptionCategory.INTERNAL, usageSyntax = "<int>", help = "Maximum allowed size of a property cache.") //
    public static final OptionKey<Integer> PROPERTY_CACHE_LIMIT = new OptionKey<>(JSConfig.PropertyCacheLimit);
    @CompilationFinal private int propertyCacheLimit;

    public static final String FUNCTION_CACHE_LIMIT_NAME = JS_OPTION_PREFIX + "function-cache-limit";
    @Option(name = FUNCTION_CACHE_LIMIT_NAME, category = OptionCategory.INTERNAL, usageSyntax = "<int>", help = "Maximum allowed size of a function cache.") //
    public static final OptionKey<Integer> FUNCTION_CACHE_LIMIT = new OptionKey<>(JSConfig.FunctionCacheLimit);
    @CompilationFinal private int functionCacheLimit;

    public static final String TOP_LEVEL_AWAIT_NAME = JS_OPTION_PREFIX + "top-level-await";
    @Option(name = TOP_LEVEL_AWAIT_NAME, category = OptionCategory.EXPERT, help = "Enable top-level-await.")
    // defaulting to ecmascript-version>=2022
    protected static final OptionKey<Boolean> TOP_LEVEL_AWAIT = new OptionKey<>(false);
    @CompilationFinal private boolean topLevelAwait;

    public static final String USE_UTC_FOR_LEGACY_DATES_NAME = JS_OPTION_PREFIX + "use-utc-for-legacy-dates";
    @Option(name = USE_UTC_FOR_LEGACY_DATES_NAME, category = OptionCategory.EXPERT, stability = OptionStability.STABLE, help = "Determines what time zone (UTC or local time zone) should be used when UTC offset is absent in a parsed date.") //
    public static final OptionKey<Boolean> USE_UTC_FOR_LEGACY_DATES = new OptionKey<>(true);
    @CompilationFinal private boolean useUTCForLegacyDates;

    public static final String WEBASSEMBLY_NAME = JS_OPTION_PREFIX + "webassembly";
    @Option(name = WEBASSEMBLY_NAME, category = OptionCategory.USER, stability = OptionStability.STABLE, help = "Enable WebAssembly JavaScript API.") //
    public static final OptionKey<Boolean> WEBASSEMBLY = new OptionKey<>(false);
    @CompilationFinal private boolean webAssembly;

    public static final String NEW_SET_METHODS_NAME = JS_OPTION_PREFIX + "new-set-methods";
    @Option(name = NEW_SET_METHODS_NAME, category = OptionCategory.EXPERT, help = "Enable new Set methods.") //
    public static final OptionKey<Boolean> NEW_SET_METHODS = new OptionKey<>(false);
    @CompilationFinal private boolean newSetMethods;

    public static final String ATOMICS_WAIT_ASYNC_NAME = JS_OPTION_PREFIX + "atomics-wait-async";
    @Option(name = ATOMICS_WAIT_ASYNC_NAME, category = OptionCategory.EXPERT, help = "Enable Atomics.waitAsync.") //
    public static final OptionKey<Boolean> ATOMICS_WAIT_ASYNC = new OptionKey<>(false);
    @CompilationFinal private boolean atomicsWaitAsync;

    public static final String TEMPORAL_NAME = JS_OPTION_PREFIX + "temporal";
    @Option(name = TEMPORAL_NAME, category = OptionCategory.EXPERT, help = "Enable JavaScript Temporal API.") //
    public static final OptionKey<Boolean> TEMPORAL = new OptionKey<>(false);
    @CompilationFinal private boolean temporal;

    public static final String ITERATOR_HELPERS_NAME = JS_OPTION_PREFIX + "iterator-helpers";
    @Option(name = ITERATOR_HELPERS_NAME, category = OptionCategory.EXPERT, help = "Enable JavaScript Iterator Helpers API.") //
    public static final OptionKey<Boolean> ITERATOR_HELPERS = new OptionKey<>(false);
    @CompilationFinal private boolean iteratorHelpers;

    public static final String ASYNC_ITERATOR_HELPERS_NAME = JS_OPTION_PREFIX + "async-iterator-helpers";
    @Option(name = ASYNC_ITERATOR_HELPERS_NAME, category = OptionCategory.EXPERT, help = "Enable JavaScript Async Iterator Helpers API.") //
    public static final OptionKey<Boolean> ASYNC_ITERATOR_HELPERS = new OptionKey<>(false);
    @CompilationFinal private boolean asyncIteratorHelpers;

    public static final String SHADOW_REALM_NAME = JS_OPTION_PREFIX + "shadow-realm";
    @Option(name = SHADOW_REALM_NAME, category = OptionCategory.EXPERT, help = "Enable ShadowRealm API.") //
    public static final OptionKey<Boolean> SHADOW_REALM = new OptionKey<>(false);
    @CompilationFinal private boolean shadowRealm;

    public static final String ASYNC_CONTEXT_NAME = JS_OPTION_PREFIX + "async-context";
    @Option(name = ASYNC_CONTEXT_NAME, category = OptionCategory.EXPERT, help = "Enable AsyncContext proposal.") //
    public static final OptionKey<Boolean> ASYNC_CONTEXT = new OptionKey<>(false);
    @CompilationFinal private boolean asyncContext;

    public static final String ALLOW_NARROW_SPACES_IN_DATE_FORMAT_NAME = JS_OPTION_PREFIX + "allow-narrow-spaces-in-date-format";
    @Option(name = ALLOW_NARROW_SPACES_IN_DATE_FORMAT_NAME, category = OptionCategory.EXPERT, help = "Allow narrow spaces when formatting dates.") //
    public static final OptionKey<Boolean> ALLOW_NARROW_SPACES_IN_DATE_FORMAT = new OptionKey<>(true);
    @CompilationFinal private boolean allowNarrowSpacesInDateFormat;

    public static final String V8_INTRINSICS_NAME = JS_OPTION_PREFIX + "v8-intrinsics";
    @Option(name = V8_INTRINSICS_NAME, category = OptionCategory.INTERNAL, help = "Enable parsing of V8 intrinsics.") //
    public static final OptionKey<Boolean> V8_INTRINSICS = new OptionKey<>(false);
    @CompilationFinal private boolean v8Intrinsics;

    public static final String ARRAY_ELEMENTS_AMONG_MEMBERS_NAME = JS_OPTION_PREFIX + "array-elements-among-members";
    @Option(name = ARRAY_ELEMENTS_AMONG_MEMBERS_NAME, category = OptionCategory.EXPERT, help = "Include array indices in Value.getMemberKeys().") //
    public static final OptionKey<Boolean> ARRAY_ELEMENTS_AMONG_MEMBERS = new OptionKey<>(true);
    @CompilationFinal private boolean arrayElementsAmongMembers;

    public static final String STACK_TRACE_API_NAME = JS_OPTION_PREFIX + "stack-trace-api";
    @Option(name = STACK_TRACE_API_NAME, category = OptionCategory.EXPERT, help = "Enable Stack Trace API (Error.captureStackTrace/prepareStackTrace/stackTraceLimit).") //
    public static final OptionKey<Boolean> STACK_TRACE_API = new OptionKey<>(false);
    @CompilationFinal private boolean stackTraceAPI;

    public static final String WORKER_NAME = JS_OPTION_PREFIX + "worker";
    @Option(name = WORKER_NAME, category = OptionCategory.EXPERT, help = "Enable Worker API.") //
    public static final OptionKey<Boolean> WORKER = new OptionKey<>(false);
    @CompilationFinal private boolean worker;

    public enum UnhandledRejectionsTrackingMode {
        NONE,
        WARN,
        THROW,
        HANDLER;

        @Override
        public String toString() {
            return name().toLowerCase(Locale.ENGLISH);
        }
    }

    public static final String UNHANDLED_REJECTIONS_NAME = JS_OPTION_PREFIX + "unhandled-rejections";

    @Option(name = UNHANDLED_REJECTIONS_NAME, category = OptionCategory.USER, stability = OptionStability.STABLE, sandbox = SandboxPolicy.CONSTRAINED, help = """
                    Configure unhandled promise rejections tracking. Accepted values: \
                    'none', unhandled rejections are not tracked. \
                    'warn', a warning is printed to stderr when an unhandled rejection is detected. \
                    'throw', an exception is thrown when an unhandled rejection is detected. \
                    'handler', the handler function set with Graal.setUnhandledPromiseRejectionHandler will be called with the rejection value and promise respectively as arguments.""") //
    public static final OptionKey<UnhandledRejectionsTrackingMode> UNHANDLED_REJECTIONS = new OptionKey<>(UnhandledRejectionsTrackingMode.NONE);
    @CompilationFinal private UnhandledRejectionsTrackingMode unhandledRejectionsMode;

    public static final String OPERATOR_OVERLOADING_NAME = JS_OPTION_PREFIX + "operator-overloading";
    @Option(name = OPERATOR_OVERLOADING_NAME, category = OptionCategory.USER, help = "Enable operator overloading") //
    public static final OptionKey<Boolean> OPERATOR_OVERLOADING = new OptionKey<>(false);
    @CompilationFinal private boolean operatorOverloading;

    public static final String ERROR_CAUSE_NAME = JS_OPTION_PREFIX + "error-cause";
    @Option(name = ERROR_CAUSE_NAME, category = OptionCategory.EXPERT, help = "" +
                    "Enable the error cause proposal. Allows an error to be chained with a cause using the optional options parameter.") //
    public static final OptionKey<Boolean> ERROR_CAUSE = new OptionKey<>(false);
    @CompilationFinal private boolean errorCause;

    public static final String IMPORT_ATTRIBUTES_NAME = JS_OPTION_PREFIX + "import-attributes";
    @Option(name = IMPORT_ATTRIBUTES_NAME, category = OptionCategory.USER, help = "Enable import attributes") //
    public static final OptionKey<Boolean> IMPORT_ATTRIBUTES = new OptionKey<>(false);
    @CompilationFinal private boolean importAttributes;

    public static final String IMPORT_ASSERTIONS_NAME = JS_OPTION_PREFIX + "import-assertions";
    @Option(name = IMPORT_ASSERTIONS_NAME, category = OptionCategory.USER, help = "Enable legacy import assertions") //
    public static final OptionKey<Boolean> IMPORT_ASSERTIONS = new OptionKey<>(false);
    @CompilationFinal private boolean importAssertions;

    public static final String JSON_MODULES_NAME = JS_OPTION_PREFIX + "json-modules";
    @Option(name = JSON_MODULES_NAME, category = OptionCategory.USER, help = "Enable loading of json modules") //
    public static final OptionKey<Boolean> JSON_MODULES = new OptionKey<>(false);
    @CompilationFinal private boolean jsonModules;

    public static final String SOURCE_PHASE_IMPORTS_NAME = JS_OPTION_PREFIX + "source-phase-imports";
    @Option(name = SOURCE_PHASE_IMPORTS_NAME, category = OptionCategory.USER, help = "Enable source phase imports proposal") //
    public static final OptionKey<Boolean> SOURCE_PHASE_IMPORTS = new OptionKey<>(false);
    @CompilationFinal private boolean sourcePhaseImports;

    public static final String WASM_BIG_INT_NAME = JS_OPTION_PREFIX + "wasm-bigint";
    @Option(name = WASM_BIG_INT_NAME, category = OptionCategory.USER, help = "Enable wasm i64 to javascript BigInt support") //
    public static final OptionKey<Boolean> WASM_BIG_INT = new OptionKey<>(true);
    @CompilationFinal private boolean wasmBigInt;

    public static final String ESM_EVAL_RETURNS_EXPORTS_NAME = JS_OPTION_PREFIX + "esm-eval-returns-exports";
    @Option(name = ESM_EVAL_RETURNS_EXPORTS_NAME, category = OptionCategory.EXPERT, stability = OptionStability.STABLE, sandbox = SandboxPolicy.UNTRUSTED, help = "Eval of an ES module through the polyglot API returns its exported symbols.") //
    public static final OptionKey<Boolean> ESM_EVAL_RETURNS_EXPORTS = new OptionKey<>(false);
    @CompilationFinal private boolean esmEvalReturnsExports;

    public static final String MLE_MODE_NAME = JS_OPTION_PREFIX + "mle-mode";
    @Option(name = MLE_MODE_NAME, category = OptionCategory.INTERNAL, help = "Provide a non-API MLE builtin. Behaviour will likely change. Don't depend on this in production code.") //
    public static final OptionKey<Boolean> MLE_MODE = new OptionKey<>(false);
    public static final String MLE_PROPERTY_NAME = "MLE";
    @CompilationFinal private boolean mleMode;

    public static final String PRIVATE_FIELDS_IN_NAME = JS_OPTION_PREFIX + "private-fields-in";
    @Option(name = PRIVATE_FIELDS_IN_NAME, category = OptionCategory.USER, help = "Enable private field in in operator") //
    public static final OptionKey<Boolean> PRIVATE_FIELDS_IN = new OptionKey<>(false);
    @CompilationFinal private boolean privateFieldsIn;

    public static final String ESM_BARE_SPECIFIER_RELATIVE_LOOKUP_NAME = JS_OPTION_PREFIX + "esm-bare-specifier-relative-lookup";
    @Option(name = ESM_BARE_SPECIFIER_RELATIVE_LOOKUP_NAME, category = OptionCategory.EXPERT, help = "Resolve ESM bare specifiers relative to the importing module's path instead of attempting an absolute path lookup.") //
    public static final OptionKey<Boolean> ESM_BARE_SPECIFIER_RELATIVE_LOOKUP = new OptionKey<>(false);
    @CompilationFinal private boolean esmBareSpecifierRelativeLookup;

    public static final String CHARSET_NAME = JS_OPTION_PREFIX + "charset";
    @Option(name = CHARSET_NAME, category = OptionCategory.EXPERT, usageSyntax = "UTF-8|UTF-32|<name>", help = "Charset used for decoding/encoding of the input/output streams.") //
    public static final OptionKey<String> CHARSET = new OptionKey<>("", new OptionType<>("CharsetName", new Function<String, String>() {
        @Override
        public String apply(String name) {
            if (name.isEmpty()) {
                return "";
            }
            try {
                return Charset.forName(name).name();
            } catch (Exception e) {
                throw new IllegalArgumentException(e);
            }
        }
    }));

    public static final String SCOPE_OPTIMIZATION_NAME = JS_OPTION_PREFIX + "scope-optimization";
    @Option(name = SCOPE_OPTIMIZATION_NAME, category = OptionCategory.INTERNAL, help = "Allow scope optimizations around closures.") //
    public static final OptionKey<Boolean> SCOPE_OPTIMIZATION = new OptionKey<>(true);
    @CompilationFinal private boolean scopeOptimization;

    public static final String FREQUENCY_BASED_PROPERTY_CACHE_LIMIT_NAME = JS_OPTION_PREFIX + "frequency-based-property-cache-limit";
    @Option(name = FREQUENCY_BASED_PROPERTY_CACHE_LIMIT_NAME, category = OptionCategory.INTERNAL, help = "Maximum size of high-frequency-key property cache.") //
    public static final OptionKey<Integer> FREQUENCY_BASED_PROPERTY_CACHE_LIMIT = new OptionKey<>(5, integerRange(0, Short.MAX_VALUE));
    @CompilationFinal private short frequencyBasedPropertyCacheLimit;

    private static OptionType<Integer> integerRange(int min, int max) {
        return new OptionType<>("Integer", sv -> {
            try {
                int iv = Integer.parseInt(sv);
                if (iv < min || iv > max) {
                    throw new NumberFormatException();
                }
                return iv;
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(String.format("Value \"%s\" is not an integer in the range [%d, %d].", sv, min, max));
            }
        });
    }

    JSContextOptions(SandboxPolicy sandboxPolicy, OptionValues optionValues) {
        this.optionValues = optionValues;
        setOptionValues(sandboxPolicy, optionValues);
    }

    public static JSContextOptions fromOptionValues(SandboxPolicy sandboxPolicy, OptionValues optionValues) {
        return new JSContextOptions(sandboxPolicy, optionValues);
    }

    public void setOptionValues(SandboxPolicy sandboxPolicy, OptionValues newOptions) {
        CompilerAsserts.neverPartOfCompilation();
        optionValues = newOptions;
        cacheOptions(sandboxPolicy);
    }

    private void cacheOptions(SandboxPolicy sandboxPolicy) {
        this.nashornCompatibilityMode = readBooleanOption(NASHORN_COMPATIBILITY_MODE);
        if (nashornCompatibilityMode && !ECMASCRIPT_VERSION.hasBeenSet(optionValues)) {
            // default to ES5 in nashorn-compat mode
            this.ecmascriptVersion = JSConfig.ECMAScript5;
        } else {
            this.ecmascriptVersion = readIntegerOption(ECMASCRIPT_VERSION);
        }

        this.annexB = readBooleanOption(ANNEX_B);
        this.intl402 = INTL_402.hasBeenSet(optionValues) ? readBooleanOption(INTL_402) : !nashornCompatibilityMode;
        this.regexpStaticResult = readBooleanOption(REGEXP_STATIC_RESULT);
        this.regexpMatchIndices = REGEXP_MATCH_INDICES.hasBeenSet(optionValues) ? readBooleanOption(REGEXP_MATCH_INDICES) : getEcmaScriptVersion() >= JSConfig.ECMAScript2022;
        this.regexpUnicodeSets = REGEXP_UNICODE_SETS.hasBeenSet(optionValues) ? readBooleanOption(REGEXP_UNICODE_SETS) : getEcmaScriptVersion() >= JSConfig.ECMAScript2024;
        this.sharedArrayBuffer = readBooleanOption(SHARED_ARRAY_BUFFER);
        this.v8CompatibilityMode = readBooleanOption(V8_COMPATIBILITY_MODE);
        this.v8RealmBuiltin = readBooleanOption(V8_REALM_BUILTIN);
        this.directByteBuffer = readBooleanOption(DIRECT_BYTE_BUFFER);
        this.parseOnly = readBooleanOption(PARSE_ONLY);
        this.debug = readBooleanOption(DEBUG_BUILTIN);
        this.zoneRulesBasedTimeZones = readBooleanOption(ZONE_RULES_BASED_TIME_ZONES);
        this.timerResolution = optionValues.hasBeenSet(TIMER_RESOLUTION) ? readLongOption(TIMER_RESOLUTION)
                        : sandboxPolicy.isStricterOrEqual(SandboxPolicy.UNTRUSTED) ? TimeUnit.SECONDS.toNanos(1) : TIMER_RESOLUTION.getDefaultValue();
        this.agentCanBlock = readBooleanOption(AGENT_CAN_BLOCK);
        this.awaitOptimization = readBooleanOption(AWAIT_OPTIMIZATION);
        this.allowEval = readBooleanOption(ALLOW_EVAL) && !readBooleanOption(DISABLE_EVAL);
        this.disableWith = readBooleanOption(DISABLE_WITH);
        this.regexDumpAutomata = readBooleanOption(REGEX_DUMP_AUTOMATA);
        this.regexStepExecution = readBooleanOption(REGEX_STEP_EXECUTION);
        this.regexAlwaysEager = readBooleanOption(REGEX_ALWAYS_EAGER);
        this.scriptEngineGlobalScopeImport = readBooleanOption(SCRIPT_ENGINE_GLOBAL_SCOPE_IMPORT);
        this.hasForeignObjectPrototype = readBooleanOption(FOREIGN_OBJECT_PROTOTYPE);
        this.hasForeignHashProperties = readBooleanOption(FOREIGN_HASH_PROPERTIES);
        this.functionArgumentsLimit = readLongOption(FUNCTION_ARGUMENTS_LIMIT);
        this.test262Mode = readBooleanOption(TEST262_MODE);
        this.testV8Mode = readBooleanOption(TESTV8_MODE);
        this.validateRegExpLiterals = readBooleanOption(VALIDATE_REGEXP_LITERALS);
        this.functionConstructorCacheSize = readIntegerOption(FUNCTION_CONSTRUCTOR_CACHE_SIZE);
        this.regexCacheSize = readIntegerOption(REGEX_CACHE_SIZE);
        this.stringLengthLimit = readIntegerOption(STRING_LENGTH_LIMIT);
        this.stringLazySubstrings = readBooleanOption(STRING_LAZY_SUBSTRINGS);
        this.bindMemberFunctions = readBooleanOption(BIND_MEMBER_FUNCTIONS);
        this.commonJSRequire = readBooleanOption(COMMONJS_REQUIRE);
        this.regexRegressionTestMode = readBooleanOption(REGEX_REGRESSION_TEST_MODE);
        this.testCloneUninitialized = readBooleanOption(TEST_CLONE_UNINITIALIZED);
        this.lazyTranslation = readBooleanOption(LAZY_TRANSLATION);
        this.stackTraceLimit = readIntegerOption(STACK_TRACE_LIMIT);
        this.maxTypedArrayLength = readIntegerOption(MAX_TYPED_ARRAY_LENGTH);
        this.maxApplyArgumentLength = readIntegerOption(MAX_APPLY_ARGUMENT_LENGTH);
        this.maxPrototypeChainLength = readIntegerOption(MAX_PROTOTYPE_CHAIN_LENGTH);
        this.asyncStackTraces = readBooleanOption(ASYNC_STACK_TRACES);
        this.topLevelAwait = TOP_LEVEL_AWAIT.hasBeenSet(optionValues) ? readBooleanOption(TOP_LEVEL_AWAIT) : getEcmaScriptVersion() >= JSConfig.ECMAScript2022;
        this.useUTCForLegacyDates = USE_UTC_FOR_LEGACY_DATES.hasBeenSet(optionValues) ? readBooleanOption(USE_UTC_FOR_LEGACY_DATES) : !v8CompatibilityMode;
        this.webAssembly = readBooleanOption(WEBASSEMBLY);
        this.unhandledRejectionsMode = readUnhandledRejectionsMode();
        this.newSetMethods = readBooleanOption(NEW_SET_METHODS);
        this.atomicsWaitAsync = ATOMICS_WAIT_ASYNC.hasBeenSet(optionValues) ? readBooleanOption(ATOMICS_WAIT_ASYNC) : getEcmaScriptVersion() >= JSConfig.ECMAScript2024;
        this.asyncIteratorHelpers = getEcmaScriptVersion() >= JSConfig.ECMAScript2018 && readBooleanOption(ASYNC_ITERATOR_HELPERS);
        this.iteratorHelpers = getEcmaScriptVersion() >= JSConfig.ECMAScript2018 && (this.asyncIteratorHelpers || readBooleanOption(ITERATOR_HELPERS));
        this.shadowRealm = getEcmaScriptVersion() >= JSConfig.ECMAScript2015 && readBooleanOption(SHADOW_REALM);
        this.asyncContext = readBooleanOption(ASYNC_CONTEXT);
        this.operatorOverloading = readBooleanOption(OPERATOR_OVERLOADING);
        this.errorCause = ERROR_CAUSE.hasBeenSet(optionValues) ? readBooleanOption(ERROR_CAUSE) : getEcmaScriptVersion() >= JSConfig.ECMAScript2022;
        this.importAttributes = readBooleanOption(IMPORT_ATTRIBUTES);
        this.importAssertions = readBooleanOption(IMPORT_ASSERTIONS);
        this.jsonModules = readBooleanOption(JSON_MODULES);
        this.sourcePhaseImports = readBooleanOption(SOURCE_PHASE_IMPORTS);
        this.wasmBigInt = readBooleanOption(WASM_BIG_INT);
        this.esmEvalReturnsExports = readBooleanOption(ESM_EVAL_RETURNS_EXPORTS);
        this.printNoNewline = readBooleanOption(PRINT_NO_NEWLINE);
        this.mleMode = readBooleanOption(MLE_MODE) || readBooleanOption(INTEROP_COMPLETE_PROMISES);
        this.privateFieldsIn = PRIVATE_FIELDS_IN.hasBeenSet(optionValues) ? readBooleanOption(PRIVATE_FIELDS_IN) : getEcmaScriptVersion() >= JSConfig.ECMAScript2022;
        this.esmBareSpecifierRelativeLookup = readBooleanOption(ESM_BARE_SPECIFIER_RELATIVE_LOOKUP);
        this.temporal = readBooleanOption(TEMPORAL);
        this.propertyCacheLimit = readIntegerOption(PROPERTY_CACHE_LIMIT);
        this.functionCacheLimit = readIntegerOption(FUNCTION_CACHE_LIMIT);
        this.frequencyBasedPropertyCacheLimit = (short) readIntegerOption(FREQUENCY_BASED_PROPERTY_CACHE_LIMIT);
        this.scopeOptimization = readBooleanOption(SCOPE_OPTIMIZATION);
        this.allowNarrowSpacesInDateFormat = ALLOW_NARROW_SPACES_IN_DATE_FORMAT.hasBeenSet(optionValues) ? readBooleanOption(ALLOW_NARROW_SPACES_IN_DATE_FORMAT) : !isV8CompatibilityMode();
        this.v8Intrinsics = readBooleanOption(V8_INTRINSICS);
        this.arrayElementsAmongMembers = readBooleanOption(ARRAY_ELEMENTS_AMONG_MEMBERS);
        this.stackTraceAPI = STACK_TRACE_API.hasBeenSet(optionValues) ? readBooleanOption(STACK_TRACE_API) : (v8CompatibilityMode || nashornCompatibilityMode);
        this.worker = WORKER.hasBeenSet(optionValues) ? readBooleanOption(WORKER) : testV8Mode;
    }

    private UnhandledRejectionsTrackingMode readUnhandledRejectionsMode() {
        return UNHANDLED_REJECTIONS.getValue(optionValues);
    }

    private boolean readBooleanOption(OptionKey<Boolean> key) {
        return key.getValue(optionValues);
    }

    private int readIntegerOption(OptionKey<Integer> key) {
        return key.getValue(optionValues);
    }

    private long readLongOption(OptionKey<Long> key) {
        return key.getValue(optionValues);
    }

    public static TruffleOptionDescriptors optionDescriptorsWithDefaultValues() {
        JSContextOptionsOptionDescriptors originalOptionDescriptors = new JSContextOptionsOptionDescriptors();
        ArrayList<OptionDescriptor> options = new ArrayList<>();
        JSContextOptions.describeOptions(originalOptionDescriptors, options);
        OptionDescriptors optionDescriptorsWithDefaults = OptionDescriptors.create(options);
        return new TruffleOptionDescriptors() {
            @Override
            public SandboxPolicy getSandboxPolicy(String key) {
                return originalOptionDescriptors.getSandboxPolicy(key);
            }

            @Override
            public OptionDescriptor get(String optionName) {
                return optionDescriptorsWithDefaults.get(optionName);
            }

            @Override
            public Iterator<OptionDescriptor> iterator() {
                return optionDescriptorsWithDefaults.iterator();
            }
        };
    }

    private static String helpWithDefault(String helpMessage, OptionKey<? extends Object> key) {
        return helpMessage + " (default:" + key.getDefaultValue() + ")";
    }

    private static OptionDescriptor newOptionDescriptor(OptionKey<?> key, String name, OptionCategory category, OptionStability stability, String help, String usageSyntax) {
        return OptionDescriptor.newBuilder(key, name).category(category).help(helpWithDefault(help, key)).stability(stability).usageSyntax(usageSyntax).build();
    }

    private static void describeOptions(JSContextOptionsOptionDescriptors descriptors, List<OptionDescriptor> options) {
        for (OptionDescriptor desc : descriptors) {
            options.add(newOptionDescriptor(desc.getKey(), desc.getName(), desc.getCategory(), desc.getStability(), desc.getHelp(), desc.getUsageSyntax()));
        }
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

    public boolean isRegexpMatchIndices() {
        return regexpMatchIndices;
    }

    public boolean isRegexpUnicodeSets() {
        return regexpUnicodeSets;
    }

    public boolean isRegexpStaticResult() {
        return regexpStaticResult;
    }

    public boolean isSharedArrayBuffer() {
        if (getEcmaScriptVersion() < JSConfig.ECMAScript2017) {
            return false;
        }
        return sharedArrayBuffer;
    }

    public boolean isAtomics() {
        if (getEcmaScriptVersion() < JSConfig.ECMAScript2017) {
            return false;
        }
        return ATOMICS.getValue(optionValues);
    }

    public boolean isV8CompatibilityMode() {
        return v8CompatibilityMode;
    }

    public boolean isNashornCompatibilityMode() {
        return nashornCompatibilityMode;
    }

    public boolean isDebugBuiltin() {
        return debug;
    }

    public boolean isMLEMode() {
        return mleMode;
    }

    public boolean isDirectByteBuffer() {
        return directByteBuffer;
    }

    public boolean isParseOnly() {
        return parseOnly;
    }

    public long getTimerResolution() {
        return timerResolution;
    }

    public boolean isV8RealmBuiltin() {
        return v8RealmBuiltin;
    }

    public boolean hasZoneRulesBasedTimeZones() {
        return zoneRulesBasedTimeZones;
    }

    public boolean canAgentBlock() {
        return agentCanBlock;
    }

    public boolean isAwaitOptimization() {
        return awaitOptimization;
    }

    public boolean isTopLevelAwait() {
        return topLevelAwait;
    }

    public boolean allowEval() {
        return allowEval;
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

    public boolean hasForeignHashProperties() {
        return hasForeignHashProperties;
    }

    public boolean isGlobalProperty() {
        CompilerAsserts.neverPartOfCompilation("Context patchable option global-property was assumed not to be accessed in compiled code.");
        return GLOBAL_PROPERTY.getValue(optionValues);
    }

    public boolean isGlobalArguments() {
        CompilerAsserts.neverPartOfCompilation("Context patchable option " + GLOBAL_ARGUMENTS_NAME + " was assumed not to be accessed in compiled code.");
        return GLOBAL_ARGUMENTS.getValue(optionValues);
    }

    public boolean isConsole() {
        CompilerAsserts.neverPartOfCompilation("Context patchable option console was assumed not to be accessed in compiled code.");
        return CONSOLE.getValue(optionValues) || (!CONSOLE.hasBeenSet(optionValues) && isShell());
    }

    public boolean isPrint() {
        CompilerAsserts.neverPartOfCompilation("Context patchable option print was assumed not to be accessed in compiled code.");
        return PRINT.getValue(optionValues) || (!PRINT.hasBeenSet(optionValues) && (isShell() || isNashornCompatibilityMode()));
    }

    public boolean isPrintNoNewline() {
        return printNoNewline;
    }

    public boolean isLoad() {
        CompilerAsserts.neverPartOfCompilation("Context patchable option load was assumed not to be accessed in compiled code.");
        return LOAD.getValue(optionValues) || (!LOAD.hasBeenSet(optionValues) && (isShell() || isNashornCompatibilityMode()));
    }

    public boolean isCommonJSRequire() {
        return commonJSRequire;
    }

    public Map<String, String> getCommonJSRequireBuiltins() {
        CompilerAsserts.neverPartOfCompilation("Context patchable option load was assumed not to be accessed in compiled code.");
        return COMMONJS_CORE_MODULES_REPLACEMENTS.getValue(optionValues);
    }

    public String getRequireCwd() {
        CompilerAsserts.neverPartOfCompilation("Context patchable option load was assumed not to be accessed in compiled code.");
        return COMMONJS_REQUIRE_CWD.getValue(optionValues);
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

    public boolean isLoadFromURL() {
        return LOAD_FROM_URL.getValue(optionValues);
    }

    public boolean isLoadFromClasspath() {
        return LOAD_FROM_CLASSPATH.getValue(optionValues);
    }

    public boolean isBigInt() {
        if (getEcmaScriptVersion() < JSConfig.ECMAScript2019) {
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

    public String getCharset() {
        return CHARSET.getValue(optionValues);
    }

    public int getFunctionConstructorCacheSize() {
        return functionConstructorCacheSize;
    }

    public int getRegexCacheSize() {
        return regexCacheSize;
    }

    public int getStringLengthLimit() {
        return stringLengthLimit;
    }

    public boolean isStringLazySubstrings() {
        return stringLazySubstrings;
    }

    public boolean bindMemberFunctions() {
        return bindMemberFunctions;
    }

    public boolean isRegexRegressionTestMode() {
        return regexRegressionTestMode;
    }

    public String getDebugPropertyName() {
        CompilerAsserts.neverPartOfCompilation("Context patchable option debug-property-name was assumed not to be accessed in compiled code.");
        return DEBUG_PROPERTY_NAME.getValue(optionValues);
    }

    public boolean isProfileTime() {
        CompilerAsserts.neverPartOfCompilation("Context patchable option profile-time was assumed not to be accessed in compiled code.");
        return PROFILE_TIME.getValue(optionValues);
    }

    public boolean isTestCloneUninitialized() {
        return testCloneUninitialized;
    }

    public boolean isLazyTranslation() {
        return lazyTranslation;
    }

    public boolean isProfileTimePrintCumulative() {
        CompilerAsserts.neverPartOfCompilation("Context patchable option profile-time-print-cumulative was assumed not to be accessed in compiled code.");
        return PROFILE_TIME_PRINT_CUMULATIVE.getValue(optionValues);
    }

    public int getStackTraceLimit() {
        return stackTraceLimit;
    }

    public int getMaxTypedArrayLength() {
        return maxTypedArrayLength;
    }

    public int getMaxApplyArgumentLength() {
        return maxApplyArgumentLength;
    }

    public int getMaxPrototypeChainLength() {
        return maxPrototypeChainLength;
    }

    public int getPropertyCacheLimit() {
        return propertyCacheLimit;
    }

    public int getFunctionCacheLimit() {
        return functionCacheLimit;
    }

    public boolean isAsyncStackTraces() {
        return asyncStackTraces;
    }

    public boolean shouldUseUTCForLegacyDates() {
        return useUTCForLegacyDates;
    }

    public boolean isWebAssembly() {
        return webAssembly;
    }

    public boolean isTemporal() {
        return temporal;
    }

    public UnhandledRejectionsTrackingMode getUnhandledRejectionsMode() {
        return unhandledRejectionsMode;
    }

    public boolean isNewSetMethods() {
        return newSetMethods;
    }

    public boolean isAtomicsWaitAsync() {
        return atomicsWaitAsync;
    }

    public boolean isIteratorHelpers() {
        return iteratorHelpers;
    }

    public boolean isAsyncIteratorHelpers() {
        return asyncIteratorHelpers;
    }

    public boolean isShadowRealm() {
        return shadowRealm;
    }

    public boolean isAsyncContext() {
        return asyncContext;
    }

    public boolean isOperatorOverloading() {
        return operatorOverloading;
    }

    public boolean isErrorCauseEnabled() {
        return errorCause;
    }

    public boolean isImportAttributes() {
        return importAttributes;
    }

    public boolean isImportAssertions() {
        return importAssertions;
    }

    public boolean isJsonModules() {
        return jsonModules;
    }

    public boolean isSourcePhaseImports() {
        return sourcePhaseImports;
    }

    public boolean isWasmBigInt() {
        return wasmBigInt;
    }

    public boolean isEsmEvalReturnsExports() {
        return esmEvalReturnsExports;
    }

    public boolean isPrivateFieldsIn() {
        return privateFieldsIn;
    }

    public boolean isEsmBareSpecifierRelativeLookup() {
        return esmBareSpecifierRelativeLookup;
    }

    public boolean isScopeOptimization() {
        return scopeOptimization;
    }

    public boolean allowNarrowSpacesInDateFormat() {
        return allowNarrowSpacesInDateFormat;
    }

    public boolean isSyntaxExtensions() {
        return SYNTAX_EXTENSIONS.hasBeenSet(optionValues) ? SYNTAX_EXTENSIONS.getValue(optionValues) : NASHORN_COMPATIBILITY_MODE.getValue(optionValues);
    }

    public boolean isScripting() {
        return SCRIPTING.getValue(optionValues);
    }

    public boolean isShebang() {
        return SHEBANG.hasBeenSet(optionValues) ? SHEBANG.getValue(optionValues) : getEcmaScriptVersion() >= JSConfig.ECMAScript2020;
    }

    public boolean isStrict() {
        return STRICT.getValue(optionValues);
    }

    public boolean isConstAsVar() {
        return CONST_AS_VAR.getValue(optionValues);
    }

    public boolean isFunctionStatementError() {
        return FUNCTION_STATEMENT_ERROR.getValue(optionValues);
    }

    public boolean isClassFields() {
        return CLASS_FIELDS.hasBeenSet(optionValues) ? CLASS_FIELDS.getValue(optionValues) : getEcmaScriptVersion() >= JSContextOptions.CLASS_FIELDS_ES_VERSION;
    }

    public boolean isV8Intrinsics() {
        return v8Intrinsics;
    }

    public boolean isArrayElementsAmongMembers() {
        return arrayElementsAmongMembers;
    }

    public boolean isStackTraceAPI() {
        return stackTraceAPI;
    }

    public short getFrequencyBasedPropertyCacheLimit() {
        return frequencyBasedPropertyCacheLimit;
    }

    public boolean isWorker() {
        return worker;
    }

}
