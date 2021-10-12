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
package com.oracle.truffle.js.nodes.access;

import java.util.Set;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.js.nodes.JSGuards;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.access.RequireObjectCoercibleNodeGen.RequireObjectCoercibleWrapperNodeGen;
import com.oracle.truffle.js.nodes.unary.JSUnaryNode;
import com.oracle.truffle.js.runtime.BigInt;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.SafeInteger;
import com.oracle.truffle.js.runtime.Symbol;

/**
 * Implementation of the abstract operation RequireObjectCoercible(argument) (ES6 7.2.1).
 */
@ImportStatic({CompilerDirectives.class, JSConfig.class})
public abstract class RequireObjectCoercibleNode extends JavaScriptBaseNode {

    protected RequireObjectCoercibleNode() {
    }

    public static RequireObjectCoercibleNode create() {
        return RequireObjectCoercibleNodeGen.create();
    }

    public final Object execute(Object operand) {
        executeVoid(operand);
        return operand;
    }

    public abstract void executeVoid(Object operand);

    @Specialization
    protected static void doInt(@SuppressWarnings("unused") int value) {
    }

    @Specialization
    protected static void doSafeInteger(@SuppressWarnings("unused") SafeInteger value) {
    }

    @Specialization
    protected static void doLong(@SuppressWarnings("unused") long value) {
    }

    @Specialization
    protected static void doDouble(@SuppressWarnings("unused") double value) {
    }

    @Specialization
    protected static void doCharSequence(@SuppressWarnings("unused") CharSequence value) {
    }

    @Specialization
    protected static void doBoolean(@SuppressWarnings("unused") boolean value) {
    }

    @Specialization
    protected static void doSymbol(@SuppressWarnings("unused") Symbol value) {
    }

    @Specialization
    protected static void doBigInt(@SuppressWarnings("unused") BigInt value) {
    }

    @Specialization(guards = {"cachedClass != null", "isExact(object, cachedClass)"}, limit = "1")
    protected static void doCachedJSClass(@SuppressWarnings("unused") Object object,
                    @Cached("getClassIfJSObject(object)") @SuppressWarnings("unused") Class<?> cachedClass) {
    }

    @Specialization(guards = {"isJSObject(object)"}, replaces = "doCachedJSClass")
    protected static void doJSObject(@SuppressWarnings("unused") Object object) {
    }

    @Specialization(guards = {"isForeignObject(object)"}, limit = "InteropLibraryLimit")
    protected void doForeignObject(Object object, @CachedLibrary("object") InteropLibrary interop) {
        if (interop.isNull(object)) {
            throw Errors.createTypeErrorNotObjectCoercible(object, this);
        }
    }

    @Specialization(guards = "isNullOrUndefined(object)")
    protected void doNullOrUndefined(DynamicObject object) {
        throw Errors.createTypeErrorNotObjectCoercible(object, this);
    }

    protected static Shape getShapeIfObject(DynamicObject object) {
        if (JSGuards.isJSObject(object)) {
            return object.getShape();
        }
        return null;
    }

    public abstract static class RequireObjectCoercibleWrapperNode extends JSUnaryNode {

        @Child private RequireObjectCoercibleNode requireObjectCoercibleNode = RequireObjectCoercibleNode.create();

        protected RequireObjectCoercibleWrapperNode(JavaScriptNode operand) {
            super(operand);
        }

        public static RequireObjectCoercibleWrapperNode create(JavaScriptNode child) {
            return RequireObjectCoercibleWrapperNodeGen.create(child);
        }

        @Override
        public boolean isResultAlwaysOfType(Class<?> clazz) {
            return getOperand().isResultAlwaysOfType(clazz);
        }

        @Specialization
        protected Object doDefault(Object value) {
            return requireObjectCoercibleNode.execute(value);
        }

        @Override
        protected JavaScriptNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
            return RequireObjectCoercibleWrapperNode.create(cloneUninitialized(getOperand(), materializedTags));
        }
    }
}
