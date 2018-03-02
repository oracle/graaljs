/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.function;

import java.util.ArrayList;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.access.JSConstantNode;
import com.oracle.truffle.js.runtime.Boundaries;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSTruffleOptions;

public class JSFunctionArgumentsNode extends AbstractFunctionArgumentsNode {

    @Children protected final JavaScriptNode[] args;

    public static AbstractFunctionArgumentsNode create(JavaScriptNode[] args) {
        for (JavaScriptNode arg : args) {
            if (arg instanceof SpreadArgumentNode) {
                return new SpreadFunctionArgumentsNode(args);
            }
        }
        if (args.length == 0) {
            return new JSFunctionZeroArgumentsNode();
        } else if (args.length == 1) {
            return JSFunctionOneArgumentNode.create(args[0]);
        }
        return new JSFunctionArgumentsNode(args);
    }

    protected JSFunctionArgumentsNode(JavaScriptNode[] args) {
        this.args = args;
        if (args.length > JSTruffleOptions.MaxFunctionArgumentsLength) {
            throw Errors.createSyntaxError("function has too many parameters");
        }
    }

    @Override
    public int getCount(VirtualFrame frame) {
        return args.length;
    }

    @Override
    @ExplodeLoop
    public Object[] executeFillObjectArray(VirtualFrame frame, Object[] arguments, int delta) {
        for (int i = 0; i < args.length; i++) {
            arguments[i + delta] = args[i].execute(frame);
        }
        return arguments;
    }

    @Override
    protected AbstractFunctionArgumentsNode copyUninitialized() {
        return new JSFunctionArgumentsNode(JavaScriptNode.cloneUninitialized(args));
    }
}

class JSFunctionZeroArgumentsNode extends AbstractFunctionArgumentsNode {
    protected JSFunctionZeroArgumentsNode() {
    }

    @Override
    public int getCount(VirtualFrame frame) {
        return 0;
    }

    @Override
    public Object[] executeFillObjectArray(VirtualFrame frame, Object[] arguments, int delta) {
        return arguments;
    }

    @Override
    protected AbstractFunctionArgumentsNode copyUninitialized() {
        return new JSFunctionZeroArgumentsNode();
    }
}

class JSFunctionOneArgumentNode extends AbstractFunctionArgumentsNode {
    @Child private JavaScriptNode child;

    protected JSFunctionOneArgumentNode(JavaScriptNode child) {
        this.child = child;
    }

    public static AbstractFunctionArgumentsNode create(JavaScriptNode child) {
        if (child instanceof JSConstantNode) {
            return new JSFunctionOneConstantArgumentNode(child.execute(null));
        } else {
            return new JSFunctionOneArgumentNode(child);
        }
    }

    public static AbstractFunctionArgumentsNode create(JavaScriptNode child, boolean optimizeConstantArguments) {
        if (optimizeConstantArguments) {
            return create(child);
        } else {
            return new JSFunctionOneArgumentNode(child);
        }
    }

    @Override
    public int getCount(VirtualFrame frame) {
        return 1;
    }

    @Override
    public Object[] executeFillObjectArray(VirtualFrame frame, Object[] arguments, int delta) {
        arguments[delta] = child.execute(frame);
        return arguments;
    }

    @Override
    protected AbstractFunctionArgumentsNode copyUninitialized() {
        return new JSFunctionOneArgumentNode(JavaScriptNode.cloneUninitialized(child));
    }
}

class JSFunctionOneConstantArgumentNode extends AbstractFunctionArgumentsNode {
    private final Object value;

    protected JSFunctionOneConstantArgumentNode(Object value) {
        assert !(value instanceof Node);
        this.value = value;
    }

    @Override
    public int getCount(VirtualFrame frame) {
        return 1;
    }

    @Override
    public Object[] executeFillObjectArray(VirtualFrame frame, Object[] arguments, int delta) {
        arguments[delta] = value;
        return arguments;
    }

    @Override
    protected AbstractFunctionArgumentsNode copyUninitialized() {
        return new JSFunctionOneConstantArgumentNode(value);
    }

    public final Object getValue() {
        return value;
    }
}

class SpreadFunctionArgumentsNode extends JSFunctionArgumentsNode {
    protected SpreadFunctionArgumentsNode(JavaScriptNode[] args) {
        super(args);
    }

    @Override
    public int getCount(VirtualFrame frame) {
        return args.length;
    }

    @Override
    @ExplodeLoop
    public Object[] executeFillObjectArray(VirtualFrame frame, Object[] arguments, int delta) {
        ArrayList<Object> argList = new ArrayList<>(args.length + delta);
        for (int i = 0; i < delta; i++) {
            Boundaries.listAdd(argList, arguments[i]);
        }
        for (int i = 0; i < args.length; i++) {
            if (args[i] instanceof SpreadArgumentNode) {
                ((SpreadArgumentNode) args[i]).executeToList(frame, argList);
            } else {
                Boundaries.listAdd(argList, args[i].execute(frame));
            }
        }
        return Boundaries.listToArray(argList);
    }

    @Override
    protected AbstractFunctionArgumentsNode copyUninitialized() {
        return new SpreadFunctionArgumentsNode(JavaScriptNode.cloneUninitialized(args));
    }
}
