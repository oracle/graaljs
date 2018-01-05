/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.access;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.runtime.builtins.JSArgumentsObject;

@NodeChildren({@NodeChild("argumentsArray"), @NodeChild("rhs")})
public abstract class JSGuardDisconnectedArgumentWrite extends JavaScriptNode implements WriteNode {
    private final int index;
    @Child private WriteElementNode argumentsArrayAccess;

    JSGuardDisconnectedArgumentWrite(int index, WriteElementNode argumentsArrayAccess) {
        this.index = index;
        this.argumentsArrayAccess = argumentsArrayAccess;
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
            argumentsArrayAccess.executeWithTargetAndIndexAndValue(argumentsArray, index, value);
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
            argumentsArrayAccess.executeWithTargetAndIndexAndValue(argumentsArray, index, value);
        }
        return value;
    }

    @Override
    public final Object executeWrite(VirtualFrame frame, Object value) {
        throw new UnsupportedOperationException();
    }

    abstract JavaScriptNode getArgumentsArray();

    @Override
    protected JavaScriptNode copyUninitialized() {
        return JSGuardDisconnectedArgumentWriteNodeGen.create(index, cloneUninitialized(argumentsArrayAccess), cloneUninitialized(getArgumentsArray()), cloneUninitialized(getRhs()));
    }
}
