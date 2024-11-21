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
package com.oracle.truffle.trufflenode;

import java.io.OutputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import org.graalvm.launcher.AbstractLanguageLauncher;
import org.graalvm.options.OptionCategory;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;

import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.runtime.JSConfig;

public final class Options {
    private final Context.Builder contextBuilder;
    private final boolean exposeGC;
    private final boolean unsafeWasmMemory;
    private final boolean auxEngineCacheMode;

    private Options(Context.Builder contextBuilder, boolean exposeGC, boolean unsafeWasmMemory, boolean auxEngineCacheMode) {
        this.contextBuilder = contextBuilder;
        this.exposeGC = exposeGC;
        this.unsafeWasmMemory = unsafeWasmMemory;
        this.auxEngineCacheMode = auxEngineCacheMode;
    }

    public static Options parseArguments(String[] args) throws Exception {
        Function<String[], Object[]> parser;
        try {
            parser = new OptionsParser();
        } catch (NoClassDefFoundError e) {
            if (JSConfig.SubstrateVM) {
                throw e;
            } else {
                // AbstractLanguageLauncher is not accessible through
                // language class loader (that loaded this class) in GraalVM (legacy build)
                Class<Function<String[], Object[]>> clazz = loadOptionsParser();
                parser = clazz.getDeclaredConstructor().newInstance();
            }
        }
        Object[] result = parser.apply(args);
        return new Options((Context.Builder) result[0], (Boolean) result[1], (Boolean) result[2], (Boolean) result[3]);
    }

    @SuppressWarnings("unchecked")
    private static Class<Function<String[], Object[]>> loadOptionsParser() throws Exception {
        String javaHome = System.getProperty("java.home");
        Path truffleNodePath = Optional.ofNullable(System.getenv("TRUFFLENODE_JAR_PATH")).map(Path::of).orElseGet(
                        () -> Path.of(javaHome, "languages", "nodejs", "trufflenode.jar"));
        Path launcherCommonPath = Optional.ofNullable(System.getenv("LAUNCHER_COMMON_JAR_PATH")).map(Path::of).orElseGet(
                        () -> Path.of(javaHome, "lib", "graalvm", "launcher-common.jar"));
        Path jlinePath = Path.of(javaHome, "lib", "graalvm", "jline3.jar");
        URL[] urls = new URL[]{
                        truffleNodePath.toUri().toURL(),
                        launcherCommonPath.toUri().toURL(),
                        jlinePath.toUri().toURL(),
        };
        ClassLoader loader = new URLClassLoader(urls, ClassLoader.getSystemClassLoader());
        return (Class<Function<String[], Object[]>>) loader.loadClass("com.oracle.truffle.trufflenode.Options$OptionsParser");
    }

    public Context.Builder getContextBuilder() {
        return contextBuilder;
    }

    public boolean isGCExposed() {
        return exposeGC;
    }

    public boolean isUnsafeWasmMemory() {
        return unsafeWasmMemory;
    }

    public boolean isAuxEngineCacheMode() {
        return auxEngineCacheMode;
    }

    public static class OptionsParser extends AbstractLanguageLauncher implements Function<String[], Object[]> {
        private static final String INSPECT = "inspect";
        private static final String INSPECT_SUSPEND = "inspect.Suspend";
        private static final String INSPECT_WAIT_ATTACHED = "inspect.WaitAttached";
        private static final String WASM_LANGUAGE_ID = "wasm";

        private Context.Builder contextBuilder;
        private boolean exposeGC;
        private boolean polyglot;
        private boolean unsafeWasmMemory;
        private boolean auxEngineCacheMode;
        private boolean wasmEnabled;

        private static final Set<String> AUX_CACHE_OPTIONS = Set.of("engine.Cache",
                        "engine.CacheLoad",
                        "engine.CacheStore");

        // Options that should not be passed to polyglot engine (they are processed
        // elsewhere or can be ignored without almost any harm).
        private static final Set<String> IGNORED_OPTIONS = Set.of(new String[]{
                        "debug-code",
                        "es-staging",
                        "experimental-modules",
                        "expose-debug-as",
                        "expose-internals",
                        "expose-natives-as",
                        "gc-global",
                        "gc-interval",
                        "harmony",
                        "harmony-bigint",
                        "harmony-default-parameters",
                        "harmony-dynamic-import",
                        "harmony-import-meta",
                        "harmony-proxies",
                        "harmony-shipping",
                        "harmony-weak-refs",
                        "jitless",
                        "lazy",
                        "log-timer-events",
                        "no-concurrent-array-buffer-freeing",
                        "no-concurrent-array-buffer-sweeping",
                        "no-freeze-flags-after-init",
                        "no-harmony-top-level-await",
                        "nolazy",
                        "nouse-idle-notification",
                        "rehash-snapshot",
                        "stack-size",
                        "trace-gc",
                        "use-idle-notification"
        });

        @Override
        public Object[] apply(String[] args) {
            launch(filterArguments(args));
            if (contextBuilder == null) {
                // launch(Context.Builder) was not called (i.e. help was printed) => exit
                System.exit(0);
            }
            return new Object[]{contextBuilder, exposeGC, unsafeWasmMemory, auxEngineCacheMode};
        }

        private String[] filterArguments(String[] args) {
            List<String> filtered = new ArrayList<>();
            for (String arg : args) {
                if ("--polyglot".equals(arg)) {
                    polyglot = true;
                } else {
                    filtered.add(arg);
                }
            }
            return filtered.toArray(new String[filtered.size()]);
        }

        @Override
        protected List<String> preprocessArguments(List<String> arguments, Map<String, String> polyglotOptions) {
            // Node.js-specific defaults, may be overridden by command line arguments.
            polyglotOptions.put("js.print", "false");
            polyglotOptions.put("js.string-length-limit", Integer.toString((1 << 29) - 24)); // v8::String::kMaxLength

            List<String> unprocessedArguments = new ArrayList<>();
            Boolean optWebAssembly = null;
            Boolean optUnsafeWasmMemory = null;
            for (String arg : arguments) {
                String key = "";
                String value = null;
                if (arg.startsWith("--")) {
                    int idx = arg.indexOf('=');
                    if (idx == -1) {
                        key = arg.substring(2);
                    } else {
                        key = arg.substring(2, idx);
                        value = arg.substring(idx + 1);
                    }
                }
                String normalizedKey = key.replace('_', '-');
                if (AUX_CACHE_OPTIONS.contains(normalizedKey)) {
                    auxEngineCacheMode = true;
                }
                if (IGNORED_OPTIONS.contains(normalizedKey)) {
                    continue;
                }
                // Convert --use-strict of V8 to --js.strict of Graal.js
                if ("use-strict".equals(normalizedKey)) {
                    polyglotOptions.put("js.strict", "true");
                    continue;
                }
                // Convert --harmony-sharedarraybuffer of V8 to --js.shared-array-buffer of Graal.js
                if ("harmony-sharedarraybuffer".equals(normalizedKey)) {
                    polyglotOptions.put("js.shared-array-buffer", "true");
                    continue;
                }
                if ("no-harmony-sharedarraybuffer".equals(normalizedKey)) {
                    polyglotOptions.put("js.shared-array-buffer", "false");
                    continue;
                }
                if ("no-harmony-atomics".equals(normalizedKey)) {
                    polyglotOptions.put("js.atomics", "false");
                    continue;
                }
                if ("harmony-top-level-await".equals(normalizedKey)) {
                    polyglotOptions.put("js.top-level-await", "true");
                    continue;
                }
                if ("harmony-import-attributes".equals(normalizedKey)) {
                    polyglotOptions.put("js.import-attributes", "true");
                    continue;
                }
                if ("no-harmony-import-assertions".equals(normalizedKey)) {
                    polyglotOptions.put("js.import-assertions", "false");
                    continue;
                }
                if ("harmony-import-assertions".equals(normalizedKey)) {
                    polyglotOptions.put("js.import-assertions", "true");
                    continue;
                }
                if ("harmony-shadow-realm".equals(normalizedKey)) {
                    polyglotOptions.put("js.shadow-realm", "true");
                    continue;
                }
                if ("disallow-code-generation-from-strings".equals(normalizedKey)) {
                    polyglotOptions.put("js.disable-eval", "true");
                    continue;
                }
                // Convert -h to --help
                if ("-h".equals(arg)) {
                    unprocessedArguments.add("--help");
                    continue;
                }
                if ("expose-gc".equals(normalizedKey)) {
                    exposeGC = true;
                    continue;
                }
                if (INSPECT_SUSPEND.equals(key)) {
                    polyglotOptions.put(INSPECT_SUSPEND, valueOrTrue(value));
                    continue;
                }
                // Convert --inspect[=port] to --inspect[=port] plus --inspect.Suspend=false
                if (INSPECT.equals(key)) {
                    // Do not override port (from --inspect-brk=)
                    if (value != null || !polyglotOptions.containsKey(INSPECT)) {
                        polyglotOptions.put(key, valueOrTrue(value));
                    }
                    // Do not override inspect.Suspend=true
                    if (!polyglotOptions.containsKey(INSPECT_SUSPEND)) {
                        polyglotOptions.put(INSPECT_SUSPEND, "false");
                    }
                    continue;
                }
                // Convert --(inspect|debug)-brk[=port] to --inspect[=port]
                // --inspect.WaitAttached=true
                if ("inspect-brk".equals(normalizedKey) || "debug-brk".equals(normalizedKey) || "inspect-brk-node".equals(normalizedKey)) {
                    // Do not override port (from --inspect=)
                    if (value != null || !polyglotOptions.containsKey(INSPECT)) {
                        polyglotOptions.put(INSPECT, valueOrTrue(value));
                    }
                    // Do not override inspect.Suspend=true
                    if (!polyglotOptions.containsKey(INSPECT_SUSPEND)) {
                        polyglotOptions.put(INSPECT_SUSPEND, "false");
                    }
                    polyglotOptions.put(INSPECT_WAIT_ATTACHED, "true");
                    continue;
                }
                if ("prof".equals(key)) {
                    System.err.println("--prof option is not supported, use one of our profiling tools instead (use --help:tools for more details)");
                    System.exit(1);
                }
                if ("stack-trace-limit".equals(key)) {
                    polyglotOptions.put("js.stack-trace-limit", value);
                    continue;
                }
                if (("js.webassembly".equals(key) || "webassembly".equals(key))) {
                    optWebAssembly = (value == null || "true".equals(value));
                } else if ("wasm.UseUnsafeMemory".equals(key)) {
                    optUnsafeWasmMemory = (value == null || "true".equals(value));
                }
                unprocessedArguments.add(arg);
            }
            if (optWebAssembly != Boolean.FALSE) {
                // WebAssembly is enabled by default, if available.
                if (isWasmAvailable()) {
                    wasmEnabled = true;
                    if (optWebAssembly == null) {
                        optWebAssembly = Boolean.TRUE;
                        polyglotOptions.put("js.webassembly", "true");
                    }
                    if (optWebAssembly) {
                        polyglotOptions.put("wasm.Threads", "true");
                        if (optUnsafeWasmMemory == null) {
                            optUnsafeWasmMemory = Boolean.TRUE;
                            polyglotOptions.put("wasm.UseUnsafeMemory", "true");
                        }
                    }
                }
                if (optUnsafeWasmMemory == Boolean.TRUE) {
                    unsafeWasmMemory = true;
                }
            }
            return unprocessedArguments;
        }

        private static String valueOrTrue(String value) {
            return (value == null) ? "true" : value;
        }

        @Override
        protected void launch(Context.Builder builder) {
            this.contextBuilder = builder;
        }

        @Override
        protected String getLanguageId() {
            return JavaScriptLanguage.ID;
        }

        @Override
        protected void collectArguments(Set<String> options) {
            options.add("--eval");
            options.add("--print");
            options.add("--check");
            options.add("--interactive");
            options.add("--require");
        }

        @Override
        protected void printHelp(OptionCategory maxCategory) {
            // @formatter:off
            System.out.println();
            System.out.println("Usage: node [options] [ -e script | script.js ] [arguments]\n");
            System.out.println("Basic Options:");
            printOption("-v, --version",         "print Node.js version");
            printOption("-e, --eval=...",        "evaluate script");
            printOption("-p, --print [...]",     "evaluate script and print result");
            printOption("-c, --check",           "syntax check script without executing");
            printOption("-i, --interactive",     "always enter the REPL even if stdin does not appear to be a terminal");
            printOption("-r, --require=...",     "CommonJS module to preload (option can be repeated)");
            printOption("--inspect[=port]",      "activate inspector on port (overrides options of Chrome Inspector)");
            printOption("--inspect-brk[=port]",  "activate inspector on port and break at start of user script (overrides options of Chrome Inspector)");
            // @formatter:on
        }

        private static void printOption(String option, String description) {
            String opt;
            if (option.length() >= 22) {
                System.out.println(String.format("%s%s", "  ", option));
                opt = "";
            } else {
                opt = option;
            }
            System.out.println(String.format("  %-22s%s", opt, description));
        }

        @Override
        protected String[] getDefaultLanguages() {
            if (polyglot) {
                return new String[0];
            } else if (wasmEnabled) {
                assert isWasmAvailable() : "wasm not available";
                return new String[]{getLanguageId(), WASM_LANGUAGE_ID};
            } else {
                return super.getDefaultLanguages();
            }
        }

        private static boolean isWasmAvailable() {
            return isLanguageAvailable(WASM_LANGUAGE_ID);
        }

        private static boolean isLanguageAvailable(String languageId) {
            try (Engine tempEngine = Engine.newBuilder().useSystemProperties(false).//
                            out(OutputStream.nullOutputStream()).//
                            err(OutputStream.nullOutputStream()).//
                            option("engine.WarnInterpreterOnly", "false").//
                            build()) {
                return tempEngine.getLanguages().containsKey(languageId);
            }
        }
    }

}
