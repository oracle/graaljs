/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.access;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.ReadNode;
import com.oracle.truffle.js.nodes.RepeatableNode;
import com.oracle.truffle.js.runtime.builtins.JSArgumentsObject;
import com.oracle.truffle.js.runtime.objects.Undefined;

@NodeChild("argumentsArray")
public abstract class JSGuardDisconnectedArgumentRead extends JavaScriptNode implements RepeatableNode, ReadNode {
    private final int index;
    @Child private ReadElementNode readElementNode;

    JSGuardDisconnectedArgumentRead(int index, ReadElementNode readElementNode) {
        this.index = index;
        this.readElementNode = readElementNode;
    }

    public static JSGuardDisconnectedArgumentRead create(int index, ReadElementNode readElementNode, JavaScriptNode argumentsArray) {
        return JSGuardDisconnectedArgumentReadNodeGen.create(index, readElementNode, argumentsArray);
    }

    @Specialization(guards = "!isArgumentsDisconnected(argumentsArray)")
    public Object doObject(DynamicObject argumentsArray, @Cached("createBinaryProfile()") ConditionProfile unconnectedProfile) {
        assert JSArgumentsObject.isJSArgumentsObject(argumentsArray);
        if (unconnectedProfile.profile(index >= JSArgumentsObject.getConnectedArgumentCount(argumentsArray))) {
            return Undefined.instance;
        } else {
            return readElementNode.executeWithTargetAndIndex(argumentsArray, index);
        }
    }

    public final int getIndex() {
        return index;
    }

    @Specialization(guards = "isArgumentsDisconnected(argumentsArray)")
    public Object doObjectDisconnected(DynamicObject argumentsArray) {
        assert JSArgumentsObject.isJSArgumentsObject(argumentsArray);
        if (JSArgumentsObject.wasIndexDisconnected(argumentsArray, index)) {
            return JSArgumentsObject.getDisconnectedIndexValue(argumentsArray, index);
        } else if (index >= JSArgumentsObject.getConnectedArgumentCount(argumentsArray)) {
            return Undefined.instance;
        } else {
            return readElementNode.executeWithTargetAndIndex(argumentsArray, index);
        }
    }

    abstract JavaScriptNode getArgumentsArray();

    @Override
    protected JavaScriptNode copyUninitialized() {
        return JSGuardDisconnectedArgumentReadNodeGen.create(index, cloneUninitialized(readElementNode), cloneUninitialized(getArgumentsArray()));
    }
}
