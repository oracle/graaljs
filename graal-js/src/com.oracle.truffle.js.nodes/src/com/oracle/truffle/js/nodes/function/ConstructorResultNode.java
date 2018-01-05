/*
 * Copyright (c) 2015, 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.function;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.access.IsPrimitiveNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.objects.Undefined;

public final class ConstructorResultNode extends JavaScriptNode {
    @Child private JavaScriptNode bodyNode;
    @Child private JavaScriptNode thisNode;
    private final boolean derived;

    @Child private IsPrimitiveNode isPrimitiveNode;
    private final ConditionProfile isObject = ConditionProfile.createBinaryProfile();
    private final ConditionProfile isNotUndefined = ConditionProfile.createBinaryProfile();

    private ConstructorResultNode(boolean derived, JavaScriptNode bodyNode, JavaScriptNode thisNode) {
        this.bodyNode = bodyNode;
        this.derived = derived;
        this.thisNode = thisNode;
    }

    public static JavaScriptNode createBase(JavaScriptNode bodyNode, JavaScriptNode thisNode) {
        return new ConstructorResultNode(false, bodyNode, thisNode);
    }

    public static JavaScriptNode createDerived(JavaScriptNode bodyNode, JavaScriptNode thisNode) {
        return new ConstructorResultNode(true, bodyNode, thisNode);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        Object result = bodyNode.execute(frame);

        // IsObject is replaced with a !IsPrimitive check for JavaInterop,
        // so that non-primitive Java types can be returned from the constructor, too.
        if (isObject.profile(!isPrimitive(result))) {
            return result;
        }

        // If [[ConstructorKind]] == "base" or result is undefined return this, otherwise throw
        if (derived && isNotUndefined.profile(result != Undefined.instance)) {
            // throw this error in the caller realm!
            throw Errors.createTypeError("constructor result not as expected").useCallerRealm();
        }

        Object thisObject = thisNode.execute(frame);
        assert thisObject != JSFunction.CONSTRUCT;
        assert !derived || JSRuntime.isObject(thisObject) || thisObject instanceof Symbol;
        return thisObject;
    }

    private boolean isPrimitive(Object result) {
        if (isPrimitiveNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            isPrimitiveNode = insert(IsPrimitiveNode.create());
        }
        return isPrimitiveNode.executeBoolean(result);
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        return new ConstructorResultNode(derived, cloneUninitialized(bodyNode), cloneUninitialized(thisNode));
    }
}
