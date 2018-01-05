/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.access;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.js.nodes.JSGuards;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.runtime.JSRuntime;

/**
 * Checks whether the argument is a DynamicObject implemented by Graal-js.
 */
public abstract class IsJSObjectNode extends JavaScriptBaseNode {

    protected static final int MAX_SHAPE_COUNT = 1;

    public abstract boolean executeBoolean(Object obj);

    @SuppressWarnings("unused")
    @Specialization(guards = "cachedShape.check(object)", limit = "MAX_SHAPE_COUNT")
    protected static boolean doIsObjectShape(DynamicObject object,
                    @Cached("object.getShape()") Shape cachedShape,
                    @Cached("isJSType(object)") boolean cachedResult) {
        return cachedResult;
    }

    @Specialization(replaces = "doIsObjectShape")
    protected static boolean doIsObject(Object object,
                    @Cached("createBinaryProfile()") ConditionProfile resultProfile) {
        return resultProfile.profile(JSGuards.isJSType(object) && JSRuntime.isObject(object));
    }

    public static IsJSObjectNode create() {
        return IsJSObjectNodeGen.create();
    }
}
