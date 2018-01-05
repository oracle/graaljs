/*
 * Copyright (c) 2012, 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.access;

import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.runtime.JSContext;

public final class JSGetLengthNode extends JavaScriptBaseNode {
    @Child private GetLengthHelperNode getLengthHelperNode;
    @Child private IsArrayNode isArrayNode;

    private JSGetLengthNode(JSContext context) {
        this.getLengthHelperNode = GetLengthHelperNode.create(context);
        this.isArrayNode = IsArrayNode.createIsArray();
    }

    public static JSGetLengthNode create(JSContext context) {
        return new JSGetLengthNode(context);
    }

    public Object execute(TruffleObject value) {
        return getLengthHelperNode.execute(value, isArrayNode.execute(value));
    }

    public long executeLong(TruffleObject value) {
        return getLengthHelperNode.executeLong(value, isArrayNode.execute(value));
    }
}
