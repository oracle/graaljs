/*
 * Copyright (c) 2012, 2014, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
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

    protected JSPrepareThisNode(JSContext context) {
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
