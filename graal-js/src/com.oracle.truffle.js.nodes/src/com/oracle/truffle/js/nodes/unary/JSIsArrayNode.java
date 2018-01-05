/*
 * Copyright (c) 2016, 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.unary;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.access.IsArrayNode;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.builtins.JSClass;
import com.oracle.truffle.js.runtime.objects.JSObject;

/**
 * ES6 7.2.2 IsArray(argument).
 *
 * @see IsArrayNode
 */
@ImportStatic(JSObject.class)
public abstract class JSIsArrayNode extends JavaScriptBaseNode {
    protected static final int MAX_SHAPE_COUNT = 1;
    protected static final int MAX_JSCLASS_COUNT = 1;

    @CompilationFinal private ConditionProfile isArrayProfile;
    @CompilationFinal private ConditionProfile isProxyProfile;

    protected JSIsArrayNode() {
    }

    public abstract boolean execute(Object operand);

    @SuppressWarnings("unused")
    @Specialization(guards = {"!cachedIsProxy", "cachedShape.check(object)"}, limit = "MAX_SHAPE_COUNT")
    protected static boolean doIsArrayShape(DynamicObject object,
                    @Cached("object.getShape()") Shape cachedShape,
                    @Cached("isJSArray(object)") boolean cachedIsArray,
                    @Cached("isJSProxy(object)") boolean cachedIsProxy) {
        // (aw) must do the shape check again to preserve the unsafe condition,
        // otherwise we could just do: return cachedResult;
        return cachedIsArray && cachedShape.check(object);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"!cachedIsProxy", "cachedClass.isInstance(object)"}, replaces = "doIsArrayShape", limit = "MAX_JSCLASS_COUNT")
    protected static boolean doIsArrayJSClass(DynamicObject object,
                    @Cached("getJSClass(object)") JSClass cachedClass,
                    @Cached("isJSArray(object)") boolean cachedIsArray,
                    @Cached("isJSProxy(object)") boolean cachedIsProxy) {
        return cachedIsArray;
    }

    @Specialization(guards = {"isJSProxy(object)"})
    protected boolean doIsProxy(DynamicObject object) {
        initProfiles();
        return JSRuntime.isArray(object, isArrayProfile, isProxyProfile);
    }

    @Specialization(replaces = {"doIsArrayJSClass", "doIsProxy"})
    protected boolean doGeneric(DynamicObject object) {
        initProfiles();
        return JSRuntime.isArray(object, isArrayProfile, isProxyProfile);
    }

    @Specialization(guards = {"!isDynamicObject(object)"})
    protected boolean doNotObject(Object object) {
        initProfiles();
        return JSRuntime.isArray(object, isArrayProfile, isProxyProfile);
    }

    private void initProfiles() {
        if (isArrayProfile == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            isArrayProfile = ConditionProfile.createBinaryProfile();
        }
        if (isProxyProfile == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            isProxyProfile = ConditionProfile.createBinaryProfile();
        }
    }

    public static JSIsArrayNode createIsArray() {
        return JSIsArrayNodeGen.create();
    }
}
