/*
 * Copyright (c) 2012, 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.control;

import java.util.Set;

import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.InstrumentableNode;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.nodes.LoopNode;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.nodes.RepeatingNode;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.access.JSConstantNode;
import com.oracle.truffle.js.nodes.instrumentation.JSTaggedExecutionNode;
import com.oracle.truffle.js.nodes.instrumentation.JSTags;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.ControlFlowBlockTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.ControlFlowBranchTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.ControlFlowRootTag;
import com.oracle.truffle.js.nodes.unary.VoidNode;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.objects.Undefined;

/**
 * 12.6.2 The while Statement.
 */
@NodeInfo(shortName = "while")
public final class WhileNode extends StatementNode {

    @Child private LoopNode loop;

    private WhileNode(RepeatingNode repeatingNode) {
        this.loop = Truffle.getRuntime().createLoopNode(repeatingNode);
    }

    public static JavaScriptNode createWhileDo(JavaScriptNode condition, JavaScriptNode body) {
        if (condition instanceof JSConstantNode && !JSRuntime.toBoolean(((JSConstantNode) condition).getValue())) {
            return new EmptyNode();
        }
        JavaScriptNode nonVoidBody = body instanceof VoidNode ? ((VoidNode) body).getOperand() : body;
        return new WhileNode(new WhileDoRepeatingNode(condition, nonVoidBody));
    }

    public static JavaScriptNode createDoWhile(JavaScriptNode condition, JavaScriptNode body) {
        if (condition instanceof JSConstantNode && !JSRuntime.toBoolean(((JSConstantNode) condition).getValue())) {
            // do {} while (0); happens 336 times in Mandreel
            return body;
        }
        JavaScriptNode nonVoidBody = body instanceof VoidNode ? ((VoidNode) body).getOperand() : body;
        return new WhileNode(new DoWhileRepeatingNode(condition, nonVoidBody));
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
        return JSTags.createNodeObjectDescriptor("type", ControlFlowRootTag.Type.Iteration.name());
    }

    @Override
    public InstrumentableNode materializeInstrumentableNodes(Set<Class<? extends Tag>> materializedTags) {
        if (hasMaterializationTag(materializedTags) && materializationNeeded()) {
            if (loop.getRepeatingNode() instanceof AbstractRepeatingNode) {
                AbstractRepeatingNode repeatingNode = (AbstractRepeatingNode) loop.getRepeatingNode();
                JavaScriptNode bodyNode = JSTaggedExecutionNode.createFor(repeatingNode.bodyNode, ControlFlowBlockTag.class);
                JavaScriptNode conditionNode = JSTaggedExecutionNode.createFor(repeatingNode.conditionNode, ControlFlowBranchTag.class,
                                JSTags.createNodeObjectDescriptor("type", ControlFlowBranchTag.Type.Condition.name()));
                transferSourceSection(this, bodyNode);
                WhileNode materialized;
                if (repeatingNode instanceof DoWhileRepeatingNode) {
                    materialized = new WhileNode(new DoWhileRepeatingNode(conditionNode, bodyNode));
                } else {
                    assert repeatingNode instanceof WhileDoRepeatingNode;
                    materialized = new WhileNode(new WhileDoRepeatingNode(conditionNode, bodyNode));
                }
                transferSourceSection(this, materialized);
                return materialized;
            }
        }
        return this;
    }

    private static boolean hasMaterializationTag(Set<Class<? extends Tag>> materializedTags) {
        return materializedTags.contains(ControlFlowRootTag.class) || materializedTags.contains(ControlFlowBlockTag.class) ||
                        materializedTags.contains(ControlFlowBranchTag.class);
    }

    private boolean materializationNeeded() {
        // If we are using tagged nodes, this node is already materialized.
        return !(((AbstractRepeatingNode) loop.getRepeatingNode()).bodyNode instanceof JSTaggedExecutionNode);
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        return new WhileNode((RepeatingNode) cloneUninitialized((JavaScriptNode) loop.getRepeatingNode()));
    }

    @Override
    public Object execute(VirtualFrame frame) {
        loop.executeLoop(frame);
        return EMPTY;
    }

    @Override
    public void executeVoid(VirtualFrame frame) {
        loop.executeLoop(frame);
    }

    @Override
    public boolean isResultAlwaysOfType(Class<?> clazz) {
        assert EMPTY == Undefined.instance;
        return clazz == Undefined.class;
    }

    /** do {body} while(condition). */
    private static final class DoWhileRepeatingNode extends AbstractRepeatingNode {

        DoWhileRepeatingNode(JavaScriptNode condition, JavaScriptNode body) {
            super(condition, body);
        }

        @Override
        public boolean executeRepeating(VirtualFrame frame) {
            executeBody(frame);
            return executeCondition(frame);

        }

        @Override
        public Object resume(VirtualFrame frame) {
            int index = getStateAsIntAndReset(frame);
            if (index == 0) {
                executeBody(frame);
            }
            try {
                return executeCondition(frame);
            } catch (YieldException e) {
                setState(frame, 1);
                throw e;
            }
        }

        @Override
        protected JavaScriptNode copyUninitialized() {
            return new DoWhileRepeatingNode(cloneUninitialized(conditionNode), cloneUninitialized(bodyNode));
        }
    }

    /** while(condition) {body}. */
    private static final class WhileDoRepeatingNode extends AbstractRepeatingNode {

        WhileDoRepeatingNode(JavaScriptNode condition, JavaScriptNode body) {
            super(condition, body);
        }

        @Override
        public boolean executeRepeating(VirtualFrame frame) {
            if (executeCondition(frame)) {
                executeBody(frame);
                return true;
            }
            return false;
        }

        @Override
        public Object resume(VirtualFrame frame) {
            int index = getStateAsIntAndReset(frame);
            if (index != 0 || executeCondition(frame)) {
                try {
                    executeBody(frame);
                } catch (YieldException e) {
                    setState(frame, 1);
                    throw e;
                }
                return true;
            }
            return false;
        }

        @Override
        protected JavaScriptNode copyUninitialized() {
            return new WhileDoRepeatingNode(cloneUninitialized(conditionNode), cloneUninitialized(bodyNode));
        }
    }
}
