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

import org.graalvm.options.OptionCategory;
import org.graalvm.options.OptionDescriptor;
import org.graalvm.options.OptionKey;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.TruffleLanguage.Env;

public final class JSContextOptions {
    @CompilationFinal private ParserOptions parserOptions;
    @CompilationFinal private Env env;

    public static final String ECMASCRIPT_VERSION_NAME = JS_OPTION_PREFIX + "ecmascript-version";
    public static final OptionKey<Integer> ECMASCRIPT_VERSION = new OptionKey<>(JSTruffleOptions.MaxECMAScriptVersion);
    private static final String ECMASCRIPT_VERSION_HELP = helpWithDefault("ECMAScript Version.", ECMASCRIPT_VERSION);
    @CompilationFinal private int ecmascriptVersion;

    public static final String ANNEX_B_NAME = JS_OPTION_PREFIX + "annex-b";
    public static final OptionKey<Boolean> ANNEX_B = new OptionKey<>(JSTruffleOptions.AnnexB);
    private static final String ANNEX_B_HELP = helpWithDefault("Enable ECMAScript Annex B features.", ANNEX_B);
    @CompilationFinal private boolean annexB;

    public static final String INTL_402_NAME = JS_OPTION_PREFIX + "intl-402";
    public static final OptionKey<Boolean> INTL_402 = new OptionKey<>(false);
    private static final String INTL_402_HELP = helpWithDefault("Enable ECMAScript Internationalization API", INTL_402);
    @CompilationFinal private boolean intl402;

    public static final String REGEXP_STATIC_RESULT_NAME = JS_OPTION_PREFIX + "regexp-static-result";
    private static final OptionKey<Boolean> REGEXP_STATIC_RESULT = new OptionKey<>(true);
    private static final String REGEXP_STATIC_RESULT_HELP = helpWithDefault("provide last RegExp match in RegExp global var, e.g. RegExp.$1", REGEXP_STATIC_RESULT);
    @CompilationFinal private boolean regexpStaticResult;

    public static final String ARRAY_SORT_INHERITED_NAME = JS_OPTION_PREFIX + "array-sort-inherited";
    private static final OptionKey<Boolean> ARRAY_SORT_INHERITED = new OptionKey<>(true);
    private static final String ARRAY_SORT_INHERITED_HELP = helpWithDefault("implementation-defined behavior in Array.protoype.sort: sort inherited keys?", ARRAY_SORT_INHERITED);
    @CompilationFinal private boolean arraySortInherited;

    public static final String SHARED_ARRAY_BUFFER_NAME = JS_OPTION_PREFIX + "shared-array-buffer";
    private static final OptionKey<Boolean> SHARED_ARRAY_BUFFER = new OptionKey<>(true);
    private static final String SHARED_ARRAY_BUFFER_HELP = helpWithDefault("ES2017 SharedArrayBuffer", SHARED_ARRAY_BUFFER);
    @CompilationFinal private boolean sharedArrayBuffer;

    public static final String ATOMICS_NAME = JS_OPTION_PREFIX + "atomics";
    private static final OptionKey<Boolean> ATOMICS = new OptionKey<>(true);
    private static final String ATOMICS_HELP = helpWithDefault("ES2017 Atomics", ATOMICS);
    @CompilationFinal private boolean atomics;

    public static final String V8_COMPATIBILITY_MODE_NAME = JS_OPTION_PREFIX + "v8-compat";
    private static final OptionKey<Boolean> V8_COMPATIBILITY_MODE = new OptionKey<>(false);
    private static final String V8_COMPATIBILITY_MODE_HELP = helpWithDefault("provide compatibility with the Google V8 engine", V8_COMPATIBILITY_MODE);
    @CompilationFinal private boolean v8CompatibilityMode;

    public static final String V8_REALM_BUILTIN_NAME = JS_OPTION_PREFIX + "v8-realm-builtin";
    private static final OptionKey<Boolean> V8_REALM_BUILTIN = new OptionKey<>(false);
    private static final String V8_REALM_BUILTIN_HELP = helpWithDefault("Provide Realm builtin compatible with V8's d8 shell.", V8_REALM_BUILTIN);
    @CompilationFinal private boolean v8RealmBuiltin;

    public static final String NASHORN_COMPATIBILITY_MODE_NAME = JS_OPTION_PREFIX + "nashorn-compat";
    private static final OptionKey<Boolean> NASHORN_COMPATIBILITY_MODE = new OptionKey<>(false);
    private static final String NASHORN_COMPATIBILITY_MODE_HELP = helpWithDefault("provide compatibility with the OpenJDK Nashorn engine", NASHORN_COMPATIBILITY_MODE);
    @CompilationFinal private boolean nashornCompatibilityMode;

    public static final String STACK_TRACE_LIMIT_NAME = JS_OPTION_PREFIX + "stack-trace-limit";
    private static final OptionKey<Integer> STACK_TRACE_LIMIT = new OptionKey<>(JSTruffleOptions.StackTraceLimit);
    private static final String STACK_TRACE_LIMIT_HELP = helpWithDefault("number of stack frames to capture", STACK_TRACE_LIMIT);
    @CompilationFinal private int stackTraceLimit;

    public static final String DEBUG_BUILTIN_NAME = JS_OPTION_PREFIX + "debug-builtin";
    private static final OptionKey<Boolean> DEBUG_BUILTIN = new OptionKey<>(false);
    private static final String DEBUG_BUILTIN_HELP = helpWithDefault("provide a non-API Debug builtin. Behaviour will likely change. Don't depend on this in production code.", DEBUG_BUILTIN);
    @CompilationFinal private boolean debug;

    public static final String DIRECT_BYTE_BUFFER_NAME = JS_OPTION_PREFIX + "direct-byte-buffer";
    private static final OptionKey<Boolean> DIRECT_BYTE_BUFFER = new OptionKey<>(JSTruffleOptions.DirectByteBuffer);
    private static final String DIRECT_BYTE_BUFFER_HELP = helpWithDefault("Use direct (off-heap) byte buffer for typed arrays.", DIRECT_BYTE_BUFFER);
    @CompilationFinal private boolean directByteBuffer;

    public static final String PARSE_ONLY_NAME = JS_OPTION_PREFIX + "parse-only";
    private static final OptionKey<Boolean> PARSE_ONLY = new OptionKey<>(false);
    private static final String PARSE_ONLY_HELP = helpWithDefault("Only parse source code, do not run it.", PARSE_ONLY);
    @CompilationFinal private boolean parseOnly;

    public static final String TIME_ZONE_NAME = JS_OPTION_PREFIX + "timezone";
    public static final OptionKey<String> TIME_ZONE = new OptionKey<>("");
    private static final String TIME_ZONE_HELP = helpWithDefault("Set custom timezone.", TIME_ZONE);

    public static final String PRECISE_TIME_NAME = JS_OPTION_PREFIX + "precise-time";
    private static final OptionKey<Boolean> PRECISE_TIME = new OptionKey<>(false);
    private static final String PRECISE_TIME_HELP = helpWithDefault("High-resolution timestamps via performance.now()", PRECISE_TIME);
    @CompilationFinal private boolean preciseTime;

    public static final String AGENT_CAN_BLOCK_NAME = JS_OPTION_PREFIX + "agent-can-block";
    public static final OptionKey<Boolean> AGENT_CAN_BLOCK = new OptionKey<>(true);
    private static final String AGENT_CAN_BLOCK_HELP = helpWithDefault("Determines whether agents can block or not.", AGENT_CAN_BLOCK);
    @CompilationFinal private boolean agentCanBlock;

    private static final OptionKey<?>[] PREINIT_CONTEXT_OPTION_KEYS = {
                    ECMASCRIPT_VERSION,
                    ANNEX_B,
                    INTL_402,
                    REGEXP_STATIC_RESULT,
                    SHARED_ARRAY_BUFFER,
                    ATOMICS,
                    V8_COMPATIBILITY_MODE,
                    V8_REALM_BUILTIN,
                    NASHORN_COMPATIBILITY_MODE,
                    STACK_TRACE_LIMIT,
                    DEBUG_BUILTIN,
                    PARSE_ONLY,
                    TIME_ZONE,
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

    public void setEnv(Env newEnv) {
        CompilerAsserts.neverPartOfCompilation();
        if (newEnv != null) {
            this.env = newEnv;
            cacheOptions();
            parserOptions = parserOptions.putOptions(env.getOptions());
        }
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
        this.stackTraceLimit = readIntegerOption(STACK_TRACE_LIMIT, STACK_TRACE_LIMIT_NAME);
        this.directByteBuffer = readBooleanOption(DIRECT_BYTE_BUFFER, DIRECT_BYTE_BUFFER_NAME);
        this.parseOnly = readBooleanOption(PARSE_ONLY, PARSE_ONLY_NAME);
        this.debug = readBooleanOption(DEBUG_BUILTIN, DEBUG_BUILTIN_NAME);
        this.preciseTime = readBooleanOption(PRECISE_TIME, PRECISE_TIME_NAME);
        this.agentCanBlock = readBooleanOption(AGENT_CAN_BLOCK, AGENT_CAN_BLOCK_NAME);
    }

    private boolean readBooleanOption(OptionKey<Boolean> key, String name) {
        if (env == null) {
            return readBooleanFromSystemProperty(key, name);
        } else {
            return env.getOptions().get(key);
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
        if (env == null) {
            return readIntegerFromSystemProperty(key, name);
        } else {
            return env.getOptions().get(key);
        }
    }

    private static int readIntegerFromSystemProperty(OptionKey<Integer> key, String name) {
        return Integer.getInteger("polyglot." + name, key.getDefaultValue());
    }

    public static String helpWithDefault(String helpMessage, OptionKey<? extends Object> key) {
        return helpMessage + " (default:" + key.getDefaultValue() + ")";
    }

    public static void describeOptions(List<OptionDescriptor> options) {
        options.add(OptionDescriptor.newBuilder(ECMASCRIPT_VERSION, ECMASCRIPT_VERSION_NAME).category(OptionCategory.USER).help(ECMASCRIPT_VERSION_HELP).build());
        options.add(OptionDescriptor.newBuilder(ANNEX_B, ANNEX_B_NAME).category(OptionCategory.USER).help(ANNEX_B_HELP).build());
        options.add(OptionDescriptor.newBuilder(INTL_402, INTL_402_NAME).category(OptionCategory.USER).help(INTL_402_HELP).build());
        options.add(OptionDescriptor.newBuilder(REGEXP_STATIC_RESULT, REGEXP_STATIC_RESULT_NAME).category(OptionCategory.USER).help(REGEXP_STATIC_RESULT_HELP).build());
        options.add(OptionDescriptor.newBuilder(ARRAY_SORT_INHERITED, ARRAY_SORT_INHERITED_NAME).category(OptionCategory.USER).help(ARRAY_SORT_INHERITED_HELP).build());
        options.add(OptionDescriptor.newBuilder(SHARED_ARRAY_BUFFER, SHARED_ARRAY_BUFFER_NAME).category(OptionCategory.USER).help(SHARED_ARRAY_BUFFER_HELP).build());
        options.add(OptionDescriptor.newBuilder(ATOMICS, ATOMICS_NAME).category(OptionCategory.USER).help(ATOMICS_HELP).build());
        options.add(OptionDescriptor.newBuilder(V8_COMPATIBILITY_MODE, V8_COMPATIBILITY_MODE_NAME).category(OptionCategory.USER).help(V8_COMPATIBILITY_MODE_HELP).build());
        options.add(OptionDescriptor.newBuilder(V8_REALM_BUILTIN, V8_REALM_BUILTIN_NAME).category(OptionCategory.DEBUG).help(V8_REALM_BUILTIN_HELP).build());
        options.add(OptionDescriptor.newBuilder(NASHORN_COMPATIBILITY_MODE, NASHORN_COMPATIBILITY_MODE_NAME).category(OptionCategory.USER).help(NASHORN_COMPATIBILITY_MODE_HELP).build());
        options.add(OptionDescriptor.newBuilder(STACK_TRACE_LIMIT, STACK_TRACE_LIMIT_NAME).category(OptionCategory.USER).help(STACK_TRACE_LIMIT_HELP).build());
        options.add(OptionDescriptor.newBuilder(DEBUG_BUILTIN, DEBUG_BUILTIN_NAME).category(OptionCategory.DEBUG).help(DEBUG_BUILTIN_HELP).build());
        options.add(OptionDescriptor.newBuilder(DIRECT_BYTE_BUFFER, DIRECT_BYTE_BUFFER_NAME).category(OptionCategory.USER).help(DIRECT_BYTE_BUFFER_HELP).build());
        options.add(OptionDescriptor.newBuilder(PARSE_ONLY, PARSE_ONLY_NAME).category(OptionCategory.USER).help(PARSE_ONLY_HELP).build());
        options.add(OptionDescriptor.newBuilder(TIME_ZONE, TIME_ZONE_NAME).category(OptionCategory.USER).help(TIME_ZONE_HELP).build());
        options.add(OptionDescriptor.newBuilder(PRECISE_TIME, PRECISE_TIME_NAME).category(OptionCategory.USER).help(PRECISE_TIME_HELP).build());
        options.add(OptionDescriptor.newBuilder(AGENT_CAN_BLOCK, AGENT_CAN_BLOCK_NAME).category(OptionCategory.DEBUG).help(AGENT_CAN_BLOCK_HELP).build());
    }

    // check for options that are not on their default value.
    // in such case, we cannot use the pre-initialized context for faster startup
    public static boolean optionsAllowPreInitializedContext(JSRealm realm, Env env) {
        for (OptionKey<?> key : PREINIT_CONTEXT_OPTION_KEYS) {
            if (!realm.getEnv().getOptions().get(key).equals(env.getOptions().get(key))) {
                return false;
            }
        }
        return true;
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

    public boolean isPreciseTime() {
        return preciseTime;
    }

    public boolean isV8RealmBuiltin() {
        return v8RealmBuiltin;
    }

    public boolean canAgentBlock() {
        return agentCanBlock;
    }

    public int getStackTraceLimit() {
        return stackTraceLimit;
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
        hash = 53 * hash + (this.debug ? 1 : 0);
        hash = 53 * hash + (this.directByteBuffer ? 1 : 0);
        hash = 53 * hash + (this.parseOnly ? 1 : 0);
        hash = 53 * hash + (this.preciseTime ? 1 : 0);
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
        if (this.debug != other.debug) {
            return false;
        }
        if (this.directByteBuffer != other.directByteBuffer) {
            return false;
        }
        if (this.parseOnly != other.parseOnly) {
            return false;
        }
        if (this.preciseTime != other.preciseTime) {
            return false;
        }
        if (this.agentCanBlock != other.agentCanBlock) {
            return false;
        }
        return Objects.equals(this.parserOptions, other.parserOptions);
    }

}
