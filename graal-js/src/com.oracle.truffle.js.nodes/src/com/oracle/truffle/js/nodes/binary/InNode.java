/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.binary;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.access.JSHasPropertyNode;
import com.oracle.truffle.js.nodes.access.JSProxyHasPropertyNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;

public abstract class InNode extends JSBinaryNode {

    protected final JSContext context;
    @Child private JSHasPropertyNode hasPropertyNode;

    protected InNode(JSContext context) {
        this.context = context;
    }

    public static InNode create(JSContext context, JavaScriptNode left, JavaScriptNode right) {
        return InNodeGen.create(context, left, right);
    }

    @Override
    public boolean isResultAlwaysOfType(Class<?> clazz) {
        return clazz == boolean.class;
    }

    @Specialization(guards = {"isJSObject(haystack)", "!isJSProxy(haystack)"})
    protected boolean doObject(Object needle, DynamicObject haystack) {
        return getHasPropertyNode().executeBoolean(haystack, needle);
    }

    @Specialization(guards = {"isJSProxy(haystack)"})
    protected boolean doProxy(Object needle, DynamicObject haystack,
                    @Cached("create(context)") JSProxyHasPropertyNode proxyHasPropertyNode) {
        return proxyHasPropertyNode.executeWithTargetAndKeyBoolean(haystack, needle);
    }

    @Specialization(guards = "isForeignObject(haystack)")
    protected boolean doForeign(Object needle, TruffleObject haystack) {
        return getHasPropertyNode().executeBoolean(haystack, needle);
    }

    @Specialization(guards = "!isJSObject(haystack)")
    protected static Object doNotObject(@SuppressWarnings("unused") Object needle, Object haystack) {
        throw Errors.createTypeErrorNotAnObject(haystack);
    }

    private JSHasPropertyNode getHasPropertyNode() {
        if (hasPropertyNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            hasPropertyNode = insert(JSHasPropertyNode.create());
        }
        return hasPropertyNode;
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        return InNodeGen.create(context, cloneUninitialized(getLeft()), cloneUninitialized(getRight()));
    }
}
