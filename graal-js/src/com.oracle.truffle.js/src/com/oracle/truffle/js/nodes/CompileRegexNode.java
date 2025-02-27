/*
 * Copyright (c) 2018, 2025, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.nodes;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NeverDefault;
import com.oracle.truffle.api.dsl.ReportPolymorphism;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.api.utilities.AssumedValue;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.RegexCompilerInterface;
import com.oracle.truffle.js.runtime.Strings;

@ImportStatic({JSConfig.class, Strings.class})
public abstract class CompileRegexNode extends JavaScriptBaseNode {

    private final JSContext context;
    @Child private InteropLibrary isCompiledRegexNullNode;
    @Child TruffleString.EqualNode equalsNode = TruffleString.EqualNode.create();

    protected CompileRegexNode(JSContext context) {
        this.context = context;
    }

    @NeverDefault
    public static CompileRegexNode create(JSContext context) {
        return CompileRegexNodeGen.create(context);
    }

    public final Object compile(Object pattern) {
        return compile(pattern, Strings.EMPTY_STRING);
    }

    public final Object compile(Object pattern, Object flags) {
        return executeCompile(pattern, flags);
    }

    protected abstract Object executeCompile(Object pattern, Object flags);

    @SuppressWarnings("unused")
    @Specialization(guards = {"equals(equalsNode, pattern, cachedPattern)", "equals(equalsNode, flags, cachedFlags)"}, limit = "MaxCompiledRegexCacheLength")
    protected Object getCached(TruffleString pattern, TruffleString flags,
                    @Cached("pattern") TruffleString cachedPattern,
                    @Cached("flags") TruffleString cachedFlags,
                    @Cached("createAssumedValue()") AssumedValue<Object> cachedCompiledRegex,
                    @Cached @Shared TruffleString.ToJavaStringNode toJavaString) {
        // Note: we must not compile the regex while holding the AST lock (initializing @Cached).
        Object cached = cachedCompiledRegex.get();
        if (cached == null) {
            cached = doCompile(cachedPattern, cachedFlags, toJavaString);
            cachedCompiledRegex.set(cached);
        }
        return cached;
    }

    @Specialization(guards = {"!TrimCompiledRegexCache"})
    protected Object doCompileNoTrimCache(TruffleString pattern, TruffleString flags,
                    @Cached @Shared TruffleString.ToJavaStringNode toJavaString) {
        // optional specialization that does not trim the cache
        return doCompile(pattern, flags, toJavaString);
    }

    @ReportPolymorphism.Megamorphic
    @Specialization(replaces = {"getCached"})
    protected Object doCompile(TruffleString pattern, TruffleString flags,
                    @Cached @Shared TruffleString.ToJavaStringNode toJavaString) {
        return RegexCompilerInterface.compile(Strings.toJavaString(toJavaString, pattern), Strings.toJavaString(toJavaString, flags), context, getRealm(), getIsCompiledRegexNullNode());
    }

    private InteropLibrary getIsCompiledRegexNullNode() {
        if (isCompiledRegexNullNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            isCompiledRegexNullNode = insert(InteropLibrary.getFactory().createDispatched(3));
        }
        return isCompiledRegexNullNode;
    }

    AssumedValue<Object> createAssumedValue() {
        return new AssumedValue<>("compiledRegex", null);
    }
}
