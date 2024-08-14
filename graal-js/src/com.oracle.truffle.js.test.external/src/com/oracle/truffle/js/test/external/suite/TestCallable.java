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
package com.oracle.truffle.js.test.external.suite;

import java.io.File;
import java.io.OutputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotAccess;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.io.IOAccess;

import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.runtime.JSContextOptions;
import com.oracle.truffle.js.snapshot.SnapshotUtil;

public class TestCallable extends AbstractTestCallable {

    private final Source[] prequelSources;
    private final Source testSource;
    private final File scriptFile;
    protected final Context.Builder contextBuilder;

    public TestCallable(TestSuite suite, Source[] prequelSources, Source testSource, File scriptFile, int ecmaScriptVersion) {
        this(suite, prequelSources, testSource, scriptFile, ecmaScriptVersion, Map.of());
    }

    public TestCallable(TestSuite suite, Source[] prequelSources, Source testSource, File scriptFile, int ecmaScriptVersion, Map<String, String> extraOptions) {
        this(suite, prequelSources, testSource, scriptFile, ecmaScriptVersion, extraOptions, IOAccess.newBuilder().allowHostFileAccess(true).build());
    }

    public TestCallable(TestSuite suite, Source[] prequelSources, Source testSource, File scriptFile, int ecmaScriptVersion, Map<String, String> extraOptions, IOAccess ioAccess) {
        super(suite);
        this.prequelSources = prequelSources;
        this.testSource = testSource;
        this.scriptFile = scriptFile;

        if (suite.getConfig().isPolyglot()) {
            this.contextBuilder = Context.newBuilder();
            contextBuilder.allowPolyglotAccess(PolyglotAccess.ALL);
        } else {
            this.contextBuilder = Context.newBuilder(JavaScriptLanguage.ID);
        }
        contextBuilder.allowIO(ioAccess);
        contextBuilder.allowExperimentalOptions(true);
        contextBuilder.allowCreateThread(true);
        contextBuilder.option(JSContextOptions.ECMASCRIPT_VERSION_NAME, ecmaScriptVersionToOptionString(ecmaScriptVersion));
        contextBuilder.option(JSContextOptions.STRICT_NAME, Boolean.toString(false));
        contextBuilder.options(suite.getCommonOptions());
        contextBuilder.options(extraOptions);
        contextBuilder.option(JSContextOptions.LOCALE_NAME, suite.getConfig().getLocale());
        contextBuilder.timeZone(suite.getConfig().getTimeZone());
        if (suite.getConfig().isShareEngine()) {
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
            boolean snapshot = getConfig().useSnapshots();
            if (snapshot) {
                SnapshotUtil.installEvalUsingSnapshotBuiltin(context, snapshotCache);
            }

            for (Source source : getPrequelSources()) {
                eval(context, source, true, snapshot);
            }
            return eval(context, getTestSource(), false, snapshot);
        }
    }

    private static Object eval(Context context, Source source, boolean prequelSource, boolean snapshot) {
        return snapshot ? evalSnapshot(context, source, prequelSource) : evalDefault(context, source);
    }

    private static Object evalDefault(Context context, Source source) {
        return context.eval(source);
    }

    private static Object evalSnapshot(Context context, Source source, boolean cacheSnapshot) {
        // Use snapshots for scripts only
        if (JavaScriptLanguage.MODULE_MIME_TYPE.equals(source.getMimeType())) {
            return evalDefault(context, source);
        }

        try {
            Value value = context.getBindings(JavaScriptLanguage.ID).getMember(SnapshotUtil.EVAL_USING_SNAPSHOT_NAME);
            String path = (source.getPath() == null) ? "" : source.getPath();
            return value.execute(source.getCharacters(), path, source.getName(), cacheSnapshot);
        } catch (RuntimeException ex) {
            String message = ex.getMessage();
            if (message != null && (message.contains("EvalNodeGen@") || message.contains("EvalVariableNode@"))) {
                // Known limitation of snapshotting: EvalNode is not created through NodeFactory
                // => fallback to the regular evaluation
                return evalDefault(context, source);
            }
            throw ex;
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

    private static final Map<com.oracle.truffle.api.source.Source, byte[]> snapshotCache = new ConcurrentHashMap<>();

}
