/*
 * Copyright (c) 2017, 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.shell;

import static com.oracle.truffle.js.shell.JSLauncher.PreprocessResult.Consumed;
import static java.lang.Math.abs;

import java.util.Map;

import org.graalvm.options.OptionCategory;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;

public class RepeatingLauncher extends JSLauncher {
    private int engines = 1;
    private int runs = 1;

    @Override
    protected PreprocessResult preprocessArgument(String argument, String value, Map<String, String> polyglotOptions) {
        if (argument.equals("engines")) {
            engines = parsePositiveInteger(argument, value);
            return Consumed;
        }
        if (argument.equals("runs")) {
            runs = parsePositiveInteger(argument, value);
            return Consumed;
        }
        return super.preprocessArgument(argument, value, polyglotOptions);
    }

    protected int parsePositiveInteger(String optName, String optValue) {
        try {
            int val = Integer.parseInt(optValue);
            if (val >= 0) {
                return val;
            }
        } catch (NumberFormatException e) {
            // fall through
        }
        throw abort("The value of '" + optName + "' must be a positive integer");
    }

    @Override
    protected int executeScripts(Context.Builder contextBuilder) {
        int result = 0;
        for (int i = 0; i < engines; i++) {
            int newResult = super.executeScripts(contextBuilder);
            if (abs(newResult) > abs(result)) {
                result = newResult;
            }
        }
        return result;
    }

    @Override
    Source[] parseSources() {
        Source[] originalSources = super.parseSources();
        if (runs == 1) {
            return originalSources;
        }
        Source[] newSources = new Source[originalSources.length * runs];
        for (int i = 0; i < originalSources.length; i++) {
            for (int j = 0; j < runs; j++) {
                newSources[i * runs + j] = originalSources[i];
            }
        }
        return newSources;
    }

    @Override
    protected void validateArguments(Map<String, String> polyglotOptions) {
        super.validateArguments(polyglotOptions);
        if (!hasSources()) {
            if (engines > 1) {
                throw abort("Can not use --engines with the REPL");
            }
            if (runs > 1) {
                throw abort("Can not use --runs with the REPL");
            }
        }
    }

    @Override
    protected void printHelp(OptionCategory maxCategory) {
        super.printHelp(maxCategory);
        System.out.println("\nCustom developer options:");
        printOption("--runs N", "run scripts N times");
        printOption("--engines N", "load scripts in N different engines");
    }
}
