/*
 * Copyright (c) 2012, 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.arguments;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.RepeatableNode;
import com.oracle.truffle.js.runtime.builtins.JSFunction;

public final class AccessLexicalThisNode extends JavaScriptNode implements RepeatableNode {
    @Child private JavaScriptNode readFunctionObject;

    AccessLexicalThisNode(JavaScriptNode readFunctionObject) {
        this.readFunctionObject = readFunctionObject;
    }

    public static AccessLexicalThisNode create(JavaScriptNode readFunctionObject) {
        return new AccessLexicalThisNode(readFunctionObject);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        try {
            DynamicObject function = readFunctionObject.executeDynamicObject(frame);
            return JSFunction.getLexicalThis(function);
        } catch (UnexpectedResultException e) {
            throw new AssertionError();
        }
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        return create(cloneUninitialized(readFunctionObject));
    }
}
