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
package com.oracle.truffle.js.nodes.control;

import java.util.Set;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.InstrumentableNode;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.cast.JSToBooleanNode;
import com.oracle.truffle.js.nodes.instrumentation.JSTaggedExecutionNode;
import com.oracle.truffle.js.nodes.instrumentation.JSTags;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.ControlFlowBlockTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.ControlFlowBranchTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.ControlFlowRootTag;
import com.oracle.truffle.js.nodes.unary.JSNotNode;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.objects.Undefined;

/**
 * 12.5 The if Statement.
 */
@NodeInfo(shortName = "if")
public final class IfNode extends StatementNode implements ResumableNode {

    @Child private JavaScriptNode condition;
    @Child private JavaScriptNode thenPart;
    @Child private JavaScriptNode elsePart;
    private final ConditionProfile conditionProfile = ConditionProfile.createCountingProfile();

    public static IfNode create(JavaScriptNode condition, JavaScriptNode thenPart, JavaScriptNode elsePart) {
        if (condition instanceof JSNotNode) {
            // if (!a) {b()} => if (a) {} else {b(); }
            JavaScriptNode operand = ((JSNotNode) condition).getOperand();
            return new IfNode(operand, elsePart, thenPart);
        }
        return new IfNode(condition, thenPart, elsePart);
    }

    @Override
    public boolean hasTag(Class<? extends Tag> tag) {
        if (tag == ControlFlowRootTag.class) {
            return true;
        }
        return super.hasTag(tag);
    }

    @Override
    public Object getNodeObject() {
        return JSTags.createNodeObjectDescriptor("type", ControlFlowRootTag.Type.Conditional.name());
    }

    @Override
    public InstrumentableNode materializeInstrumentableNodes(Set<Class<? extends Tag>> materializedTags) {
        if (hasMaterializationTag(materializedTags) && materializationNeeded()) {
            JavaScriptNode newElsePart = elsePart != null ? JSTaggedExecutionNode.createFor(elsePart, JSTags.ControlFlowBlockTag.class) : null;
            JavaScriptNode newThenPart = thenPart != null ? JSTaggedExecutionNode.createFor(thenPart, JSTags.ControlFlowBlockTag.class) : null;
            JavaScriptNode newCondition = JSTaggedExecutionNode.createFor(condition, ControlFlowBranchTag.class,
                            JSTags.createNodeObjectDescriptor("type", ControlFlowBranchTag.Type.Condition.name()));
            JavaScriptNode newIf = IfNode.create(newCondition, newThenPart, newElsePart);
            transferSourceSectionAndTags(this, newIf);
            return newIf;
        } else {
            return this;
        }
    }

    private boolean materializationNeeded() {
        // If we are using tagged nodes, this node is already materialized.
        return !(condition instanceof JSTaggedExecutionNode && (elsePart == null || elsePart instanceof JSTaggedExecutionNode) && (thenPart == null || thenPart instanceof JSTaggedExecutionNode));
    }

    private static boolean hasMaterializationTag(Set<Class<? extends Tag>> materializedTags) {
        return materializedTags.contains(ControlFlowRootTag.class) || materializedTags.contains(ControlFlowBranchTag.class) ||
                        materializedTags.contains(ControlFlowBlockTag.class);
    }

    private IfNode(JavaScriptNode condition, JavaScriptNode thenPart, JavaScriptNode elsePart) {
        this.condition = condition;
        this.thenPart = thenPart;
        this.elsePart = elsePart;
    }

    public JavaScriptNode getThenPart() {
        return thenPart;
    }

    public JavaScriptNode getElsePart() {
        return elsePart;
    }

    public JavaScriptNode getCondition() {
        return condition;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        if (conditionProfile.profile(executeCondition(frame))) {
            if (thenPart != null) {
                return thenPart.execute(frame);
            } else {
                return EMPTY;
            }
        } else {
            if (elsePart != null) {
                return elsePart.execute(frame);
            } else {
                return EMPTY;
            }
        }
    }

    @Override
    public void executeVoid(VirtualFrame frame) {
        if (conditionProfile.profile(executeCondition(frame))) {
            if (thenPart != null) {
                thenPart.executeVoid(frame);
            }
        } else {
            if (elsePart != null) {
                elsePart.executeVoid(frame);
            }
        }
    }

    @Override
    public Object resume(VirtualFrame frame) {
        int index = getStateAsIntAndReset(frame);
        if (index == 0 && conditionProfile.profile(executeCondition(frame)) || index == 1) {
            try {
                if (thenPart != null) {
                    return thenPart.execute(frame);
                } else {
                    return EMPTY;
                }
            } catch (YieldException e) {
                setState(frame, 1);
                throw e;
            }
        } else {
            assert index == 0 || index == 2;
            try {
                if (elsePart != null) {
                    return elsePart.execute(frame);
                } else {
                    return EMPTY;
                }
            } catch (YieldException e) {
                setState(frame, 2);
                throw e;
            }
        }
    }

    @Override
    public boolean isResultAlwaysOfType(Class<?> clazz) {
        return isResultAlwaysOfType(thenPart, clazz) && isResultAlwaysOfType(elsePart, clazz);
    }

    private static boolean isResultAlwaysOfType(JavaScriptNode child, Class<?> clazz) {
        if (child == null) {
            return clazz == Undefined.class;
        } else {
            return child.isResultAlwaysOfType(clazz);
        }
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        return new IfNode(cloneUninitialized(condition), cloneUninitialized(thenPart), cloneUninitialized(elsePart));
    }

    protected boolean executeCondition(VirtualFrame frame) {
        try {
            return condition.executeBoolean(frame);
        } catch (UnexpectedResultException ex) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            condition.replace(JSToBooleanNode.create(condition));
            return JSRuntime.toBoolean(ex.getResult());
        }
    }
}
