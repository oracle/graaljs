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

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import org.graalvm.options.OptionCategory;
import org.graalvm.options.OptionDescriptor;
import org.graalvm.options.OptionKey;
import org.graalvm.options.OptionValues;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.TruffleLanguage.Env;

public final class JSContextOptions {
    @CompilationFinal private ParserOptions parserOptions;
    @CompilationFinal private OptionValues optionValues;

    public static final String ECMASCRIPT_VERSION_NAME = JS_OPTION_PREFIX + "ecmascript-version";
    public static final OptionKey<Integer> ECMASCRIPT_VERSION = new OptionKey<>(JSTruffleOptions.MaxECMAScriptVersion);
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
    private static final OptionKey<Boolean> ARRAY_SORT_INHERITED = new OptionKey<>(true);
    private static final String ARRAY_SORT_INHERITED_HELP = "implementation-defined behavior in Array.protoype.sort: sort inherited keys?";
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
    private static final OptionKey<Boolean> V8_COMPATIBILITY_MODE = new OptionKey<>(false);
    private static final String V8_COMPATIBILITY_MODE_HELP = "provide compatibility with the Google V8 engine";
    @CompilationFinal private boolean v8CompatibilityMode;

    public static final String V8_REALM_BUILTIN_NAME = JS_OPTION_PREFIX + "v8-realm-builtin";
    private static final OptionKey<Boolean> V8_REALM_BUILTIN = new OptionKey<>(false);
    private static final String V8_REALM_BUILTIN_HELP = "Provide Realm builtin compatible with V8's d8 shell.";
    @CompilationFinal private boolean v8RealmBuiltin;

    public static final String NASHORN_COMPATIBILITY_MODE_NAME = JS_OPTION_PREFIX + "nashorn-compat";
    private static final OptionKey<Boolean> NASHORN_COMPATIBILITY_MODE = new OptionKey<>(false);
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
    private static final OptionKey<Boolean> DIRECT_BYTE_BUFFER = new OptionKey<>(JSTruffleOptions.DirectByteBuffer);
    private static final String DIRECT_BYTE_BUFFER_HELP = "Use direct (off-heap) byte buffer for typed arrays.";
    @CompilationFinal private boolean directByteBuffer;

    public static final String PARSE_ONLY_NAME = JS_OPTION_PREFIX + "parse-only";
    private static final OptionKey<Boolean> PARSE_ONLY = new OptionKey<>(false);
    private static final String PARSE_ONLY_HELP = "Only parse source code, do not run it.";
    @CompilationFinal private boolean parseOnly;

    public static final String TIME_ZONE_NAME = JS_OPTION_PREFIX + "timezone";
    public static final OptionKey<String> TIME_ZONE = new OptionKey<>("");
    private static final String TIME_ZONE_HELP = "Set custom timezone.";

    public static final String TIMER_RESOLUTION_NAME = JS_OPTION_PREFIX + "timer-resolution";
    private static final OptionKey<Long> TIMER_RESOLUTION = new OptionKey<>(1000000L);
    private static final String TIMER_RESOLUTION_HELP = "Resolution of timers (performance.now() and Date built-ins) in nanoseconds. Fuzzy time is used when set to 0.";
    @CompilationFinal private long timerResolution;

    public static final String AGENT_CAN_BLOCK_NAME = JS_OPTION_PREFIX + "agent-can-block";
    public static final OptionKey<Boolean> AGENT_CAN_BLOCK = new OptionKey<>(true);
    private static final String AGENT_CAN_BLOCK_HELP = "Determines whether agents can block or not.";
    @CompilationFinal private boolean agentCanBlock;

    public static final String JAVA_PACKAGE_GLOBALS_NAME = JS_OPTION_PREFIX + "java-package-globals";
    public static final OptionKey<Boolean> JAVA_PACKAGE_GLOBALS = new OptionKey<>(true);
    private static final String JAVA_PACKAGE_GLOBALS_HELP = "provide Java package globals: Packages, java, javafx, javax, com, org, edu.";

    public static final String GLOBAL_THIS_NAME = JS_OPTION_PREFIX + "global-this";
    public static final OptionKey<Boolean> GLOBAL_THIS = new OptionKey<>(true);
    private static final String GLOBAL_THIS_HELP = "provide 'global' global property.";

    public static final String CONSOLE_NAME = JS_OPTION_PREFIX + "console";
    public static final OptionKey<Boolean> CONSOLE = new OptionKey<>(true);
    private static final String CONSOLE_HELP = "provide 'console' global property.";

    public static final String PERFORMANCE_NAME = JS_OPTION_PREFIX + "performance";
    public static final OptionKey<Boolean> PERFORMANCE = new OptionKey<>(true);
    private static final String PERFORMANCE_HELP = "provide 'performance' global property.";

    public static final String SHELL_NAME = JS_OPTION_PREFIX + "shell";
    public static final OptionKey<Boolean> SHELL = new OptionKey<>(false);
    private static final String SHELL_HELP = "provide global functions for js shell.";

    public static final String GRAAL_BUILTIN_NAME = JS_OPTION_PREFIX + "graal-builtin";
    public static final OptionKey<Boolean> GRAAL_BUILTIN = new OptionKey<>(true);
    private static final String GRAAL_BUILTIN_HELP = "provide 'Graal' global property.";

    /**
     * Options which can be patched without throwing away the pre-initialized context.
     */
    private static final OptionKey<?>[] PREINIT_CONTEXT_PATCHABLE_OPTIONS = {
                    ARRAY_SORT_INHERITED,
                    TIMER_RESOLUTION,
                    SHELL,
    };

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
        this.arraySortInherited = readBooleanOption(ARRAY_SORT_INHERITED, ARRAY_SORT_INHERITED_NAME);
        this.sharedArrayBuffer = readBooleanOption(SHARED_ARRAY_BUFFER, SHARED_ARRAY_BUFFER_NAME);
        this.atomics = readBooleanOption(ATOMICS, ATOMICS_NAME);
        this.v8CompatibilityMode = readBooleanOption(V8_COMPATIBILITY_MODE, V8_COMPATIBILITY_MODE_NAME);
        this.v8RealmBuiltin = readBooleanOption(V8_REALM_BUILTIN, V8_REALM_BUILTIN_NAME);
        this.nashornCompatibilityMode = readBooleanOption(NASHORN_COMPATIBILITY_MODE, NASHORN_COMPATIBILITY_MODE_NAME);
        this.directByteBuffer = readBooleanOption(DIRECT_BYTE_BUFFER, DIRECT_BYTE_BUFFER_NAME);
        this.parseOnly = readBooleanOption(PARSE_ONLY, PARSE_ONLY_NAME);
        this.debug = readBooleanOption(DEBUG_BUILTIN, DEBUG_BUILTIN_NAME);
        this.timerResolution = readLongOption(TIMER_RESOLUTION, TIMER_RESOLUTION_NAME);
        this.agentCanBlock = readBooleanOption(AGENT_CAN_BLOCK, AGENT_CAN_BLOCK_NAME);
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
        options.add(newOptionDescriptor(GLOBAL_THIS, GLOBAL_THIS_NAME, OptionCategory.USER, GLOBAL_THIS_HELP));
        options.add(newOptionDescriptor(CONSOLE, CONSOLE_NAME, OptionCategory.USER, CONSOLE_HELP));
        options.add(newOptionDescriptor(PERFORMANCE, PERFORMANCE_NAME, OptionCategory.USER, PERFORMANCE_HELP));
        options.add(newOptionDescriptor(SHELL, SHELL_NAME, OptionCategory.USER, SHELL_HELP));
        options.add(newOptionDescriptor(GRAAL_BUILTIN, GRAAL_BUILTIN_NAME, OptionCategory.USER, GRAAL_BUILTIN_HELP));
    }

    /**
     * Check for options that differ from the expected options and do not support patching, in which
     * case we cannot use the pre-initialized context for faster startup.
     */
    public static boolean optionsAllowPreInitializedContext(Env preinitEnv, Env env) {
        OptionValues preinitOptions = preinitEnv.getOptions();
        OptionValues options = env.getOptions();
        if (!preinitOptions.hasSetOptions() && !options.hasSetOptions()) {
            return true;
        } else if (preinitOptions.equals(options)) {
            return true;
        } else {
            assert preinitOptions.getDescriptors().equals(options.getDescriptors());
            Collection<OptionKey<?>> ignoredOptions = Arrays.asList(PREINIT_CONTEXT_PATCHABLE_OPTIONS);
            for (OptionDescriptor descriptor : options.getDescriptors()) {
                OptionKey<?> key = descriptor.getKey();
                if (preinitOptions.hasBeenSet(key) || options.hasBeenSet(key)) {
                    if (ignoredOptions.contains(key)) {
                        continue;
                    }
                    if (!preinitOptions.get(key).equals(options.get(key))) {
                        return false;
                    }
                }
            }
            return true;
        }
    }

    public int getEcmaScriptVersion() {
        return ecmascriptVersion;
    }

    public boolean isAnnexB() {
        return annexB;
    }

    public boolean isIntl402() {
        return intl402;
    }

    public boolean isRegexpStaticResult() {
        return regexpStaticResult;
    }

    public boolean isArraySortInherited() {
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
        return v8CompatibilityMode;
    }

    public boolean isNashornCompatibilityMode() {
        return nashornCompatibilityMode;
    }

    public boolean isDebugBuiltin() {
        return debug;
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

    public boolean canAgentBlock() {
        return agentCanBlock;
    }

    public boolean isConsole() {
        return CONSOLE.getValue(optionValues);
    }

    public boolean isPerformance() {
        return PERFORMANCE.getValue(optionValues);
    }

    public boolean isShell() {
        return SHELL.getValue(optionValues);
    }

    public boolean isGraalBuiltin() {
        return GRAAL_BUILTIN.getValue(optionValues);
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
        return Objects.equals(this.parserOptions, other.parserOptions);
    }

}
