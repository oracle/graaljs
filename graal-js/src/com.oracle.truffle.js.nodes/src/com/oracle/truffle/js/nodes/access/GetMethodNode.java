/*
 * Copyright (c) 2015, 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.access;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.objects.Null;
import com.oracle.truffle.js.runtime.objects.Undefined;

/**
 * ES6 7.3.9 GetMethod(O, P).
 */
public class GetMethodNode extends JSTargetableNode {
    @Child private JavaScriptNode targetNode;
    @Child private PropertyGetNode cacheNode;
    @Child private IsJSClassNode isCallableNode;
    private final ConditionProfile undefinedOrNull = ConditionProfile.createBinaryProfile();
    private final BranchProfile notCallableBranch = BranchProfile.create();

    protected GetMethodNode(JSContext context, JavaScriptNode target, Object propertyKey) {
        this.targetNode = target;
        this.cacheNode = PropertyGetNode.create(propertyKey, false, context);
        this.isCallableNode = IsJSClassNode.create(JSFunction.INSTANCE);
    }

    public static GetMethodNode create(JSContext ctx, JavaScriptNode target, Object key) {
        return new GetMethodNode(ctx, target, key);
    }

    @Override
    public Object executeWithTarget(VirtualFrame frame, Object target) {
        return executeWithTarget(target);
    }

    public Object executeWithTarget(Object target) {
        Object method = cacheNode.getValue(target);
        if (isCallableNode.executeBoolean(method)) {
            return method;
        } else if (undefinedOrNull.profile(method == Undefined.instance || method == Null.instance)) {
            return Undefined.instance;
        } else {
            notCallableBranch.enter();
            throw Errors.createTypeErrorNotAFunction(method);
        }
    }

    @Override
    public Object evaluateTarget(VirtualFrame frame) {
        return targetNode.execute(frame);
    }

    @Override
    public JavaScriptNode getTarget() {
        return targetNode;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        return executeWithTarget(evaluateTarget(frame));
    }

    final JSContext getContext() {
        return cacheNode.getContext();
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        return new GetMethodNode(getContext(), cloneUninitialized(targetNode), cacheNode.getKey());
    }
}
