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
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.runtime.BigInt;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Symbol;

/**
 * Represents abstract operation IsCallable.
 *
 * @see JSRuntime#isCallable(Object)
 */
@GenerateUncached
public abstract class IsCallableNode extends JavaScriptBaseNode {

    protected IsCallableNode() {
    }

    public abstract boolean executeBoolean(Object operand);

    @SuppressWarnings("unused")
    @Specialization(guards = {"shape.check(function)", "isJSFunctionShape(shape)"}, limit = "1")
    protected static boolean doJSFunctionShape(DynamicObject function,
                    @Cached("function.getShape()") Shape shape) {
        return true;
    }

    @SuppressWarnings("unused")
    @Specialization(guards = "isJSFunction(function)", replaces = "doJSFunctionShape")
    protected static boolean doJSFunction(DynamicObject function) {
        return true;
    }

    @Specialization(guards = "isJSProxy(proxy)")
    protected static boolean doJSProxy(DynamicObject proxy) {
        return JSRuntime.isCallableProxy(proxy);
    }

    @Specialization(guards = {"isJSDynamicObject(object)", "!isJSFunction(object)", "!isJSProxy(object)"})
    protected static boolean doJSTypeOther(@SuppressWarnings("unused") DynamicObject object) {
        return false;
    }

    @Specialization(guards = "isForeignObject(obj)", limit = "3")
    protected static boolean doTruffleObject(Object obj,
                    @CachedLibrary("obj") InteropLibrary interop) {
        return interop.isExecutable(obj);
    }

    @Specialization
    protected static boolean doCharSequence(@SuppressWarnings("unused") CharSequence charSequence) {
        return false;
    }

    @Specialization
    protected static boolean doNumber(@SuppressWarnings("unused") Number number) {
        return false;
    }

    @Specialization
    protected static boolean doBoolean(@SuppressWarnings("unused") boolean value) {
        return false;
    }

    @Specialization
    protected static boolean doSymbol(@SuppressWarnings("unused") Symbol symbol) {
        return false;
    }

    @Specialization
    protected static boolean doBigInt(@SuppressWarnings("unused") BigInt bigInt) {
        return false;
    }

    public static IsCallableNode create() {
        return IsCallableNodeGen.create();
    }
}
