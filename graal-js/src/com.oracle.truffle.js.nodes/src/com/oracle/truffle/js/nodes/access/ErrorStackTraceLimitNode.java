/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.access;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.cast.JSToIntegerNode;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSErrorType;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.builtins.JSError;

public abstract class ErrorStackTraceLimitNode extends JavaScriptBaseNode {
    @Child private RealmNode realmNode;
    @Child private PropertyGetNode getStackTraceLimit;
    @Child private JSToIntegerNode toInteger;

    protected ErrorStackTraceLimitNode(JSContext context) {
        this.realmNode = RealmNode.create(context);
        this.getStackTraceLimit = PropertyGetNode.create(JSError.STACK_TRACE_LIMIT_PROPERTY_NAME, false, context);
        this.toInteger = JSToIntegerNode.create();
    }

    public static ErrorStackTraceLimitNode create(JSContext context) {
        return ErrorStackTraceLimitNodeGen.create(context);
    }

    @Specialization
    public int doInt(VirtualFrame frame) {
        JSRealm realm = realmNode.execute(frame);
        DynamicObject errorConstructor = realm.getErrorConstructor(JSErrorType.Error).getFunctionObject();
        return Math.max(0, toInteger.executeInt(getStackTraceLimit.getValue(errorConstructor)));
    }

    public abstract int executeInt(VirtualFrame frame);

}
