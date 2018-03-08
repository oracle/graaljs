/*
 * Copyright (c) 2013, 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.access;

import java.util.Set;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.InstrumentableNode;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.js.nodes.JSTypesGen;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.ReadNode;
import com.oracle.truffle.js.nodes.instrumentation.JSTags;
import com.oracle.truffle.js.nodes.instrumentation.NodeObjectDescriptor;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.ReadPropertyExpressionTag;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSTruffleOptions;

public class GlobalPropertyNode extends JSTargetableNode implements ReadNode {

    private final String propertyName;
    private final JSContext context;
    @Child private PropertyGetNode cache;
    @Child private GlobalObjectNode globalObjectNode;

    protected GlobalPropertyNode(JSContext context, String propertyName) {
        this.propertyName = propertyName;
        this.context = context;
    }

    public static JSTargetableNode createPropertyNode(JSContext ctx, String propertyName) {
        if (JSTruffleOptions.NashornExtensions) {
            if (propertyName.equals("__LINE__")) {
                return new GlobalConstantNode(ctx, propertyName, new GlobalConstantNode.LineNumberNode());
            } else if (propertyName.equals("__FILE__")) {
                return new GlobalConstantNode(ctx, propertyName, new GlobalConstantNode.FileNameNode());
            } else if (propertyName.equals("__DIR__")) {
                return new GlobalConstantNode(ctx, propertyName, new GlobalConstantNode.DirNameNode());
            }
        }
        return new GlobalPropertyNode(ctx, propertyName);
    }

    @Override
    public boolean hasTag(Class<? extends Tag> tag) {
        if (tag == ReadPropertyExpressionTag.class) {
            return true;
        } else {
            return super.hasTag(tag);
        }
    }

    @Override
    public Object getNodeObject() {
        NodeObjectDescriptor descriptor = JSTags.createNodeObjectDescriptor();
        descriptor.addProperty("key", getPropertyKey());
        return descriptor;
    }

    @Override
    public InstrumentableNode materializeInstrumentableNodes(Set<Class<? extends Tag>> materializedTags) {
        if (materializedTags.contains(ReadPropertyExpressionTag.class)) {
            GlobalObjectNode globalObject = GlobalObjectNode.create(context);
            PropertyNode propertyNode = PropertyNode.createProperty(context, globalObject, getPropertyKey());
            transferSourceSection(this, propertyNode);
            transferSourceSectionNoTags(this, globalObject);
            return propertyNode;
        } else {
            return this;
        }
    }

    @Override
    public Object executeWithTarget(VirtualFrame frame, Object target) {
        try {
            return getCache().getValue(JSTypesGen.expectDynamicObject(target));
        } catch (UnexpectedResultException e) {
            throw new AssertionError("target must always be a JSObject");
        }
    }

    @Override
    public final Object evaluateTarget(VirtualFrame frame) {
        return GlobalObjectNode.getGlobalObject(context);
    }

    @Override
    public JavaScriptNode getTarget() {
        return getGlobalObjectNode();
    }

    @Override
    public Object execute(VirtualFrame frame) {
        return getCache().getValue(evaluateTarget(frame));
    }

    @Override
    public int executeInt(VirtualFrame frame) throws UnexpectedResultException {
        return getCache().getValueInt(evaluateTarget(frame));
    }

    @Override
    public double executeDouble(VirtualFrame frame) throws UnexpectedResultException {
        return getCache().getValueDouble(evaluateTarget(frame));
    }

    public String getPropertyKey() {
        return propertyName;
    }

    public void setMethod() {
        getCache().setMethod();
    }

    public void setPropertyAssumptionCheckEnabled(boolean enabled) {
        getCache().setPropertyAssumptionCheckEnabled(enabled);
    }

    private PropertyGetNode getCache() {
        if (cache == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            this.cache = insert(PropertyGetNode.create(propertyName, true, context));
        }
        return cache;
    }

    private GlobalObjectNode getGlobalObjectNode() {
        if (globalObjectNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            this.globalObjectNode = insert(GlobalObjectNode.create(context));
        }
        return globalObjectNode;
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        GlobalPropertyNode copy = new GlobalPropertyNode(context, propertyName);
        if (this.cache != null && this.cache.isMethod()) {
            copy.getCache().setMethod();
        }
        return copy;
    }

    @Override
    public String expressionToString() {
        return getPropertyKey();
    }
}
