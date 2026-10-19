/*
 * Copyright (c) 2018, 2026, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.nodes.cast;

import java.util.Set;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.HostCompilerDirectives.InliningCutoff;
import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NeverDefault;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.runtime.BigInt;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSFrameUtil;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.builtins.JSBigInt;
import com.oracle.truffle.js.runtime.builtins.JSBoolean;
import com.oracle.truffle.js.runtime.builtins.JSNumber;
import com.oracle.truffle.js.runtime.builtins.JSString;
import com.oracle.truffle.js.runtime.builtins.JSSymbol;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSObject;

/**
 * Implementation of the implicit {@code this} argument conversion upon entering non-strict
 * functions (OrdinaryCallBindThis).
 *
 * Converts the caller provided thisArg to the ThisBinding of the callee's execution context,
 * replacing null or undefined with the global object and performing ToObject on primitives.
 *
 * @see JSToObjectNode
 */
@ImportStatic({CompilerDirectives.class, JSConfig.class})
public abstract class JSPrepareThisNode extends JavaScriptNode {

    protected JSPrepareThisNode() {
    }

    @NeverDefault
    public static JSPrepareThisNode create() {
        return JSPrepareThisNodeGen.create();
    }

    @Override
    public final Object execute(VirtualFrame frame) {
        return execute(JSFrameUtil.getThisObj(frame));
    }

    public abstract Object execute(Object value);

    @Specialization(guards = "isNullOrUndefined(object)")
    public static JSDynamicObject doNullish(@SuppressWarnings("unused") Object object,
                    @Bind JSRealm realm) {
        return realm.getGlobalObject();
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"cachedClass != null", "isExact(object, cachedClass)"}, limit = "1")
    public static Object doJSObjectCached(Object object,
                    @Cached(value = "getClassIfJSObject(object)") Class<?> cachedClass) {
        return object;
    }

    @Specialization(replaces = "doJSObjectCached")
    public static JSObject doJSObject(JSObject object) {
        return object;
    }

    @Specialization
    public static JSObject doBoolean(boolean value,
                    @Bind JSContext context,
                    @Bind JSRealm realm) {
        return JSBoolean.create(context, realm, value);
    }

    @Specialization
    public static JSObject doString(TruffleString value,
                    @Bind JSContext context,
                    @Bind JSRealm realm) {
        return JSString.create(context, realm, value);
    }

    @Specialization
    public static JSObject doInt(int value,
                    @Bind JSContext context,
                    @Bind JSRealm realm) {
        return JSNumber.create(context, realm, value);
    }

    @Specialization
    public static JSObject doDouble(double value,
                    @Bind JSContext context,
                    @Bind JSRealm realm) {
        return JSNumber.create(context, realm, value);
    }

    @Specialization
    public static JSObject doBigInt(BigInt value,
                    @Bind JSContext context,
                    @Bind JSRealm realm) {
        return JSBigInt.create(context, realm, value);
    }

    @Specialization
    public static JSObject doSymbol(Symbol value,
                    @Bind JSContext context,
                    @Bind JSRealm realm) {
        return JSSymbol.create(context, realm, value);
    }

    @InliningCutoff
    @Specialization(guards = {"isForeignObjectOrNumber(value)"}, limit = "InteropLibraryLimit")
    public static Object doForeignObject(Object value,
                    @Bind Node node,
                    @Bind JSRealm realm,
                    @CachedLibrary("value") InteropLibrary interop) {
        if (interop.isNull(value)) {
            return realm.getGlobalObject();
        }
        return JSToObjectNode.doForeignObjectNonNull(value, interop, node);
    }

    @Override
    public final int executeInt(VirtualFrame frame) throws UnexpectedResultException {
        return super.executeInt(frame);
    }

    @Override
    public final double executeDouble(VirtualFrame frame) throws UnexpectedResultException {
        return super.executeDouble(frame);
    }

    @Override
    public final boolean executeBoolean(VirtualFrame frame) throws UnexpectedResultException {
        return super.executeBoolean(frame);
    }

    @Override
    public final void executeVoid(VirtualFrame frame) {
        super.executeVoid(frame);
    }

    @Override
    protected JavaScriptNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
        return JSPrepareThisNodeGen.create();
    }
}
