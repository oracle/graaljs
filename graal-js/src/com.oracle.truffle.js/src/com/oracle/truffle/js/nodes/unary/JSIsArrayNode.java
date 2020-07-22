/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.access.IsArrayNode;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.builtins.JSClass;

/**
 * ES6 7.2.2 IsArray(argument).
 *
 * @see IsArrayNode
 */
public abstract class JSIsArrayNode extends JavaScriptBaseNode {
    protected static final int MAX_SHAPE_COUNT = 1;
    protected static final int MAX_JSCLASS_COUNT = 1;

    final boolean jsType;

    protected JSIsArrayNode(boolean jsType) {
        this.jsType = jsType;
    }

    public abstract boolean execute(Object operand);

    @SuppressWarnings("unused")
    @Specialization(guards = {"cachedIsJSObject", "!cachedIsProxy", "cachedShape.check(object)"}, limit = "MAX_SHAPE_COUNT")
    protected static boolean doIsArrayShape(DynamicObject object,
                    @Cached("object.getShape()") Shape cachedShape,
                    @Cached("isJSObject(object)") boolean cachedIsJSObject, // ignore non-JS objects
                    @Cached("isJSArray(object)") boolean cachedIsArray,
                    @Cached("isJSProxy(object)") boolean cachedIsProxy) {
        // (aw) must do the shape check again to preserve the unsafe condition,
        // otherwise we could just do: return cachedResult;
        return cachedIsArray && cachedShape.check(object);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"!cachedIsProxy", "cachedClass != null", "cachedClass.isInstance(object)"}, replaces = "doIsArrayShape", limit = "MAX_JSCLASS_COUNT")
    protected static boolean doIsArrayJSClass(DynamicObject object,
                    @Cached("getJSClassChecked(object)") JSClass cachedClass,
                    @Cached("isJSArray(object)") boolean cachedIsArray,
                    @Cached("isJSProxy(object)") boolean cachedIsProxy) {
        return cachedIsArray;
    }

    @Specialization(guards = {"isJSArray(object)"}, replaces = {"doIsArrayJSClass"})
    protected boolean doJSArray(@SuppressWarnings("unused") DynamicObject object) {
        return true;
    }

    @Specialization(guards = {"isJSProxy(object)"})
    protected boolean doJSProxy(DynamicObject object) {
        return JSRuntime.isProxyAnArray(object);
    }

    @Specialization(guards = {"isJSType(object)", "!isJSArray(object)", "!isJSProxy(object)"}, replaces = {"doIsArrayJSClass"})
    protected boolean doJSObject(DynamicObject object) {
        assert !JSRuntime.isArray(object);
        return false;
    }

    @Specialization(guards = {"!isJSType(object)", "jsType"})
    protected boolean doNotObject(Object object) {
        assert !JSRuntime.isArray(object) || JSRuntime.isForeignObject(object);
        return false;
    }

    @Specialization(guards = {"!isJSType(object)", "!jsType"})
    protected boolean doPrimitiveOrForeign(Object object,
                    @CachedLibrary(limit = "6") InteropLibrary interop) {
        return interop.hasArrayElements(object);
    }

    public static JSIsArrayNode createIsArrayLike() {
        return JSIsArrayNodeGen.create(false);
    }

    public static JSIsArrayNode createIsArray() {
        return JSIsArrayNodeGen.create(true);
    }
}
