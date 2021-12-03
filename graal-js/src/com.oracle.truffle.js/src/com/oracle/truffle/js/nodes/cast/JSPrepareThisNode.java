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
package com.oracle.truffle.js.nodes.cast;

import java.util.Set;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.unary.JSUnaryNode;
import com.oracle.truffle.js.runtime.BigInt;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.builtins.JSBigInt;
import com.oracle.truffle.js.runtime.builtins.JSBoolean;
import com.oracle.truffle.js.runtime.builtins.JSNumber;
import com.oracle.truffle.js.runtime.builtins.JSString;
import com.oracle.truffle.js.runtime.builtins.JSSymbol;
import com.oracle.truffle.js.runtime.objects.JSLazyString;

/**
 * Implementation of ECMAScript 5.1, 10.4.3 Entering Function Code, for non-strict callees.
 *
 * Converts the caller provided thisArg to the ThisBinding of the callee's execution context,
 * replacing null or undefined with the global object and performing ToObject on primitives.
 */
@ImportStatic({CompilerDirectives.class, JSConfig.class})
public abstract class JSPrepareThisNode extends JSUnaryNode {

    final JSContext context;

    protected JSPrepareThisNode(JSContext context, JavaScriptNode child) {
        super(child);
        this.context = context;
    }

    public static JSPrepareThisNode createPrepareThisBinding(JSContext context, JavaScriptNode child) {
        return JSPrepareThisNodeGen.create(context, child);
    }

    @Specialization(guards = "isNullOrUndefined(object)")
    protected DynamicObject doJSObject(@SuppressWarnings("unused") Object object) {
        return getRealm().getGlobalObject();
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"cachedClass != null", "isExact(object, cachedClass)"}, limit = "1")
    protected Object doJSObjectCached(Object object,
                    @Cached("getClassIfJSObject(object)") Class<?> cachedClass) {
        return object;
    }

    @Specialization(guards = "isJSObject(object)", replaces = "doJSObjectCached")
    protected DynamicObject doJSObject(DynamicObject object) {
        return object;
    }

    @Specialization
    protected DynamicObject doBoolean(boolean value) {
        return JSBoolean.create(context, getRealm(), value);
    }

    @Specialization
    protected DynamicObject doJSLazyString(JSLazyString value) {
        return JSString.create(context, getRealm(), value);
    }

    @Specialization
    protected DynamicObject doString(String value) {
        return JSString.create(context, getRealm(), value);
    }

    @Specialization
    protected DynamicObject doInt(int value) {
        return JSNumber.create(context, getRealm(), value);
    }

    @Specialization
    protected DynamicObject doDouble(double value) {
        return JSNumber.create(context, getRealm(), value);
    }

    @Specialization
    protected DynamicObject doBigInt(BigInt value) {
        return JSBigInt.create(context, getRealm(), value);
    }

    @Specialization(guards = "isJavaNumber(value)")
    protected DynamicObject doNumber(Object value) {
        return JSNumber.create(context, getRealm(), (Number) value);
    }

    @Specialization
    protected DynamicObject doSymbol(Symbol value) {
        return JSSymbol.create(context, getRealm(), value);
    }

    @Specialization(guards = "isForeignObject(object)", limit = "InteropLibraryLimit")
    protected Object doForeignObject(Object object,
                    @CachedLibrary("object") InteropLibrary interop) {
        if (interop.isNull(object)) {
            return getRealm().getGlobalObject();
        }
        return object;
    }

    @Override
    protected JavaScriptNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
        return JSPrepareThisNodeGen.create(context, cloneUninitialized(getOperand(), materializedTags));
    }
}
