/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSTruffleOptions;
import com.oracle.truffle.js.runtime.RegexCompilerInterface;

@ImportStatic(JSTruffleOptions.class)
public abstract class CompileRegexNode extends JavaScriptBaseNode {

    @Child private Node executeCompilerNode = RegexCompilerInterface.createExecuteCompilerNode();
    private final JSContext context;

    protected CompileRegexNode(JSContext context) {
        this.context = context;
    }

    public static CompileRegexNode create(JSContext context) {
        return CompileRegexNodeGen.create(context);
    }

    public final TruffleObject compile(String pattern) {
        return compile(pattern, "");
    }

    public final TruffleObject compile(CharSequence pattern, String flags) {
        return (TruffleObject) executeCompile(pattern, flags);
    }

    protected abstract Object executeCompile(CharSequence pattern, String flags);

    @SuppressWarnings("unused")
    @Specialization(guards = {"stringEquals(pattern, cachedPattern)", "stringEquals(flags, cachedFlags)"}, limit = "MaxCompiledRegexCacheLength")
    protected Object getCached(String pattern, String flags, //
                    @Cached("pattern") String cachedPattern, //
                    @Cached("flags") String cachedFlags, //
                    @Cached("doCompile(pattern, flags)") Object cachedCompiledRegex) {
        return cachedCompiledRegex;
    }

    @TruffleBoundary
    protected static boolean stringEquals(String a, String b) {
        return a.equals(b);
    }

    @Specialization(guards = {"!TrimCompiledRegexCache"})
    protected Object doCompileNoTrimCache(String pattern, String flags) {
        // optional specialization that does not trim the cache
        return doCompile(pattern, flags);
    }

    @Specialization(replaces = {"getCached"})
    protected Object doCompile(String pattern, String flags) {
        return RegexCompilerInterface.compile(pattern, flags, context, executeCompilerNode);
    }
}
