/*
 * Copyright (c) 2012, 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.control;

import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.LoopNode;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.nodes.NodeUtil;
import com.oracle.truffle.api.nodes.RepeatingNode;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.function.IterationScopeNode;
import com.oracle.truffle.js.nodes.unary.VoidNode;
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
        VirtualFrame saveFrame = state == Undefined.instance ? copy.execute(frame) : (VirtualFrame) state;
        try {
            loop.executeLoop(saveFrame);
        } catch (YieldException e) {
            setState(frame, saveFrame);
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
            VirtualFrame iterationFrame;
            int index; // resume into: 0:modify, 1:condition, 2:body
            if (state == Undefined.instance) {
                iterationFrame = copy.execute(frame);
                index = 0;
            } else {
                @SuppressWarnings("unchecked")
                Pair<VirtualFrame, Integer> statePair = (Pair<VirtualFrame, Integer>) state;
                iterationFrame = statePair.getFirst();
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
                    condition = executeCondition(iterationFrame);
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
