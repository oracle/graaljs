/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.access;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.js.runtime.JSContext;

public class DeclareGlobalLexicalVariableNode extends DeclareGlobalNode {
    public DeclareGlobalLexicalVariableNode(String varName) {
        super(varName);
    }

    @Override
    public void executeVoid(VirtualFrame frame, JSContext context) {
    }

    @Override
    public boolean isLexicallyDeclared() {
        return true;
    }

    @Override
    protected DeclareGlobalNode copyUninitialized() {
        return new DeclareGlobalLexicalVariableNode(varName);
    }
}
