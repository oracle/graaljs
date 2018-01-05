/*
 * Copyright (c) 2012, 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.access;

import java.util.Objects;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.ReadNode;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.objects.Undefined;

/**
 * Wrapper around a variable access in the presence of dynamic scopes induced by direct eval calls.
 */
public final class EvalVariableNode extends JavaScriptNode implements ReadNode, WriteNode {

    @Child private JavaScriptNode defaultDelegate;
    private final String varName;
    @Child private JavaScriptNode dynamicScopeNode;
    @Child private HasPropertyCacheNode hasPropertyNode;
    @Child private JSTargetableNode scopeAccessNode;
    private final JSContext context;

    public EvalVariableNode(JSContext context, String varName, JavaScriptNode defaultDelegate, JavaScriptNode dynamicScope, JSTargetableNode scopeAccessNode) {
        this.varName = varName;
        this.defaultDelegate = Objects.requireNonNull(defaultDelegate);
        this.dynamicScopeNode = dynamicScope;

        this.hasPropertyNode = HasPropertyCacheNode.create(varName, context);
        this.scopeAccessNode = scopeAccessNode;
        this.context = context;
    }

    public String getPropertyName() {
        return varName;
    }

    public JavaScriptNode getDefaultDelegate() {
        return defaultDelegate;
    }

    private boolean isWrite() {
        return scopeAccessNode instanceof WritePropertyNode;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        Object dynamicScope = dynamicScopeNode.execute(frame);
        if (dynamicScope != Undefined.instance && hasPropertyNode.hasProperty(dynamicScope)) {
            if (isWrite()) {
                Object value = ((WriteNode) defaultDelegate).getRhs().execute(frame);
                ((WritePropertyNode) scopeAccessNode).executeWithValue(dynamicScope, value);
                return value;
            } else {
                // read or delete
                return scopeAccessNode.executeWithTarget(frame, dynamicScope);
            }
        }
        return defaultDelegate.execute(frame);
    }

    @Override
    public Object executeWrite(VirtualFrame frame, Object value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public JavaScriptNode getRhs() {
        return ((WriteNode) defaultDelegate).getRhs();
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        return new EvalVariableNode(context, varName, cloneUninitialized(defaultDelegate), cloneUninitialized(dynamicScopeNode), cloneUninitialized(scopeAccessNode));
    }
}
