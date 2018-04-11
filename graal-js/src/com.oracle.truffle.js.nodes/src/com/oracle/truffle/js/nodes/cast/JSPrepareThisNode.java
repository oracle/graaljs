/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.access.GlobalObjectNode;
import com.oracle.truffle.js.nodes.access.IsObjectNode;
import com.oracle.truffle.js.nodes.unary.JSUnaryNode;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.builtins.JSBoolean;
import com.oracle.truffle.js.runtime.builtins.JSNumber;
import com.oracle.truffle.js.runtime.builtins.JSString;
import com.oracle.truffle.js.runtime.builtins.JSSymbol;
import com.oracle.truffle.js.runtime.interop.JavaClass;
import com.oracle.truffle.js.runtime.interop.JavaMethod;
import com.oracle.truffle.js.runtime.objects.JSLazyString;
import com.oracle.truffle.js.runtime.objects.JSObject;

/**
 * Implementation of ECMAScript 5.1, 10.4.3 Entering Function Code, for non-strict callees.
 *
 * Converts the caller provided thisArg to the ThisBinding of the callee's execution context,
 * replacing null or undefined with the global object and performing ToObject on primitives.
 */
@ImportStatic(JSObject.class)
public abstract class JSPrepareThisNode extends JSUnaryNode {
    protected static final int MAX_CLASSES = 3;

    final JSContext context;

    protected JSPrepareThisNode(JSContext context, JavaScriptNode child) {
        super(child);
        this.context = context;
    }

    public static JSPrepareThisNode createPrepareThisBinding(JSContext context, JavaScriptNode child) {
        return JSPrepareThisNodeGen.create(context, child);
    }

    @Specialization
    protected DynamicObject doJSObject(DynamicObject object,
                    @Cached("create()") IsObjectNode isObjectNode,
                    @Cached("createBinaryProfile()") ConditionProfile objectOrGlobalProfile) {
        if (objectOrGlobalProfile.profile(isObjectNode.executeBoolean(object))) {
            return object;
        } else {
            return GlobalObjectNode.getGlobalObject(context);
        }
    }

    @Specialization
    protected DynamicObject doBoolean(boolean value) {
        return JSBoolean.create(context, value);
    }

    @Specialization
    protected DynamicObject doJSLazyString(JSLazyString value) {
        return JSString.create(context, value);
    }

    @Specialization
    protected DynamicObject doString(String value) {
        return JSString.create(context, value);
    }

    @Specialization
    protected DynamicObject doInt(int value) {
        return JSNumber.create(context, value);
    }

    @Specialization
    protected DynamicObject doDouble(double value) {
        return JSNumber.create(context, value);
    }

    @Specialization(guards = "isJavaNumber(value)")
    protected DynamicObject doNumber(Object value) {
        return JSNumber.create(context, (Number) value);
    }

    @Specialization
    protected DynamicObject doSymbol(Symbol value) {
        return JSSymbol.create(context, value);
    }

    @Specialization
    protected JavaClass doJava(JavaClass value) {
        return value;
    }

    @Specialization
    protected JavaMethod doJava(JavaMethod value) {
        return value;
    }

    @Specialization(guards = {"object != null", "cachedClass != null", "object.getClass() == cachedClass"}, limit = "MAX_CLASSES")
    protected Object doJavaObject(Object object, @Cached("getNonJSObjectClass(object)") Class<?> cachedClass) {
        return doJavaGeneric(cachedClass.cast(object));
    }

    @Specialization(guards = {"!isBoolean(object)", "!isNumber(object)", "!isString(object)", "!isSymbol(object)", "!isJSObject(object)"}, replaces = "doJavaObject")
    protected Object doJavaGeneric(Object object) {
        assert JSRuntime.isJavaObject(object) || JSRuntime.isForeignObject(object);
        return object;
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        return JSPrepareThisNodeGen.create(context, getOperand());
    }
}
