/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.access;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.runtime.JSContext;

public abstract class DeclareGlobalNode extends JavaScriptBaseNode {
    protected final String varName;

    protected DeclareGlobalNode(String varName) {
        this.varName = varName;
    }

    public abstract void executeVoid(VirtualFrame frame, JSContext context);

    public boolean isLexicallyDeclared() {
        return false;
    }

    protected abstract DeclareGlobalNode copyUninitialized();
}
