/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.access;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSUserObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.Undefined;

/**
 * GetIterator(obj, hint = async).
 */
public abstract class GetAsyncIteratorNode extends GetIteratorNode {
    @Child private PropertySetNode setState;
    @Child private GetMethodNode getIteratorMethodNode;
    @Child private GetMethodNode getAsyncIteratorMethodNode;

    private final ConditionProfile asyncToSync = ConditionProfile.createBinaryProfile();

    protected GetAsyncIteratorNode(JSContext context) {
        super(context);
        this.setState = PropertySetNode.create(JSFunction.ASYNC_FROM_SYNC_ITERATOR_KEY, false, context, false);
        this.getAsyncIteratorMethodNode = GetMethodNode.create(context, null, Symbol.SYMBOL_ASYNC_ITERATOR);
    }

    @Override
    @Specialization(guards = {"!isForeignObject(iteratedObject)"})
    protected DynamicObject doGetIterator(Object iteratedObject,
                    @Cached("createCall()") JSFunctionCallNode methodCallNode,
                    @Cached("create()") IsObjectNode isObjectNode) {
        Object method = getAsyncIteratorMethodNode.executeWithTarget(iteratedObject);
        if (asyncToSync.profile(method == Undefined.instance)) {
            Object syncMethod = getIteratorMethodNode().executeWithTarget(iteratedObject);
            Object syncIterator = getIterator(iteratedObject, syncMethod, methodCallNode, isObjectNode, this);
            return createAsyncFromSyncIterator(syncIterator);
        }
        return getIterator(iteratedObject, method, methodCallNode, isObjectNode, this);
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        return GetAsyncIteratorNodeGen.create(context, cloneUninitialized(getIteratedObject()));
    }

    private DynamicObject createAsyncFromSyncIterator(Object syncIterator) {
        if (!JSObject.isJSObject(syncIterator)) {
            throw Errors.createTypeError("Object expected");
        }
        DynamicObject obj = JSObject.create(context.getRealm(), context.getRealm().getAsyncFromSyncIteratorPrototype(), JSUserObject.INSTANCE);
        setState.setValue(obj, syncIterator);
        return obj;
    }
}
