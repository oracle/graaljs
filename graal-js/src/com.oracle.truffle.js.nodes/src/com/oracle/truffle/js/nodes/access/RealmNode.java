/*
 * Copyright (c) 2015, 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.access;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSFrameUtil;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.builtins.JSFunction;

public class RealmNode extends JavaScriptBaseNode {
    private final JSContext context;

    protected RealmNode(JSContext context) {
        this.context = context;
    }

    public static RealmNode create(JSContext context) {
        return new RealmNode(context);
    }

    public JSRealm execute(VirtualFrame frame) {
        assert context.getRealm() == JSFunction.getRealm(JSFrameUtil.getFunctionObject(frame));
        return context.getRealm();
    }

    public JSContext getContext() {
        return context;
    }
}
