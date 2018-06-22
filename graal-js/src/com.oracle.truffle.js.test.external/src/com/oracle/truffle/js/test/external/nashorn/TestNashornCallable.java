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
package com.oracle.truffle.js.test.external.nashorn;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;

import com.oracle.truffle.js.parser.GraalJSParserOptions;
import com.oracle.truffle.js.parser.JavaScriptLanguage;
import com.oracle.truffle.js.runtime.JSContextOptions;
import com.oracle.truffle.js.runtime.JSTruffleOptions;
import com.oracle.truffle.js.test.external.suite.TestCallable;
import com.oracle.truffle.js.test.external.suite.TestSuite;

public class TestNashornCallable extends TestCallable {

    private String resultOut;
    private String resultErr;

    // Options read from test file
    private boolean forceStrictMode;
    private String timezone;
    private String locale;
    private boolean scripting;
    private boolean syntaxExtensions;
    private boolean constAsVar;
    private boolean languageES6;
    private boolean functionStatementError;
    @SuppressWarnings("unused") private boolean functionStatementWarning;
    private boolean parseOnly;
    private List<String> arguments = new ArrayList<>();
    private static final int TESTNASHORN_ECMASCRIPT_VERSION = 5;

    public TestNashornCallable(TestSuite suite, Source[] harnessCode, Source scriptSource, File scriptFile) {
        super(suite, harnessCode, scriptSource, scriptFile, TESTNASHORN_ECMASCRIPT_VERSION, Collections.emptyMap());
    }

    @Override
    public Object call() throws Exception {
        checkOptions(getScriptFileContent());

        int ecmaScriptVersion = languageES6 ? 6 : TESTNASHORN_ECMASCRIPT_VERSION;
        assert ecmaScriptVersion <= JSTruffleOptions.MaxECMAScriptVersion;

        Context.Builder contextBuilder = Context.newBuilder(JavaScriptLanguage.ID);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        contextBuilder.out(out);
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        contextBuilder.err(err);
        contextBuilder.allowHostAccess(true);

        contextBuilder.option(JSContextOptions.ECMASCRIPT_VERSION_NAME, Integer.toString(ecmaScriptVersion));
        contextBuilder.option(GraalJSParserOptions.STRICT_NAME, Boolean.toString(forceStrictMode));
        contextBuilder.option(GraalJSParserOptions.SCRIPTING_NAME, Boolean.toString(scripting));
        contextBuilder.option(GraalJSParserOptions.SYNTAX_EXTENSIONS_NAME, Boolean.toString(syntaxExtensions));
        contextBuilder.option(GraalJSParserOptions.CONST_AS_VAR_NAME, Boolean.toString(constAsVar));
        contextBuilder.option(GraalJSParserOptions.FUNCTION_STATEMENT_ERROR_NAME, Boolean.toString(functionStatementError));
        contextBuilder.option(JSContextOptions.PARSE_ONLY_NAME, Boolean.toString(parseOnly));
        contextBuilder.option(JSContextOptions.NASHORN_COMPATIBILITY_MODE_NAME, "true");
        if (!arguments.isEmpty()) {
            contextBuilder.arguments(JavaScriptLanguage.ID, arguments.toArray(new String[0]));
        }
        if (timezone != null) {
            contextBuilder.option(JSContextOptions.TIME_ZONE_NAME, timezone);
        }

        try (Context context = contextBuilder.build()) {
            for (Source source : prequelSources) {
                context.eval(JavaScriptLanguage.ID, source.getCharacters());
            }
            return context.eval(org.graalvm.polyglot.Source.newBuilder(JavaScriptLanguage.ID, getScriptFile()).name(getScriptFile().getPath()).build());
        } finally {
            resultOut = out.toString();
            resultErr = err.toString();
        }
    }

    private void checkOptions(String scriptCode) {
        forceStrictMode = scriptCode.contains("* @run/fail --strict-mode") || scriptCode.contains("* @option -strict");
        int timezoneIndex = scriptCode.indexOf("@option -timezone=");
        if (timezoneIndex > 0) {
            int timezoneEndIndex = scriptCode.indexOf("\n", timezoneIndex);
            timezone = scriptCode.substring(timezoneIndex + 18, timezoneEndIndex);
        }
        int localeIndex = scriptCode.indexOf("@option --locale=");
        if (localeIndex > 0) {
            int localeEndIndex = scriptCode.indexOf("\n", localeIndex);
            locale = scriptCode.substring(localeIndex + 17, localeEndIndex);
            Locale loc = Locale.forLanguageTag(locale);
            Locale.setDefault(loc);
        }
        int argumentIndex = scriptCode.indexOf("@argument");
        while (argumentIndex >= 0) {
            int argumentStartIndex = argumentIndex + "@argument".length() + 1;
            int argumentEndIndex = findArgumentEnd(scriptCode, argumentStartIndex, true);
            String argument = scriptCode.substring(argumentStartIndex, argumentEndIndex);
            if (argument.startsWith("\"") && argument.endsWith("\"")) {
                argument = argument.substring(1, argument.length() - 1);
            }
            arguments.add(argument);
            argumentIndex = scriptCode.indexOf("@argument", argumentEndIndex);
        }

        // @option -Dnashorn.test.foo=bar
        int syspropIndex = scriptCode.indexOf("@option -D");
        while (syspropIndex >= 0) {
            int syspropEndIndex = findArgumentEnd(scriptCode, syspropIndex + 10, false);
            String sysprop = scriptCode.substring(syspropIndex + 10, syspropEndIndex);
            int eqpos = sysprop.indexOf('=');
            if (eqpos != -1) {
                System.setProperty(sysprop.substring(0, eqpos), sysprop.substring(eqpos + 1));
            } else {
                System.setProperty(sysprop, "");
            }
            syspropIndex = scriptCode.indexOf("@option -D", syspropEndIndex);
        }

        scripting = scriptCode.contains("@option -scripting") || scriptCode.startsWith("#");
        syntaxExtensions = !(scriptCode.contains("@option --no-syntax-extensions") || scriptCode.contains("@option -nse"));
        constAsVar = scriptCode.contains("@option --const-as-var");
        languageES6 = scriptCode.contains("@option --language=es6");
        functionStatementError = scriptCode.contains("@option --function-statement-error");
        functionStatementWarning = scriptCode.contains("@option --function-statement-warning");
        parseOnly = scriptCode.contains("@option --parse-only");

        // not supported, affecting error/NASHORN-214.js
        // earlyLValueError = !scriptCode.contains("--early-lvalue-error=false");
    }

    private static int findArgumentEnd(String scriptCode, int argumentIndex, boolean ignoreSpaces) {
        int lineEndIndex = scriptCode.indexOf("\n", argumentIndex);
        int spaceIndex = ignoreSpaces ? -1 : scriptCode.indexOf(" ", argumentIndex);
        return (spaceIndex >= 0 && spaceIndex < lineEndIndex) ? spaceIndex : lineEndIndex;
    }

    public String getResultOutput() {
        return resultOut;
    }

    public String getResultError() {
        return resultErr;
    }
}
