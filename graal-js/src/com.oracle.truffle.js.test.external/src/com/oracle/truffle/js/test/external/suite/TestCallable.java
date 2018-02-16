/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.test.external.suite;

import java.io.File;
import java.io.OutputStream;
import java.util.Map;
import java.util.concurrent.Callable;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;

import com.oracle.truffle.js.parser.GraalJSParserOptions;
import com.oracle.truffle.js.parser.JavaScriptLanguage;
import com.oracle.truffle.js.runtime.JSContextOptions;
import com.oracle.truffle.js.runtime.JSTruffleOptions;

public class TestCallable implements Callable<Object> {

    protected final Source[] prequelSources;
    private final org.graalvm.polyglot.Source testSource;
    private final File scriptFile;
    private final TestSuite suite;

    private final Context.Builder contextBuilder;

    public TestCallable(TestSuite suite, Source[] prequelSources, Source testSource, File scriptFile, int ecmaScriptVersion, Map<String, String> options) {
        this.prequelSources = prequelSources;
        this.testSource = testSource;
        this.scriptFile = scriptFile;
        this.suite = suite;

        this.contextBuilder = Context.newBuilder(JavaScriptLanguage.ID);

        assert ecmaScriptVersion <= JSTruffleOptions.MaxECMAScriptVersion;

        contextBuilder.option(JSContextOptions.ECMASCRIPT_VERSION_NAME, Integer.toString(ecmaScriptVersion));
        contextBuilder.option(GraalJSParserOptions.STRICT_NAME, Boolean.toString(false));
        contextBuilder.option(GraalJSParserOptions.SYNTAX_EXTENSIONS_NAME, Boolean.toString(false));
        contextBuilder.option(GraalJSParserOptions.SHEBANG_NAME, Boolean.toString(false));
        contextBuilder.option(GraalJSParserOptions.CONST_AS_VAR_NAME, Boolean.toString(false));
        contextBuilder.options(options);
    }

    protected final SuiteConfig getConfig() {
        return suite.getConfig();
    }

    protected String getScriptFileContent() {
        return testSource.getCharacters().toString();
    }

    protected File getScriptFile() {
        return scriptFile;
    }

    @Override
    public Object call() throws Exception {
        try (Context context = contextBuilder.build()) {
            for (Source source : prequelSources) {
                context.eval(JavaScriptLanguage.ID, source.getCharacters());
            }
            return context.eval(testSource);
        } catch (Exception e) {
            throw e;
        } finally {
            // resultOut = out.toString();
            // resultErr = err.toString();
        }
    }

    public void setOutput(OutputStream out) {
        contextBuilder.out(out);
    }

    public void setError(OutputStream err) {
        contextBuilder.err(err);
    }
}
