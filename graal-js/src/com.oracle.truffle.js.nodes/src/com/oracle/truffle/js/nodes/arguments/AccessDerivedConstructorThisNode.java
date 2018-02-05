/*
 * Copyright (c) 2012, 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.arguments;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.RepeatableNode;
import com.oracle.truffle.js.runtime.Errors;

public final class AccessDerivedConstructorThisNode extends JavaScriptNode implements RepeatableNode {
    @Child private JavaScriptNode accessThisNode;
    private final BranchProfile errorBranch = BranchProfile.create();

    AccessDerivedConstructorThisNode(JavaScriptNode accessThisNode) {
        this.accessThisNode = accessThisNode;
    }

    public static AccessDerivedConstructorThisNode create(JavaScriptNode accessThisNode) {
        return new AccessDerivedConstructorThisNode(accessThisNode);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        Object thisObj = accessThisNode.execute(frame);
        if (ArgumentsObjectNode.isInitialized(thisObj)) {
            return thisObj;
        } else {
            errorBranch.enter();
            throw Errors.createReferenceError("this is not defined").useCallerRealm();
        }
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        return new AccessDerivedConstructorThisNode(cloneUninitialized(accessThisNode));
    }
}
