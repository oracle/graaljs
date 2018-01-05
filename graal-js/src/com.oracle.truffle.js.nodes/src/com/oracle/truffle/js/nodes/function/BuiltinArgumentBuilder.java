/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.function;

import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.NodeFactory;
import com.oracle.truffle.js.runtime.JSContext;

@SuppressWarnings("hiding")
public class BuiltinArgumentBuilder {
    private int fixedArgumentCount;
    private boolean hasThis;
    private boolean varArgs;
    private boolean newTarget;
    private boolean hasFunction;
    public static final JavaScriptNode[] EMPTY_NODE_ARRAY = new JavaScriptNode[0];

    BuiltinArgumentBuilder() {
    }

    public static BuiltinArgumentBuilder builder() {
        boolean ea = false;
        assert !!(ea = true);
        if (ea) {
            return new BuiltinArgumentBuilder() {
                private int order;

                @Override
                protected void assertOrder(int order) {
                    assert this.order < order;
                    this.order = order;
                }
            };
        }
        return new BuiltinArgumentBuilder();
    }

    public BuiltinArgumentBuilder withThis() {
        return withThis(true);
    }

    public BuiltinArgumentBuilder withThis(boolean hasThis) {
        this.hasThis = hasThis;
        assertOrder(1);
        return this;
    }

    public BuiltinArgumentBuilder function() {
        return function(true);
    }

    public BuiltinArgumentBuilder function(boolean function) {
        this.hasFunction = function;
        assertOrder(2);
        return this;
    }

    public BuiltinArgumentBuilder newTarget() {
        return newTarget(true);
    }

    public BuiltinArgumentBuilder newTarget(boolean newTarget) {
        this.newTarget = newTarget;
        assertOrder(3);
        return this;
    }

    public BuiltinArgumentBuilder fixedArgs(int fixedArgumentCount) {
        this.fixedArgumentCount = fixedArgumentCount;
        assertOrder(4);
        return this;
    }

    public BuiltinArgumentBuilder varArgs() {
        return varArgs(true);
    }

    public BuiltinArgumentBuilder varArgs(boolean varArgs) {
        this.varArgs = varArgs;
        assertOrder(5);
        return this;
    }

    public JavaScriptNode[] createArgumentNodes(JSContext context) {
        NodeFactory factory = NodeFactory.getInstance(context);
        int totalArgs = getTotalArgumentCount();
        JavaScriptNode[] callArgs = totalArgs == 0 ? EMPTY_NODE_ARRAY : new JavaScriptNode[totalArgs];
        int index = 0;
        if (hasThis) {
            callArgs[index++] = factory.createAccessThis();
        }
        if (hasFunction) {
            callArgs[index++] = factory.createAccessCallee(0);
        }
        int argIndex = 0;
        int size = varArgs ? totalArgs - 1 : totalArgs;
        for (int i = index; i < size; i++) {
            callArgs[i] = factory.createAccessArgument(argIndex++);
        }
        if (varArgs) {
            callArgs[size] = factory.createAccessVarArgs(argIndex++);
        }
        return callArgs;
    }

    private int getTotalArgumentCount() {
        return (hasThis ? 1 : 0) + (hasFunction ? 1 : 0) + (newTarget ? 1 : 0) + fixedArgumentCount + (varArgs ? 1 : 0);
    }

    /**
     * Ensures that the builder methods are called in a consistent order, which reflects the order
     * of the eventually created argument nodes.
     *
     * @param order desired order
     */
    protected void assertOrder(int order) {
    }
}
