/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.interop;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.js.nodes.JavaScriptNode;

public class JSInteropArgumentNode extends JavaScriptNode {
    private final int index;
    private final boolean convertToJSValue;

    @Child private JSForeignToJSTypeNode toJSType;

    public JSInteropArgumentNode(int index) {
        this(index, false);
    }

    public JSInteropArgumentNode(int index, boolean convertToJSValue) {
        this.index = index;
        this.convertToJSValue = convertToJSValue;
        if (convertToJSValue) {
            this.toJSType = JSForeignToJSTypeNodeGen.create();
        }
    }

    @Override
    public Object execute(VirtualFrame frame) {
        Object value = ForeignAccess.getArguments(frame).get(index);
        if (convertToJSValue) {
            return toJSType.executeWithTarget(value);
        } else {
            return value;
        }
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        return new JSInteropArgumentNode(index, convertToJSValue);
    }
}
