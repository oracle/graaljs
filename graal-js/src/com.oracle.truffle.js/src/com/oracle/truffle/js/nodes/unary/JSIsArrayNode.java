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
package com.oracle.truffle.js.nodes.unary;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NeverDefault;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.access.IsArrayNode;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;

/**
 * ES6 7.2.2 IsArray(argument).
 *
 * @see IsArrayNode
 */
@ImportStatic({CompilerDirectives.class})
public abstract class JSIsArrayNode extends JavaScriptBaseNode {

    final boolean jsType;

    protected JSIsArrayNode(boolean jsType) {
        this.jsType = jsType;
    }

    public abstract boolean execute(Object operand);

    @SuppressWarnings("unused")
    @Specialization(guards = {"!cachedIsProxy", "cachedClass != null", "isExact(object, cachedClass)"}, limit = "1")
    protected static boolean doIsArrayClass(Object object,
                    @Cached(value = "getClassIfJSDynamicObject(object)") Class<?> cachedClass,
                    @Cached(value = "isJSArray(object)") boolean cachedIsArray,
                    @Cached(value = "isJSProxy(object)") boolean cachedIsProxy) {
        return cachedIsArray;
    }

    @Specialization(guards = {"isJSArray(object)"}, replaces = {"doIsArrayClass"})
    protected boolean doJSArray(@SuppressWarnings("unused") Object object) {
        return true;
    }

    @Specialization(guards = {"isJSProxy(object)"})
    protected boolean doJSProxy(JSDynamicObject object) {
        return JSRuntime.isProxyAnArray(object);
    }

    @Specialization(guards = {"!isJSArray(object)", "!isJSProxy(object)", "isJSDynamicObject(object)"}, replaces = {"doIsArrayClass"})
    protected boolean doJSObject(Object object) {
        assert !JSRuntime.isArray(object);
        return false;
    }

    @Specialization(guards = {"!isJSDynamicObject(object)", "jsType"})
    protected boolean doNotObject(Object object) {
        assert !JSRuntime.isArray(object) || JSRuntime.isForeignObject(object);
        return false;
    }

    @Specialization(guards = {"!isJSDynamicObject(object)", "!jsType"})
    protected boolean doPrimitiveOrForeign(Object object,
                    @CachedLibrary(limit = "6") InteropLibrary interop) {
        return interop.hasArrayElements(object);
    }

    @NeverDefault
    public static JSIsArrayNode createIsArrayLike() {
        return JSIsArrayNodeGen.create(false);
    }

    @NeverDefault
    public static JSIsArrayNode createIsArray() {
        return JSIsArrayNodeGen.create(true);
    }
}
