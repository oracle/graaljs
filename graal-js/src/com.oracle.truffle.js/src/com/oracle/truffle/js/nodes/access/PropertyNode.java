/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The Universal Permissive License (UPL), Version 1.0
 *
 * Subject to the condition set forth below, permission is hereby granted to any
 * person obtaining a copy of this software, associated documentation and/or
 * data (collectively the "Software"), free of charge and under any and all
 * copyright rights in the Software, and any and all patent rights owned or
 * freely licensable by each licensor hereunder covering either (i) the
 * unmodified Software as contributed to or provided by such licensor, or (ii)
 * the Larger Works (as defined below), to deal in both
 *
 * (a) the Software, and
 *
 * (b) any piece of software and/or hardware listed in the lrgrwrks.txt file if
 * one is included with the Software each a "Larger Work" to which the Software
 * is contributed by such licensors),
 *
 * without restriction, including without limitation the rights to copy, create
 * derivative works of, display, perform, and distribute the Software and make,
 * use, sell, offer for sale, import, export, have made, and have sold the
 * Software and the Larger Work(s), and to sublicense the foregoing rights on
 * either these or other terms.
 *
 * This license is subject to the following condition:
 *
 * The above copyright notice and either this complete permission notice or at a
 * minimum a reference to the UPL must be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.oracle.truffle.js.nodes.access;

import java.util.Objects;
import java.util.Set;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.InstrumentableNode;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.js.nodes.JSNodeUtil;
import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.ReadNode;
import com.oracle.truffle.js.nodes.instrumentation.JSTaggedExecutionNode;
import com.oracle.truffle.js.nodes.instrumentation.JSTags;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.InputNodeTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.ReadPropertyTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.ReadVariableTag;
import com.oracle.truffle.js.nodes.instrumentation.NodeObjectDescriptor;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;

public class PropertyNode extends JSTargetableNode implements ReadNode {

    @Child private JavaScriptNode target;
    @Child private PropertyGetNode cache;

    @Override
    public boolean hasTag(Class<? extends Tag> tag) {
        if ((tag == ReadVariableTag.class || tag == StandardTags.ReadVariableTag.class) && isScopeAccess()) {
            return true;
        } else if (tag == ReadPropertyTag.class && !isScopeAccess()) {
            return true;
        } else if (tag == InputNodeTag.class) {
            return true;
        } else {
            return super.hasTag(tag);
        }
    }

    private boolean isScopeAccess() {
        return target instanceof GlobalScopeNode;
    }

    @Override
    public Object getNodeObject() {
        if (isScopeAccess()) {
            NodeObjectDescriptor descriptor = JSTags.createNodeObjectDescriptor("name", getPropertyKey());
            descriptor.addProperty(StandardTags.ReadVariableTag.NAME, getPropertyKey());
            return descriptor;
        }
        return JSTags.createNodeObjectDescriptor("key", getPropertyKey());
    }

    @Override
    public InstrumentableNode materializeInstrumentableNodes(Set<Class<? extends Tag>> materializedTags) {
        if (materializedTags.contains(ReadPropertyTag.class) && !isScopeAccess()) {
            if (JSNodeUtil.isTaggedNode(target)) {
                // this node is already materialized
                return this;
            }
            JavaScriptNode clonedTarget = JSTaggedExecutionNode.createForInput(target, this, materializedTags);
            if (clonedTarget == target) {
                return this;
            }
            PropertyNode propertyNode = new PropertyNode(cache.getContext(), clonedTarget, cache.getKey(), cache.isOwnProperty(), cache.isMethod());
            transferSourceSectionAndTags(this, propertyNode);
            return propertyNode;
        }
        return this;
    }

    protected PropertyNode(JSContext context, JavaScriptNode target, Object propertyKey, boolean getOwnProperty, boolean method) {
        this.target = target;
        this.cache = PropertyGetNode.create(propertyKey, false, context, getOwnProperty, method);
    }

    public static PropertyNode createProperty(JSContext ctx, JavaScriptNode target, Object propertyKey, boolean method) {
        assert JSRuntime.isPropertyKey(propertyKey);
        return new PropertyNode(ctx, target, propertyKey, false, method);
    }

    public static PropertyNode createProperty(JSContext ctx, JavaScriptNode target, Object propertyKey) {
        return createProperty(ctx, target, propertyKey, false);
    }

    public static PropertyNode createMethod(JSContext ctx, JavaScriptNode target, Object propertyKey) {
        return createProperty(ctx, target, propertyKey, true);
    }

    public static PropertyNode createGetHidden(JSContext ctx, JavaScriptNode target, HiddenKey hiddenKey) {
        return new PropertyNode(ctx, target, hiddenKey, true, false);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        Object targetValue = evaluateTarget(frame);
        return executeWithTarget(targetValue, evaluateReceiver(target, frame, targetValue));
    }

    @Override
    public Object executeWithTarget(VirtualFrame frame, Object targetValue) {
        return executeWithTarget(targetValue, targetValue);
    }

    public Object executeWithTarget(Object targetValue) {
        return executeWithTarget(targetValue, targetValue);
    }

    public Object executeWithTarget(Object targetValue, Object receiverValue) {
        return cache.getValueOrUndefined(targetValue, receiverValue);
    }

    @Override
    public int executeInt(VirtualFrame frame) throws UnexpectedResultException {
        Object targetValue = evaluateTarget(frame);
        return executeInt(targetValue, evaluateReceiver(target, frame, targetValue));
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
        return executeDouble(targetValue, evaluateReceiver(target, frame, targetValue));
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

    @Override
    protected JavaScriptNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
        return new PropertyNode(cache.getContext(), cloneUninitialized(target, materializedTags), cache.getKey(), cache.isOwnProperty(), cache.isMethod());
    }

    @Override
    public String expressionToString() {
        if (target != null) {
            return Objects.toString(target.expressionToString(), INTERMEDIATE_VALUE) + "." + getPropertyKey();
        }
        return null;
    }

    public final boolean isOwnProperty() {
        return cache.isOwnProperty();
    }

    public final boolean isMethod() {
        return cache.isMethod();
    }

    public JSContext getContext() {
        return cache.getContext();
    }
}
