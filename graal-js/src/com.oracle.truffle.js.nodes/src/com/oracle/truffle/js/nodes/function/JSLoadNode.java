/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.function;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.NodeEvaluator;
import com.oracle.truffle.js.nodes.ScriptNode;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSTruffleOptions;

@ImportStatic(JSTruffleOptions.class)
public abstract class JSLoadNode extends JavaScriptBaseNode {

    protected final JSContext context;

    public static JSLoadNode create(JSContext context) {
        return JSLoadNodeGen.create(context);
    }

    protected JSLoadNode(JSContext context) {
        this.context = context;
    }

    public abstract Object executeLoad(Source source, JSRealm realm);

    @TruffleBoundary
    protected final ScriptNode loadScript(Source source) {
        long startTime = JSTruffleOptions.ProfileTime ? System.nanoTime() : 0L;
        try {
            return ((NodeEvaluator) context.getEvaluator()).loadCompile(context, source);
        } finally {
            if (JSTruffleOptions.ProfileTime) {
                context.getTimeProfiler().printElapsed(startTime, "parsing " + source.getName());
            }
        }
    }

    @TruffleBoundary
    static boolean equals(Source source, Source cachedSource) {
        return source.equals(cachedSource);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = "equals(source, cachedSource)", limit = "MaxLoadCacheLength")
    static Object cachedLoad(Source source, JSRealm realm,
                    @Cached("source") Source cachedSource,
                    @Cached("loadScript(source)") ScriptNode script) {
        return script.run(realm);
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    @Specialization(guards = "TrimLoadCache", replaces = "cachedLoad")
    final Object uncachedLoad(Source source, JSRealm realm) {
        return loadScript(source).run(realm);
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    @Specialization(guards = "!TrimLoadCache")
    final Object uncachedLoadNoTrim(Source source, JSRealm realm) {
        return loadScript(source).run(realm);
    }
}
