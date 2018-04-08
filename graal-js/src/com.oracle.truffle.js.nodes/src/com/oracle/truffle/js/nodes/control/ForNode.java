/*
 * Copyright (c) 2012, 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.control;

import java.util.Set;

import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.InstrumentableNode;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.nodes.LoopNode;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.nodes.NodeUtil;
import com.oracle.truffle.api.nodes.RepeatingNode;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.function.IterationScopeNode;
import com.oracle.truffle.js.nodes.instrumentation.JSTaggedExecutionNode;
import com.oracle.truffle.js.nodes.instrumentation.JSTags;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.ControlFlowBlockTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.ControlFlowBranchTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.ControlFlowRootTag;
import com.oracle.truffle.js.nodes.unary.VoidNode;
import com.oracle.truffle.js.runtime.JSFrameUtil;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.Pair;

/**
 * for(;condition;modify) {body} with per-iteration scope.
 */
@NodeInfo(shortName = "for")
public final class ForNode extends StatementNode implements ResumableNode {

    @Child private LoopNode loop;
    @Child private IterationScopeNode copy;

    private ForNode(RepeatingNode repeatingNode, IterationScopeNode copy) {
        this.copy = copy;
        this.loop = Truffle.getRuntime().createLoopNode(repeatingNode);
    }

    public static ForNode createFor(JavaScriptNode condition, JavaScriptNode body, JavaScriptNode modify, IterationScopeNode copy, JavaScriptNode isFirstNode, JavaScriptNode setNotFirstNode) {
        JavaScriptNode nonVoidBody = body instanceof VoidNode ? ((VoidNode) body).getOperand() : body;
        return new ForNode(new ForRepeatingNode(condition, nonVoidBody, modify, copy, isFirstNode, setNotFirstNode), NodeUtil.cloneNode(copy));
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
    public Object execute(VirtualFrame frame) {
        executeVoid(frame);
        return EMPTY;
    }

    @Override
    public void executeVoid(VirtualFrame frame) {
        loop.executeLoop(copy.execute(frame));
    }

    @Override
    public Object resume(VirtualFrame frame) {
        Object state = getStateAndReset(frame);
        MaterializedFrame loopFrame = state == Undefined.instance ? copy.execute(frame).materialize() : JSFrameUtil.castMaterializedFrame(state);
        try {
            loop.executeLoop(loopFrame);
        } catch (YieldException e) {
            setState(frame, loopFrame);
            throw e;
        }
        return EMPTY;
    }

    @Override
    public boolean isResultAlwaysOfType(Class<?> clazz) {
        assert EMPTY == Undefined.instance;
        return clazz == Undefined.class;
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        return new ForNode((RepeatingNode) cloneUninitialized((JavaScriptNode) loop.getRepeatingNode()), cloneUninitialized(copy));
    }

    public LoopNode getLoopNode() {
        return loop;
    }

    /** for(;condition;modify) {body}. */
    private static final class ForRepeatingNode extends AbstractRepeatingNode {
        @Child private JavaScriptNode modify;
        @Child private IterationScopeNode copy;
        @Child private JavaScriptNode isFirstNode;
        @Child private JavaScriptNode setNotFirstNode;

        ForRepeatingNode(JavaScriptNode condition, JavaScriptNode body, JavaScriptNode modify, IterationScopeNode copy, JavaScriptNode isFirstNode, JavaScriptNode setNotFirstNode) {
            super(condition, body);
            this.modify = modify;
            this.copy = copy;
            this.isFirstNode = isFirstNode;
            this.setNotFirstNode = setNotFirstNode;
        }

        @Override
        public InstrumentableNode materializeInstrumentableNodes(Set<Class<? extends Tag>> materializedTags) {
            if (hasMaterializationTag(materializedTags) && materializationNeeded()) {
                JavaScriptNode newBody = JSTaggedExecutionNode.createFor(bodyNode, ControlFlowBlockTag.class);
                JavaScriptNode newCondition = JSTaggedExecutionNode.createFor(conditionNode, ControlFlowBranchTag.class,
                                JSTags.createNodeObjectDescriptor("type", ControlFlowBranchTag.Type.Condition.name()));
                JavaScriptNode newLoop = new ForRepeatingNode(newCondition, newBody, cloneUninitialized(modify), cloneUninitialized(copy), isFirstNode, cloneUninitialized(setNotFirstNode));
                transferSourceSection(this, newLoop);
                return newLoop;
            } else {
                return this;
            }
        }

        private static boolean hasMaterializationTag(Set<Class<? extends Tag>> materializedTags) {
            return materializedTags.contains(ControlFlowRootTag.class) || materializedTags.contains(ControlFlowBlockTag.class) ||
                            materializedTags.contains(ControlFlowBranchTag.class);
        }

        private boolean materializationNeeded() {
            // if body is tagged, no materialization is needed.
            return !(bodyNode instanceof JSTaggedExecutionNode);
        }

        @Override
        public boolean executeRepeating(VirtualFrame frame) {
            VirtualFrame iterationFrame = copy.execute(frame);
            if (notFirstIteration(frame)) {
                modify.executeVoid(iterationFrame);
            }
            if (executeCondition(iterationFrame)) {
                executeBody(iterationFrame);
                copy.executeCopy(frame, iterationFrame);
                return true;
            }
            return false;
        }

        private boolean notFirstIteration(VirtualFrame frame) {
            if (executeConditionAsBoolean(frame, isFirstNode)) {
                setNotFirstNode.executeVoid(frame);
                return false;
            }
            return true;
        }

        @Override
        public Object resume(VirtualFrame frame) {
            Object state = getStateAndReset(frame);
            MaterializedFrame iterationFrame;
            int index; // resume into: 0:modify, 1:condition, 2:body
            if (state == Undefined.instance) {
                iterationFrame = copy.execute(frame).materialize();
                index = 0;
            } else {
                @SuppressWarnings("unchecked")
                Pair<VirtualFrame, Integer> statePair = (Pair<VirtualFrame, Integer>) state;
                iterationFrame = JSFrameUtil.castMaterializedFrame(statePair.getFirst());
                index = statePair.getSecond();
            }
            if (index <= 0 && notFirstIteration(frame)) {
                try {
                    modify.executeVoid(iterationFrame);
                } catch (YieldException e) {
                    setState(frame, new Pair<>(iterationFrame, 0));
                    throw e;
                }
            }
            boolean condition = true;
            if (index <= 1) {
                try {
                    /*
                     * Cannot profile here: branch probability injection would fail due to the
                     * following control flow merge with the else branch where we do not execute;
                     * i.e., condition in the if below actually becomes phi(condition, true).
                     */
                    condition = executeConditionNoProfile(iterationFrame);
                } catch (YieldException e) {
                    setState(frame, new Pair<>(iterationFrame, 1));
                    throw e;
                }
            }
            if (condition) {
                try {
                    executeBody(iterationFrame);
                } catch (YieldException e) {
                    setState(frame, new Pair<>(iterationFrame, 2));
                    throw e;
                }
                copy.executeCopy(frame, iterationFrame);
                return true;
            }
            return false;
        }

        @Override
        protected JavaScriptNode copyUninitialized() {
            return new ForRepeatingNode(cloneUninitialized(conditionNode), cloneUninitialized(bodyNode), cloneUninitialized(modify), cloneUninitialized(copy), cloneUninitialized(isFirstNode),
                            cloneUninitialized(setNotFirstNode));
        }
    }
}
