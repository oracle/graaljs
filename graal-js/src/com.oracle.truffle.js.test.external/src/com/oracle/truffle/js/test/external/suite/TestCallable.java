/*
 * Copyright (c) 2018, 2022, Oracle and/or its affiliates. All rights reserved.
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

import java.nio.ByteBuffer;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotAccess;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.nodes.ScriptNode;
import com.oracle.truffle.js.parser.JSParser;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSContextOptions;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSFunctionData;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.snapshot.Recording;

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
            boolean snapshot = getConfig().useSnapshots();
            if (snapshot) {
                installEvalUsingSnapshotBuiltin(context);
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
            Value value = context.getBindings(JavaScriptLanguage.ID).getMember(EVAL_USING_SNAPSHOT_NAME);
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

    private static final String EVAL_USING_SNAPSHOT_NAME = "evalUsingSnapshot";
    private static final Map<com.oracle.truffle.api.source.Source, byte[]> snapshotCache = new ConcurrentHashMap<>();

    // Installs a built-in for the evaluation using snapshot. A built-in
    // is needed for the correct processing of promises, exceptions etc.
    // This built-in is not defined in com.oracle.truffle.js.builtins
    // to avoid the dependency on com.oracle.truffle.js.snapshot there.
    private static void installEvalUsingSnapshotBuiltin(Context polyglotContext) {
        polyglotContext.initialize(JavaScriptLanguage.ID);
        polyglotContext.enter();
        try {
            JSRealm realm = JavaScriptLanguage.getJSRealm(polyglotContext);
            JSContext context = realm.getContext();
            RootNode rootNode = new RootNode(JavaScriptLanguage.getCurrentLanguage()) {
                @Override
                public Object execute(VirtualFrame frame) {
                    Object[] args = frame.getArguments();
                    return evalUsingSnapshot(
                                    JSArguments.getUserArgument(args, 0),
                                    JSArguments.getUserArgument(args, 1),
                                    JSArguments.getUserArgument(args, 2),
                                    JSArguments.getUserArgument(args, 3));
                }

                @CompilerDirectives.TruffleBoundary
                private Object evalUsingSnapshot(Object arg0, Object arg1, Object arg2, Object arg3) {
                    String code = arg0.toString();
                    String path = arg1.toString();
                    String name = arg2.toString();
                    boolean cacheSnapshot = (boolean) arg3;
                    com.oracle.truffle.api.source.Source s;
                    if (path.isEmpty()) {
                        s = com.oracle.truffle.api.source.Source.newBuilder(JavaScriptLanguage.ID, code, name).build();
                    } else {
                        s = com.oracle.truffle.api.source.Source.newBuilder(JavaScriptLanguage.ID, realm.getEnv().getPublicTruffleFile(path)).content(code).build();
                    }

                    byte[] bytes = cacheSnapshot ? snapshotCache.get(s) : null;
                    if (bytes == null) {
                        Recording rec = Recording.recordSource(s, context, false, "", "");
                        ByteArrayOutputStream outs = new ByteArrayOutputStream();
                        rec.saveToStream(null, outs, true);
                        bytes = outs.toByteArray();
                        if (cacheSnapshot) {
                            snapshotCache.put(s, bytes);
                        }
                    }

                    JSParser parser = (JSParser) context.getEvaluator();
                    ScriptNode scriptNode = parser.parseScript(context, s, ByteBuffer.wrap(bytes));
                    return scriptNode.run(realm);
                }
            };
            Object fn = JSFunction.create(realm, JSFunctionData.create(context, rootNode.getCallTarget(), 4, EVAL_USING_SNAPSHOT_NAME));
            JSObjectUtil.putDataProperty(context, realm.getGlobalObject(), EVAL_USING_SNAPSHOT_NAME, fn);
        } finally {
            polyglotContext.leave();
        }
    }

}
