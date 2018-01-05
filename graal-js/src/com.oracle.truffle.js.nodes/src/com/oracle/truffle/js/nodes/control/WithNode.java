/*
 * Copyright (c) 2012, 2014, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.control;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.js.nodes.JavaScriptNode;

public final class WithNode extends StatementNode {

    @Child private JavaScriptNode statement;
    @Child private JavaScriptNode writeActiveObject;

    private WithNode(JavaScriptNode expression, JavaScriptNode statement) {
        this.writeActiveObject = expression;
        this.statement = statement;
    }

    public static WithNode create(JavaScriptNode expression, JavaScriptNode statement) {
        return new WithNode(expression, statement);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        writeActiveObject.executeVoid(frame);
        return statement.execute(frame);
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        return create(cloneUninitialized(writeActiveObject), cloneUninitialized(statement));
    }
}
