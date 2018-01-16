/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.interop;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.access.PropertyGetNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.builtins.JSFunction;

public class JSInteropInvokeNode extends JavaScriptBaseNode {
    private final JSContext context;

    @CompilationFinal private String cachedName;

    @Child private JSFunctionCallNode call;
    @Child private PropertyGetNode functionPropertyGetNode;

    public JSInteropInvokeNode(JSContext context) {
        this.context = context;
    }

    public Object execute(DynamicObject receiver, String name, Object[] arguments) {
        if (call == null || cachedName == null || functionPropertyGetNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            cachedName = name;
            functionPropertyGetNode = insert(PropertyGetNode.create(cachedName, false, context));
            call = insert(JSFunctionCallNode.createCall());
        }

        if (cachedName.equals(name)) {
            Object function = functionPropertyGetNode.getValue(receiver);
            if (JSFunction.isJSFunction(function)) {
                return call.executeCall(JSArguments.create(receiver, function, arguments));
            } else {
                throw UnknownIdentifierException.raise(cachedName);
            }
        } else {
            CompilerDirectives.transferToInterpreter();
            throw new IllegalStateException("Name changed");
        }
    }
}
