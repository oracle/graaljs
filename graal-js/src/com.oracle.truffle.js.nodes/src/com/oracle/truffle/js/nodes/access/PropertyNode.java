/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.access;

import java.util.Objects;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.ReadNode;
import com.oracle.truffle.js.nodes.tags.JSSpecificTags;
import com.oracle.truffle.js.nodes.tags.NodeObjectDescriptor;
import com.oracle.truffle.js.nodes.tags.JSSpecificTags.PropertyReadTag;
import com.oracle.truffle.js.runtime.JSContext;

public class PropertyNode extends JSTargetableNode implements ReadNode {

    @Child private JavaScriptNode target;
    @Child private PropertyGetNode cache;

    @Override
    public boolean hasTag(Class<? extends Tag> tag) {
        if (tag == PropertyReadTag.class) {
            return true;
        }
        return super.hasTag(tag);
    }

    @Override
    public Object getNodeObject() {
        NodeObjectDescriptor descriptor = JSSpecificTags.createNodeObjectDescriptor();
        descriptor.addProperty("key", getPropertyKey());
        return descriptor;
    }

    protected PropertyNode(JSContext context, JavaScriptNode target, Object propertyKey) {
        this.target = target;
        this.cache = PropertyGetNode.create(propertyKey, false, context);
    }

    public static PropertyNode createProperty(JSContext ctx, JavaScriptNode target, Object propertyKey) {
        PropertyNode node = new PropertyNode(ctx, target, propertyKey);
        return node;
    }

    public static PropertyNode createMethod(JSContext ctx, JavaScriptNode target, Object propertyKey) {
        PropertyNode propertyNode = new PropertyNode(ctx, target, propertyKey);
        propertyNode.setMethod();
        return propertyNode;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        Object targetValue = evaluateTarget(frame);
        return executeWithTarget(targetValue, evaluateReceiver(frame, targetValue));
    }

    @Override
    public Object executeWithTarget(VirtualFrame frame, Object targetValue) {
        return executeWithTarget(targetValue, targetValue);
    }

    public Object executeWithTarget(Object targetValue) {
        return executeWithTarget(targetValue, targetValue);
    }

    public Object executeWithTarget(Object targetValue, Object receiverValue) {
        return cache.getValue(targetValue, receiverValue);
    }

    @Override
    public int executeInt(VirtualFrame frame) throws UnexpectedResultException {
        Object targetValue = evaluateTarget(frame);
        return executeInt(targetValue, evaluateReceiver(frame, targetValue));
    }

    public int executeInt(Object targetValue) throws UnexpectedResultException {
        return executeInt(targetValue, targetValue);
    }

    public int executeInt(Object targetValue, Object receiverValue) throws UnexpectedResultException {
        return cache.getValueInt(targetValue, receiverValue);
    }

    @Override
    public double executeDouble(VirtualFrame frame) throws UnexpectedResultException {
        Object targetValue = evaluateTarget(frame);
        return executeDouble(targetValue, evaluateReceiver(frame, targetValue));
    }

    public double executeDouble(Object targetValue) throws UnexpectedResultException {
        return executeDouble(targetValue, targetValue);
    }

    public double executeDouble(Object targetValue, Object receiverValue) throws UnexpectedResultException {
        return cache.getValueDouble(targetValue, receiverValue);
    }

    @Override
    public final Object evaluateTarget(VirtualFrame frame) {
        return target.execute(frame);
    }

    public final Object evaluateReceiver(VirtualFrame frame, Object targetValue) {
        if (!(target instanceof SuperPropertyReferenceNode)) {
            return targetValue;
        } else {
            return ((SuperPropertyReferenceNode) target).getThisValue().execute(frame);
        }
    }

    @Override
    public JavaScriptNode getTarget() {
        return target;
    }

    public Object getPropertyKey() {
        return cache.getKey();
    }

    @Override
    @TruffleBoundary
    public String toString() {
        return super.toString() + " property = " + cache.getKey();
    }

    public void setMethod() {
        cache.setMethod();
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        PropertyNode copy = new PropertyNode(cache.getContext(), cloneUninitialized(target), cache.getKey());
        if (this.cache.isMethod()) {
            copy.cache.setMethod();
        }
        return copy;
    }

    @Override
    public String expressionToString() {
        if (target != null) {
            return Objects.toString(target.expressionToString(), INTERMEDIATE_VALUE) + "." + getPropertyKey();
        }
        return null;
    }
}
