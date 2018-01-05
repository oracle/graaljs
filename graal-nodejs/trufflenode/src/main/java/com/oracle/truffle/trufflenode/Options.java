/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.trufflenode;

import static java.lang.Integer.max;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.graalvm.options.OptionCategory;
import org.graalvm.options.OptionDescriptor;
import org.graalvm.options.OptionDescriptors;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.Instrument;
import org.graalvm.polyglot.Language;

/**
 * Processing of options/command line arguments. Most of this class is copied from
 * {@code com.oracle.graalvm.launcher} and should be replaced by an appropriate API.
 */
public class Options {

    private Engine tempEngine;

    private boolean help;
    private boolean helpDebug;
    private boolean helpExpert;
    private boolean helpTools;
    private boolean helpLanguages;
    private boolean polyglot;
    private boolean exposeGC;

    private Map<String, String> polyglotOptions;

    private OptionCategory helpCategory;

    private VersionAction versionAction = VersionAction.None;

    protected enum VersionAction {
        None,
        PrintAndExit,
        PrintAndContinue
    }

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

    public static Options parseArguments(String[] args) {
        Options options = new Options();
        options.polyglotOptions = options.parsePolyglotOptions(args);
        if (options.runPolyglotAction()) {
            System.exit(0);
        }
        return options;
    }

    public boolean isPolyglot() {
        return polyglot;
    }

    public boolean isGCExposed() {
        return exposeGC;
    }

    public Map<String, String> getPolyglotOptions() {
        return polyglotOptions;
    }

    private Map<String, String> parsePolyglotOptions(String[] args) {
        Map<String, String> options = new HashMap<>();
        for (String arg : args) {
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
                arg = "--js.strict";
            }
            // Convert --harmony-sharedarraybuffer of V8 to --js.shared-array-buffer of Graal.js
            if ("harmony-sharedarraybuffer".equals(normalizedKey)) {
                arg = "--js.shared-array-buffer";
            }
            // Convert -h to --help
            if ("-h".equals(arg)) {
                arg = "--help";
            }
            if ("--polyglot".equals(arg)) {
                polyglot = true;
                continue;
            }
            if ("expose-gc".equals(normalizedKey)) {
                exposeGC = true;
                continue;
            }
            // Convert --inspect[=port] to --inspect[=port] plus --inspect.Suspend=false
            if ("inspect".equals(key)) {
                // Do not override port (from --inspect-brk=)
                if (value != null || !options.containsKey("inspect")) {
                    processPolyglotOption(options, arg, arg);
                }
                // Do not override inspect.Suspend=true
                if (!options.containsKey("inspect.Suspend")) {
                    processPolyglotOption(options, "--inspect.Suspend=false", arg);
                }
                continue;
            }
            // Convert --(inspect|debug)-brk[=port] to --inspect[=port] --inspect.WaitAttached=true
            if ("inspect-brk".equals(normalizedKey) || "debug-brk".equals(normalizedKey)) {
                // Do not override port (from --inspect=)
                if (value != null || !options.containsKey("inspect")) {
                    processPolyglotOption(options, "--inspect" + (value == null ? "" : "=" + value), arg);
                }
                // Do not override inspect.Suspend=true
                if (!options.containsKey("inspect.Suspend")) {
                    processPolyglotOption(options, "--inspect.Suspend=false", arg);
                }
                processPolyglotOption(options, "--inspect.WaitAttached=true", arg);
                continue;
            }
            if ("prof".equals(key)) {
                System.err.println("--prof option is not supported, use one of our profiling tools instead (use --help:tools for more details)");
                System.exit(1);
            }
            processPolyglotOption(options, arg, arg);
        }
        return options;
    }

    private void processPolyglotOption(Map<String, String> options, String arg, String originalArgument) {
        if (!parsePolyglotOption("js", options, arg)) {
            throw abortInvalidArgument(originalArgument, "Unrecognized argument: " + originalArgument + ". Use --help for usage instructions.", 1);
        }
    }

    private static void printInstrumentOptions(Engine engine, OptionCategory maxCategory) {
        Map<Instrument, List<PrintableOption>> instrumentsOptions = new HashMap<>();
        List<Instrument> instruments = sortedInstruments(engine);
        for (Instrument instrument : instruments) {
            List<PrintableOption> options = new ArrayList<>();
            for (OptionDescriptor descriptor : instrument.getOptions()) {
                if (descriptor.getCategory().ordinal() <= maxCategory.ordinal()) {
                    options.add(asPrintableOption(descriptor));
                }
            }
            if (!options.isEmpty()) {
                instrumentsOptions.put(instrument, options);
            }
        }
        if (!instrumentsOptions.isEmpty()) {
            System.out.println();
            System.out.println("Tool options:");
            for (Instrument instrument : instruments) {
                List<PrintableOption> options = instrumentsOptions.get(instrument);
                if (options != null) {
                    printOptions(options, "  " + instrument.getName() + ":", 4);
                }
            }
        }
    }

    private static void printLanguageOptions(Engine engine, OptionCategory maxCategory) {
        Map<Language, List<PrintableOption>> languagesOptions = new HashMap<>();
        List<Language> languages = sortedLanguages(engine);
        for (Language language : languages) {
            List<PrintableOption> options = new ArrayList<>();
            for (OptionDescriptor descriptor : language.getOptions()) {
                if (descriptor.getCategory().ordinal() <= maxCategory.ordinal()) {
                    options.add(asPrintableOption(descriptor));
                }
            }
            if (!options.isEmpty()) {
                languagesOptions.put(language, options);
            }
        }
        if (!languagesOptions.isEmpty()) {
            System.out.println();
            System.out.println("Language Options:");
            for (Language language : languages) {
                List<PrintableOption> options = languagesOptions.get(language);
                if (options != null) {
                    printOptions(options, "  " + language.getName() + ":", 4);
                }
            }
        }
    }

    protected boolean parsePolyglotOption(String defaultOptionPrefix, Map<String, String> options, String arg) {
        switch (arg) {
            case "--help":
                help = true;
                return true;
            case "--help:debug":
                helpDebug = true;
                return true;
            case "--help:expert":
                helpExpert = true;
                return true;
            case "--help:tools":
                helpTools = true;
                return true;
            case "--help:languages":
                helpLanguages = true;
                return true;
            case "--version":
                versionAction = VersionAction.PrintAndExit;
                return true;
            case "--show-version":
                versionAction = VersionAction.PrintAndContinue;
                return true;
            case "--polyglot":
            case "--jvm":
            case "--native":
                return false;
            default:
                // getLanguageId() or null?
                if (arg.length() <= 2 || !arg.startsWith("--")) {
                    return false;
                }
                int eqIdx = arg.indexOf('=');
                String key;
                String value;
                if (eqIdx < 0) {
                    key = arg.substring(2);
                    value = null;
                } else {
                    key = arg.substring(2, eqIdx);
                    value = arg.substring(eqIdx + 1);
                }

                if (value == null) {
                    value = "true";
                }
                int index = key.indexOf('.');
                String group = key;
                if (index >= 0) {
                    group = group.substring(0, index);
                }
                OptionDescriptor descriptor = findPolyglotOptionDescriptor(group, key);
                if (descriptor == null) {
                    if (defaultOptionPrefix != null) {
                        descriptor = findPolyglotOptionDescriptor(defaultOptionPrefix, defaultOptionPrefix + "." + key);
                    }
                    if (descriptor == null) {
                        return false;
                    }
                }
                try {
                    descriptor.getKey().getType().convert(value);
                } catch (IllegalArgumentException e) {
                    throw abort(String.format("Invalid argument %s specified. %s'", arg, e.getMessage()));
                }
                options.put(descriptor.getName(), value);
                return true;
        }
    }

    private OptionDescriptor findPolyglotOptionDescriptor(String group, String key) {
        OptionDescriptors descriptors = null;
        switch (group) {
            case "compiler":
            case "engine":
                descriptors = getTempEngine().getOptions();
                break;
            default:
                Engine engine = getTempEngine();
                if (engine.getLanguages().containsKey(group)) {
                    descriptors = engine.getLanguages().get(group).getOptions();
                } else if (engine.getInstruments().containsKey(group)) {
                    descriptors = engine.getInstruments().get(group).getOptions();
                }
                break;
        }
        if (descriptors == null) {
            return null;
        }
        return descriptors.get(key);
    }

    protected void setHelpCategory(OptionCategory helpCategory) {
        this.helpCategory = helpCategory;
    }

    private Engine getTempEngine() {
        if (tempEngine == null) {
            tempEngine = Engine.create();
        }
        return tempEngine;
    }

    protected final AbortException abortInvalidArgument(String argument, String message, int code) {
        Set<String> allArguments = collectAllArguments();
        int equalIndex = -1;
        String testString = argument;
        if ((equalIndex = argument.indexOf('=')) != -1) {
            testString = argument.substring(0, equalIndex);
        }

        List<String> matches = fuzzyMatch(allArguments, testString, 0.7f);
        if (matches.isEmpty()) {
            // try even harder
            matches = fuzzyMatch(allArguments, testString, 0.5f);
        }

        if (message != null) {
            System.err.println("ERROR: " + message);
        }
        if (!matches.isEmpty()) {
            System.err.println("Did you mean one of the following arguments?");
            for (String match : matches) {
                System.err.println("      " + match);
            }
        }
        System.exit(code);
        throw new AbortException(message);
    }

    private Set<String> collectAllArguments() {
        Engine engine = getTempEngine();
        Set<String> options = new LinkedHashSet<>();
        collectArguments(options);
        options.add("--polyglot");
        options.add("--native");
        options.add("--jvm");
        options.add("--help");
        options.add("--help:languages");
        options.add("--help:tools");
        options.add("--help:expert");
        options.add("--version");
        options.add("--show-version");
        if (helpExpert || helpDebug) {
            options.add("--help:debug");
        }
        addOptions(engine.getOptions(), options);
        for (Language language : engine.getLanguages().values()) {
            addOptions(language.getOptions(), options);
        }
        for (Instrument instrument : engine.getInstruments().values()) {
            addOptions(instrument.getOptions(), options);
        }
        return options;
    }

    private static void addOptions(OptionDescriptors descriptors, Set<String> target) {
        for (OptionDescriptor descriptor : descriptors) {
            target.add("--" + descriptor.getName());
        }
    }

    /**
     * Returns the set of options that fuzzy match a given option name.
     */
    static List<String> fuzzyMatch(Set<String> arguments, String argument, float threshold) {
        List<String> matches = new ArrayList<>();
        for (String arg : arguments) {
            float score = stringSimiliarity(arg, argument);
            if (score >= threshold) {
                matches.add(arg);
            }
        }
        return matches;
    }

    /**
     * Compute string similarity based on Dice's coefficient.
     *
     * Ported from str_similar() in globals.cpp.
     */
    private static float stringSimiliarity(String str1, String str2) {
        int hit = 0;
        for (int i = 0; i < str1.length() - 1; ++i) {
            for (int j = 0; j < str2.length() - 1; ++j) {
                if ((str1.charAt(i) == str2.charAt(j)) && (str1.charAt(i + 1) == str2.charAt(j + 1))) {
                    ++hit;
                    break;
                }
            }
        }
        return 2.0f * hit / (str1.length() + str2.length());
    }

    protected static List<Language> sortedLanguages(Engine engine) {
        List<Language> languages = new ArrayList<>(engine.getLanguages().values());
        languages.sort(Comparator.comparing(Language::getId));
        return languages;
    }

    protected static List<Instrument> sortedInstruments(Engine engine) {
        List<Instrument> instruments = new ArrayList<>(engine.getInstruments().values());
        instruments.sort(Comparator.comparing(Instrument::getId));
        return instruments;
    }

    protected void collectArguments(Set<String> options) {
        options.add("--eval");
        options.add("--print");
        options.add("--check");
        options.add("--interactive");
        options.add("--require");
    }

    protected void printHelp() {
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

    protected void printVersion() {
        printPolyglotVersions();
    }

    protected static void printPolyglotVersions() {
        Engine engine = Engine.create();
        System.out.println("GraalVM Polyglot Engine Version " + engine.getVersion());
        printLanguages(engine, true);
        printInstruments(engine, true);
    }

    protected final boolean runPolyglotAction() {
        OptionCategory maxCategory = helpDebug ? OptionCategory.DEBUG : (helpExpert ? OptionCategory.EXPERT : (helpCategory != null ? helpCategory : OptionCategory.USER));

        switch (versionAction) {
            case PrintAndContinue:
                printVersion();
                return false;
            case PrintAndExit:
                printVersion();
                return true;
            case None:
                break;
        }
        boolean printDefaultHelp = helpCategory != null || help || ((helpExpert || helpDebug) && !helpTools && !helpLanguages);
        Engine tmpEngine = null;
        if (printDefaultHelp) {
            printHelp();
            // @formatter:off
            System.out.println();
            System.out.println("Runtime Options:");
            printOption("--polyglot",                   "Run with all other guest languages accessible.");
            printOption("--native",                     "Run using the native launcher with limited Java access (default).");
            printOption("--native.[option]",            "Pass options to the native image; for example, '--native.XX:+PrintGC'. To see available options, use '--native.help'.");
            printOption("--jvm",                        "Run on the Java Virtual Machine with Java access.");
            printOption("--jvm.[option]",               "Pass options to the JVM; for example, '--jvm.classpath=myapp.jar'. To see available options. use '--jvm.help'.");
            printOption("--help",                       "Print this help message.");
            printOption("--help:languages",             "Print options for all installed languages.");
            printOption("--help:tools",                 "Print options for all installed tools.");
            printOption("--help:expert",                "Print additional engine options for experts.");
            if (helpExpert || helpDebug) {
                printOption("--help:debug",             "Print additional engine options for debugging.");
            }
            printOption("--version",                    "Print version information and exit.");
            printOption("--show-version",               "Print version information and continue execution.");
            // @formatter:on
            if (tmpEngine == null) {
                tmpEngine = Engine.create();
            }
            List<PrintableOption> engineOptions = new ArrayList<>();
            for (OptionDescriptor descriptor : tmpEngine.getOptions()) {
                if (!descriptor.getName().startsWith("engine.") && !descriptor.getName().startsWith("compiler.")) {
                    continue;
                }
                if (descriptor.getCategory().ordinal() <= maxCategory.ordinal()) {
                    engineOptions.add(asPrintableOption(descriptor));
                }
            }
            if (!engineOptions.isEmpty()) {
                printOptions(engineOptions, "Engine options:", 2);
            }
        }

        if (helpLanguages) {
            if (tmpEngine == null) {
                tmpEngine = Engine.create();
            }
            printLanguageOptions(tmpEngine, maxCategory);
        }

        if (helpTools) {
            if (tmpEngine == null) {
                tmpEngine = Engine.create();
            }
            printInstrumentOptions(tmpEngine, maxCategory);
        }

        if (printDefaultHelp || helpLanguages || helpTools) {
            System.out.println("\nSee http://www.oracle.com/technetwork/oracle-labs/program-languages/overview/index.html for more information.");
            return true;
        }

        return false;
    }

    protected static void printLanguages(Engine engine, boolean printWhenEmpty) {
        if (engine.getLanguages().isEmpty()) {
            if (printWhenEmpty) {
                System.out.println("  Installed Languages: none");
            }
        } else {
            System.out.println("  Installed Languages:");
            List<Language> languages = new ArrayList<>(engine.getLanguages().size());
            int nameLength = 0;
            for (Language language : engine.getLanguages().values()) {
                languages.add(language);
                nameLength = max(nameLength, language.getName().length());
            }
            languages.sort(Comparator.comparing(Language::getId));
            String langFormat = "    %-" + nameLength + "s version %s%n";
            for (Language language : languages) {
                String version = language.getVersion();
                if (version == null || version.length() == 0) {
                    version = "";
                }
                System.out.printf(langFormat, language.getName().isEmpty() ? "Unnamed" : language.getName(), version);
            }
        }
    }

    protected static void printInstruments(Engine engine, boolean printWhenEmpty) {
        if (engine.getInstruments().isEmpty()) {
            if (printWhenEmpty) {
                System.out.println("  Installed Tools: none");
            }
        } else {
            System.out.println("  Installed Tools:");
            List<Instrument> instruments = sortedInstruments(engine);
            int nameLength = 0;
            for (Instrument instrument : instruments) {
                nameLength = max(nameLength, instrument.getName().length());
            }
            String instrumentFormat = "    %-" + nameLength + "s version %s%n";
            for (Instrument instrument : instruments) {
                String version = instrument.getVersion();
                if (version == null || version.length() == 0) {
                    version = "";
                }
                System.out.printf(instrumentFormat, instrument.getName().isEmpty() ? "Unnamed" : instrument.getName(), version);
            }
        }
    }

    protected static PrintableOption asPrintableOption(OptionDescriptor descriptor) {
        StringBuilder key = new StringBuilder("--");
        key.append(descriptor.getName());
        Object defaultValue = descriptor.getKey().getDefaultValue();
        if (defaultValue instanceof Boolean && defaultValue == Boolean.FALSE) {
            // nothing to print
        } else {
            key.append("=<");
            key.append(descriptor.getKey().getType().getName());
            key.append(">");
        }
        return new PrintableOption(key.toString(), descriptor.getHelp());
    }

    protected static void printOption(String option, String description) {
        printOption(option, description, 2);
    }

    protected static void printOption(String option, String description, int indentation) {
        StringBuilder indent = new StringBuilder(indentation);
        for (int i = 0; i < indentation; i++) {
            indent.append(' ');
        }
        String desc = description != null ? description : "";
        if (option.length() >= 45 && description != null) {
            System.out.println(String.format("%s%s%n%s%-45s%s", indent, option, indent, "", desc));
        } else {
            System.out.println(String.format("%s%-45s%s", indent, option, desc));
        }
    }

    private static final class PrintableOption implements Comparable<PrintableOption> {
        final String option;
        final String description;

        private PrintableOption(String option, String description) {
            this.option = option;
            this.description = description;
        }

        @Override
        public int compareTo(PrintableOption o) {
            return this.option.compareTo(o.option);
        }
    }

    private static void printOptions(List<PrintableOption> options, String title, int indentation) {
        Collections.sort(options);
        System.out.println(title);
        for (PrintableOption option : options) {
            printOption(option, indentation);
        }
    }

    protected static void printOption(PrintableOption option, int indentation) {
        printOption(option.option, option.description, indentation);
    }

    private static class AbortException extends RuntimeException {
        static final long serialVersionUID = 4681646279864737876L;

        AbortException(String message) {
            super(message, null);
        }

        @SuppressWarnings("sync-override")
        @Override
        public final Throwable fillInStackTrace() {
            return null;
        }
    }

    protected AbortException exit() {
        return abort(null, 0);
    }

    protected AbortException abort(String message) {
        return abort(message, 1);
    }

    protected AbortException abort(String message, int exitCode) {
        if (message != null) {
            System.err.println("ERROR: " + message);
        }
        System.exit(exitCode);
        throw new AbortException(message);
    }

}
