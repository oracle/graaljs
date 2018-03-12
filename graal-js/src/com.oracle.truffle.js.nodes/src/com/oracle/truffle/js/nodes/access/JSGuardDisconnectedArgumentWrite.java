/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.access;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Executed;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.runtime.builtins.JSArgumentsObject;

public abstract class JSGuardDisconnectedArgumentWrite extends JavaScriptNode implements WriteNode {
    private final int index;
    @Child @Executed JavaScriptNode argumentsArrayNode;
    @Child @Executed JavaScriptNode rhsNode;
    @Child private WriteElementNode writeArgumentsElementNode;

    JSGuardDisconnectedArgumentWrite(int index, WriteElementNode argumentsArrayAccess, JavaScriptNode argumentsArray, JavaScriptNode rhs) {
        this.index = index;
        this.argumentsArrayNode = argumentsArray;
        this.rhsNode = rhs;
        this.writeArgumentsElementNode = argumentsArrayAccess;
    }

    public static JSGuardDisconnectedArgumentWrite create(int index, WriteElementNode argumentsArrayAccess, JavaScriptNode argumentsArray, JavaScriptNode rhs) {
        return JSGuardDisconnectedArgumentWriteNodeGen.create(index, argumentsArrayAccess, argumentsArray, rhs);
    }

    @Specialization(guards = "!isArgumentsDisconnected(argumentsArray)")
    public Object doObject(DynamicObject argumentsArray, Object value, @Cached("create()") BranchProfile unconnectedBranch) {
        assert JSArgumentsObject.isJSArgumentsObject(argumentsArray);
        if (index >= JSArgumentsObject.getConnectedArgumentCount(argumentsArray)) {
            unconnectedBranch.enter();
            JSArgumentsObject.disconnectIndex(argumentsArray, index, value);
        } else {
            writeArgumentsElementNode.executeWithTargetAndIndexAndValue(argumentsArray, index, value);
        }
        return value;
    }

    @Specialization(guards = "isArgumentsDisconnected(argumentsArray)")
    public Object doObjectDisconnected(DynamicObject argumentsArray, Object value) {
        assert JSArgumentsObject.isJSArgumentsObject(argumentsArray);
        if (JSArgumentsObject.wasIndexDisconnected(argumentsArray, index)) {
            JSArgumentsObject.setDisconnectedIndexValue(argumentsArray, index, value);
        } else if (index >= JSArgumentsObject.getConnectedArgumentCount(argumentsArray)) {
            JSArgumentsObject.disconnectIndex(argumentsArray, index, value);
        } else {
            writeArgumentsElementNode.executeWithTargetAndIndexAndValue(argumentsArray, index, value);
        }
        return value;
    }

    @Override
    public final Object executeWrite(VirtualFrame frame, Object value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public JavaScriptNode getRhs() {
        return rhsNode;
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        return JSGuardDisconnectedArgumentWriteNodeGen.create(index, cloneUninitialized(writeArgumentsElementNode), cloneUninitialized(argumentsArrayNode), cloneUninitialized(rhsNode));
    }
}
