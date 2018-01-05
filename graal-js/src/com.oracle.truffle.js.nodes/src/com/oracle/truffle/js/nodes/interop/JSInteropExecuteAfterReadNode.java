/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.interop;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.access.PropertyGetNode;
import com.oracle.truffle.js.nodes.function.AbstractFunctionArgumentsNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.builtins.JSFunction;

public class JSInteropExecuteAfterReadNode extends JavaScriptBaseNode {

    @CompilationFinal private String name;

    private final int nameIndex;

    @Child private JSFunctionCallNode call;
    @Child private JavaScriptNode targetNode;
    @Child private PropertyGetNode functionPropertyGetNode;
    @Child private AbstractFunctionArgumentsNode argumentsNode;

    private final JSContext context;
    private final int arity;

    public JSInteropExecuteAfterReadNode(int arity, JSContext context) {
        this.nameIndex = 0;
        this.context = context;
        this.arity = arity;
    }

    public Object execute(VirtualFrame frame) {
        if (call == null || name == null || functionPropertyGetNode == null || argumentsNode == null || targetNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            name = (String) ForeignAccess.getArguments(frame).get(nameIndex);
            functionPropertyGetNode = insert(PropertyGetNode.create(name, false, context));
            argumentsNode = insert(new JSInteropArgumentsNode(arity, 1));
            targetNode = insert(new JSInteropReceiverNode());
            call = insert(JSFunctionCallNode.createCall());
        }

        if (name.equals(ForeignAccess.getArguments(frame).get(nameIndex))) {
            Object receiver = targetNode.execute(frame);
            Object function = functionPropertyGetNode.getValue(ForeignAccess.getReceiver(frame));
            if (JSFunction.isJSFunction(function)) {
                Object[] arguments = JSArguments.createInitial(receiver, function, argumentsNode.getCount(frame));
                Object[] preparedArguments = argumentsNode.executeFillObjectArray(frame, arguments, JSArguments.RUNTIME_ARGUMENT_COUNT);
                return call.executeCall(preparedArguments);
            } else {
                throw UnknownIdentifierException.raise(name);
            }
        } else {
            CompilerDirectives.transferToInterpreter();
            throw new IllegalStateException("Name changed");
        }
    }
}
