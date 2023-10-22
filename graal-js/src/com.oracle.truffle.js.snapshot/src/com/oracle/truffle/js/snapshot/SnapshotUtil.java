/*
 * Copyright (c) 2023, 2023, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.snapshot;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.Map;

import org.graalvm.polyglot.Context;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.nodes.ScriptNode;
import com.oracle.truffle.js.parser.JSParser;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSFunctionData;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;

public class SnapshotUtil {
    public static final String EVAL_USING_SNAPSHOT_NAME = "evalUsingSnapshot";
    private static final TruffleString EVAL_USING_SNAPSHOT_NAME_TS = Strings.constant(EVAL_USING_SNAPSHOT_NAME);

    // Installs a built-in for the evaluation using snapshot. A built-in
    // is needed for the correct processing of promises, exceptions etc.
    // This built-in is not defined in com.oracle.truffle.js.builtins
    // to avoid the dependency on com.oracle.truffle.js.snapshot there.
    public static void installEvalUsingSnapshotBuiltin(Context polyglotContext, Map<com.oracle.truffle.api.source.Source, byte[]> snapshotCache) {
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
            Object fn = JSFunction.create(realm, JSFunctionData.create(context, rootNode.getCallTarget(), 4, EVAL_USING_SNAPSHOT_NAME_TS));
            JSObjectUtil.putDataProperty(realm.getGlobalObject(), EVAL_USING_SNAPSHOT_NAME_TS, fn);
        } finally {
            polyglotContext.leave();
        }
    }

}
