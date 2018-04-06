/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.control;

import java.util.Set;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.InstrumentableNode;
import com.oracle.truffle.api.instrumentation.StandardTags.ExpressionTag;
import com.oracle.truffle.api.instrumentation.StandardTags.StatementTag;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.cast.JSToBooleanNode;
import com.oracle.truffle.js.nodes.instrumentation.JSTags;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.ControlFlowBlockTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.ControlFlowBranchTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.ControlFlowRootTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTaggedExecutionNode;
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
        } else if (tag == ExpressionTag.class) {
            // We assume that all if statements are expressions if not statements.
            // This enables instrumentation for cases like: 100 > 0 ? true : false;
            return !hasTag(StatementTag.class);
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
            transferSourceSection(this, newIf);
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
                        materializedTags.contains(ControlFlowBlockTag.class) || materializedTags.contains(StatementTag.class) || materializedTags.contains(ExpressionTag.class);
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
