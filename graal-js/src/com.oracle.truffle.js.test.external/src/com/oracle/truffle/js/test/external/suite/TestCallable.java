/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.test.external.suite;

import java.io.File;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Map;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotAccess;
import org.graalvm.polyglot.Source;

import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.runtime.JSContextOptions;

public class TestCallable extends AbstractTestCallable {

    private final Source[] prequelSources;
    private final Source testSource;
    private final File scriptFile;
    private final Context.Builder contextBuilder;

    public TestCallable(TestSuite suite, Source[] prequelSources, Source testSource, File scriptFile, int ecmaScriptVersion) {
        this(suite, prequelSources, testSource, scriptFile, ecmaScriptVersion, Collections.emptyMap());
    }

    public TestCallable(TestSuite suite, Source[] prequelSources, Source testSource, File scriptFile, int ecmaScriptVersion, Map<String, String> extraOptions) {
        super(suite);
        this.prequelSources = prequelSources;
        this.testSource = testSource;
        this.scriptFile = scriptFile;

        if (getConfig().isPolyglot()) {
            this.contextBuilder = Context.newBuilder();
            contextBuilder.allowPolyglotAccess(PolyglotAccess.ALL);
        } else {
            this.contextBuilder = Context.newBuilder(JavaScriptLanguage.ID);
        }
        contextBuilder.allowIO(true);
        contextBuilder.allowExperimentalOptions(true);
        contextBuilder.option(JSContextOptions.ECMASCRIPT_VERSION_NAME, ecmaScriptVersionToOptionString(ecmaScriptVersion));
        contextBuilder.option(JSContextOptions.STRICT_NAME, Boolean.toString(false));
        contextBuilder.options(suite.getCommonOptions());
        contextBuilder.options(extraOptions);
        if (getConfig().isShareEngine()) {
            contextBuilder.engine(suite.getSharedEngine());
        } else {
            contextBuilder.option("engine.WarnInterpreterOnly", Boolean.toString(false));
        }
    }

    protected Source[] getPrequelSources() {
        return prequelSources;
    }

    protected Source getTestSource() {
        return testSource;
    }

    protected File getScriptFile() {
        return scriptFile;
    }

    protected String getScriptFileContent() {
        return testSource.getCharacters().toString();
    }

    @Override
    public Object call() throws Exception {
        try (Context context = contextBuilder.build()) {
            for (Source source : getPrequelSources()) {
                context.eval(JavaScriptLanguage.ID, source.getCharacters());
            }
            return context.eval(getTestSource());
        }
    }

    @Override
    public void setOutput(OutputStream out) {
        contextBuilder.out(out);
    }

    @Override
    public void setError(OutputStream err) {
        contextBuilder.err(err);
    }
}
