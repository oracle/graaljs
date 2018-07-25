/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Set;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.InstrumentableNode;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.instrumentation.JSTags;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.WritePropertyExpressionTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.WriteVariableExpressionTag;
import com.oracle.truffle.js.runtime.JSContext;

public class WritePropertyNode extends JSTargetableNode implements WriteNode {

    @Child protected JavaScriptNode targetNode;
    @Child protected JavaScriptNode rhsNode;
    @Child protected PropertySetNode cache;

    @CompilationFinal private byte valueState;
    private static final byte VALUE_INT = 1;
    private static final byte VALUE_DOUBLE = 2;
    private static final byte VALUE_OBJECT = 3;

    protected WritePropertyNode(JavaScriptNode target, JavaScriptNode rhs, Object propertyKey, boolean isGlobal, JSContext context, boolean isStrict) {
        this.targetNode = target;
        this.rhsNode = rhs;
        this.cache = PropertySetNode.create(propertyKey, isGlobal, context, isStrict);
    }

    public static WritePropertyNode create(JavaScriptNode target, Object propertyKey, JavaScriptNode rhs, JSContext ctx, boolean isStrict) {
        return create(target, propertyKey, rhs, false, ctx, isStrict);
    }

    public static WritePropertyNode create(JavaScriptNode target, Object propertyKey, JavaScriptNode rhs, boolean isGlobal, JSContext ctx, boolean isStrict) {
        return new WritePropertyNode(target, rhs, propertyKey, isGlobal, ctx, isStrict);
    }

    @Override
    public boolean hasTag(Class<? extends Tag> tag) {
        if (tag == WriteVariableExpressionTag.class && isScopeAccess()) {
            return true;
        } else if (tag == WritePropertyExpressionTag.class && !isScopeAccess()) {
            return true;
        } else {
            return super.hasTag(tag);
        }
    }

    private boolean isScopeAccess() {
        return targetNode instanceof GlobalScopeNode;
    }

    @Override
    public Object getNodeObject() {
        if (isScopeAccess()) {
            return JSTags.createNodeObjectDescriptor("name", getKey());
        }
        return JSTags.createNodeObjectDescriptor("key", getKey());
    }

    @Override
    public InstrumentableNode materializeInstrumentableNodes(Set<Class<? extends Tag>> materializedTags) {
        if (materializedTags.contains(WritePropertyExpressionTag.class) && !isScopeAccess()) {
            // if we have no source section, we must assign one to be discoverable at
            // instrumentation time.
            if (!targetNode.hasSourceSection()) {
                JavaScriptNode clonedTarget = (JavaScriptNode) targetNode.materializeInstrumentableNodes(materializedTags);
                JavaScriptNode clonedRhs = (JavaScriptNode) rhsNode.materializeInstrumentableNodes(materializedTags);
                WritePropertyNode cloneUninitialized = WritePropertyNode.create(clonedTarget, cache.getKey(), clonedRhs, cache.isGlobal(), cache.getContext(), cache.isStrict());
                transferSourceSectionAndTags(this, cloneUninitialized);
                transferSourceSectionAndTags(this, clonedRhs);
                transferSourceSectionAddExpressionTag(this, cloneUninitialized.targetNode);
                return cloneUninitialized;
            }
        }
        return this;
    }

    @Override
    public final JavaScriptNode getTarget() {
        return targetNode;
    }

    @Override
    public final JavaScriptNode getRhs() {
        return rhsNode;
    }

    public final Object getKey() {
        return cache.getKey();
    }

    public final boolean isGlobal() {
        return cache.isGlobal();
    }

    public final Object executeWithValue(Object obj, Object value) {
        cache.setValue(obj, value);
        return value;
    }

    public final int executeIntWithValue(Object obj, int value) {
        cache.setValueInt(obj, value);
        return value;
    }

    public final double executeDoubleWithValue(Object obj, double value) {
        cache.setValueDouble(obj, value);
        return value;
    }

    private Object executeEvaluated(Object obj, Object value, Object receiver) {
        cache.setValue(obj, value, receiver);
        return value;
    }

    private int executeIntEvaluated(Object obj, int value, Object receiver) {
        cache.setValueInt(obj, value, receiver);
        return value;
    }

    private double executeDoubleEvaluated(Object obj, double value, Object receiver) {
        cache.setValueDouble(obj, value, receiver);
        return value;
    }

    @Override
    public final Object execute(VirtualFrame frame) {
        Object target = evaluateTarget(frame);
        Object receiver = evaluateReceiver(frame, target);
        Object value = rhsNode.execute(frame);
        return executeEvaluated(target, value, receiver);
    }

    @Override
    public final int executeInt(VirtualFrame frame) throws UnexpectedResultException {
        Object target = evaluateTarget(frame);
        Object receiver = evaluateReceiver(frame, target);
        try {
            int value = rhsNode.executeInt(frame);
            return executeIntEvaluated(target, value, receiver);
        } catch (UnexpectedResultException e) {
            executeEvaluated(target, e.getResult(), receiver);
            throw e;
        }
    }

    @Override
    public final double executeDouble(VirtualFrame frame) throws UnexpectedResultException {
        Object target = evaluateTarget(frame);
        Object receiver = evaluateReceiver(frame, target);
        try {
            double value = rhsNode.executeDouble(frame);
            return executeDoubleEvaluated(target, value, receiver);
        } catch (UnexpectedResultException e) {
            executeEvaluated(target, e.getResult(), receiver);
            throw e;
        }
    }

    @Override
    public final void executeVoid(VirtualFrame frame) {
        Object target = evaluateTarget(frame);
        Object receiver = evaluateReceiver(frame, target);
        if (valueState == 0) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            executeAndSpecialize(frame, target, receiver);
            return;
        }
        if (valueState == VALUE_INT) {
            try {
                int value = rhsNode.executeInt(frame);
                executeIntEvaluated(target, value, receiver);
            } catch (UnexpectedResultException e) {
                valueState = VALUE_OBJECT;
                executeEvaluated(target, e.getResult(), receiver);
            }
        } else if (valueState == VALUE_DOUBLE) {
            try {
                double value = rhsNode.executeDouble(frame);
                executeDoubleEvaluated(target, value, receiver);
            } catch (UnexpectedResultException e) {
                valueState = VALUE_OBJECT;
                executeEvaluated(target, e.getResult(), receiver);
            }
        } else {
            assert valueState == VALUE_OBJECT;
            Object value = rhsNode.execute(frame);
            executeEvaluated(target, value, receiver);
        }
    }

    private void executeAndSpecialize(VirtualFrame frame, Object target, Object receiver) {
        CompilerAsserts.neverPartOfCompilation();
        Object value = rhsNode.execute(frame);
        if (value instanceof Integer) {
            valueState = VALUE_INT;
            executeIntEvaluated(target, (int) value, receiver);
        } else if (value instanceof Double) {
            valueState = VALUE_DOUBLE;
            executeDoubleEvaluated(target, (double) value, receiver);
        } else {
            valueState = VALUE_OBJECT;
            executeEvaluated(target, value, receiver);
        }
    }

    @Override
    public final Object executeWrite(VirtualFrame frame, Object value) {
        Object target = evaluateTarget(frame);
        Object receiver = evaluateReceiver(frame, target);
        return executeEvaluated(target, value, receiver);
    }

    @Override
    public final Object executeWithTarget(VirtualFrame frame, Object target) {
        Object value = rhsNode.execute(frame);
        return executeEvaluated(target, value, target);
    }

    @Override
    public final Object evaluateTarget(VirtualFrame frame) {
        return targetNode.execute(frame);
    }

    public final Object evaluateReceiver(VirtualFrame frame, Object target) {
        if (!(targetNode instanceof SuperPropertyReferenceNode)) {
            return target;
        } else {
            return ((SuperPropertyReferenceNode) targetNode).getThisValue().execute(frame);
        }
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        return create(cloneUninitialized(targetNode), cache.getKey(), cloneUninitialized(rhsNode), cache.isGlobal(), cache.getContext(), cache.isStrict());
    }

    @Override
    public boolean isResultAlwaysOfType(Class<?> clazz) {
        return getRhs().isResultAlwaysOfType(clazz);
    }
}
