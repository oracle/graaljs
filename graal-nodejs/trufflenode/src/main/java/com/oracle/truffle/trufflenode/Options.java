/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.trufflenode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.oracle.truffle.js.runtime.AbstractJavaScriptLanguage;
import org.graalvm.launcher.AbstractLanguageLauncher;
import org.graalvm.options.OptionCategory;
import org.graalvm.polyglot.Context;

public final class Options extends AbstractLanguageLauncher {
    private Context.Builder contextBuilder;
    private boolean exposeGC;

    // Options that should not be passed to polyglot engine (they are processed
    // elsewhere or can be ignored without almost any harm).
    private static final Set<String> IGNORED_OPTIONS = new HashSet<>(Arrays.asList(new String[]{
                    "debug-code",
                    "es-staging",
                    "expose-debug-as",
                    "expose-natives-as",
                    "harmony",
                    "harmony-default-parameters",
                    "harmony-proxies",
                    "harmony-shipping",
                    "lazy",
                    "log-timer-events",
                    "nolazy",
                    "nouse-idle-notification",
                    "stack-size",
                    "use_idle_notification"
    }));

    private Options() {
    }

    public static Options parseArguments(String[] args) {
        Options options = new Options();
        options.launch(args);
        return options;
    }

    public Context.Builder getContextBuilder() {
        return contextBuilder;
    }

    public boolean isGCExposed() {
        return exposeGC;
    }

    @Override
    protected List<String> preprocessArguments(List<String> arguments, Map<String, String> polyglotOptions) {
        List<String> unprocessedArguments = new ArrayList<>();
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
            // Convert -h to --help
            if ("-h".equals(arg)) {
                unprocessedArguments.add("--help");
                continue;
            }
            if ("expose-gc".equals(normalizedKey)) {
                exposeGC = true;
                continue;
            }
            // Convert --inspect[=port] to --inspect[=port] plus --inspect.Suspend=false
            if ("inspect".equals(key)) {
                // Do not override port (from --inspect-brk=)
                if (value != null || !polyglotOptions.containsKey("inspect")) {
                    unprocessedArguments.add(arg);
                }
                // Do not override inspect.Suspend=true
                if (!polyglotOptions.containsKey("inspect.Suspend")) {
                    unprocessedArguments.add("--inspect.Suspend=false");
                }
                continue;
            }
            // Convert --(inspect|debug)-brk[=port] to --inspect[=port] --inspect.WaitAttached=true
            if ("inspect-brk".equals(normalizedKey) || "debug-brk".equals(normalizedKey)) {
                // Do not override port (from --inspect=)
                if (value != null || !polyglotOptions.containsKey("inspect")) {
                    unprocessedArguments.add("--inspect" + (value == null ? "" : "=" + value));
                }
                // Do not override inspect.Suspend=true
                if (!polyglotOptions.containsKey("inspect.Suspend")) {
                    unprocessedArguments.add("--inspect.Suspend=false");
                }
                unprocessedArguments.add("--inspect.WaitAttached=true");
                continue;
            }
            if ("prof".equals(key)) {
                System.err.println("--prof option is not supported, use one of our profiling tools instead (use --help:tools for more details)");
                System.exit(1);
            }
            unprocessedArguments.add(arg);
        }
        return unprocessedArguments;
    }

    @Override
    protected void launch(Context.Builder builder) {
        this.contextBuilder = builder;
    }

    @Override
    protected String getLanguageId() {
        return AbstractJavaScriptLanguage.ID;
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
        printOption("-e, --eval script",     "evaluate script");
        printOption("-p, --print",           "evaluate script and print result");
        printOption("-c, --check",           "syntax check script without executing");
        printOption("-i, --interactive",     "always enter the REPL even if stdin does not appear to be a terminal");
        printOption("-r, --require",         "module to preload (option can be repeated)");
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

}
