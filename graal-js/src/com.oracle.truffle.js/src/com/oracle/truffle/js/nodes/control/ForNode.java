/*
 * Copyright (c) 2018, 2025, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.js.nodes.instrumentation.JSTags;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.ControlFlowBlockTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.ControlFlowBranchTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.ControlFlowRootTag;
import com.oracle.truffle.js.runtime.JSFrameUtil;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.Pair;

/**
 * for(;condition;modify) {body} with per-iteration scope.
 */
@NodeInfo(shortName = "for")
public final class ForNode extends StatementNode implements ResumableNode.WithObjectState {

    @Child private LoopNode loop;
    @Child private IterationScopeNode copy;

    private ForNode(RepeatingNode repeatingNode, IterationScopeNode copy) {
        this(Truffle.getRuntime().createLoopNode(repeatingNode), copy);
    }

    private ForNode(LoopNode loopNode, IterationScopeNode copy) {
        this.copy = copy;
        this.loop = loopNode;
    }

    public static RepeatingNode createForRepeatingNode(JavaScriptNode condition, JavaScriptNode body, JavaScriptNode modify, IterationScopeNode copy, JavaScriptNode isFirstNode,
                    JavaScriptNode setNotFirstNode) {
        JavaScriptNode nonVoidBody = body instanceof DiscardResultNode ? ((DiscardResultNode) body).getOperand() : body;
        return new ForRepeatingNode(condition, nonVoidBody, modify, copy, isFirstNode, setNotFirstNode);
    }

    public static ForNode createFor(LoopNode loopNode) {
        ForRepeatingNode forRepeatingNode = (ForRepeatingNode) loopNode.getRepeatingNode();
        return new ForNode(loopNode, NodeUtil.cloneNode(forRepeatingNode.copy));
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
        return JSTags.createNodeObjectDescriptor("type", ControlFlowRootTag.Type.ForIteration.name());
    }

    @Override
    public InstrumentableNode materializeInstrumentableNodes(Set<Class<? extends Tag>> materializedTags) {
        if (hasMaterializationTag(materializedTags) &&
                        loop.getRepeatingNode() instanceof AbstractRepeatingNode repeatingNode &&
                        repeatingNode.materializationNeeded()) {
            IterationScopeNode newCopy = cloneUninitialized(copy, materializedTags);
            /*
             * The repeating node might not be instrumentable at this point, because source section
             * is transferred later, so we need to force the materialization of repeating node.
             */
            AbstractRepeatingNode materializedLoop = repeatingNode.materializeInstrumentableNodes(materializedTags);
            transferSourceSection(this, materializedLoop.bodyNode);
            ForNode materializedNode = new ForNode(materializedLoop, newCopy);
            transferSourceSectionAndTags(this, materializedNode);
            return materializedNode;
        } else {
            return this;
        }
    }

    private static boolean hasMaterializationTag(Set<Class<? extends Tag>> materializedTags) {
        return materializedTags.contains(ControlFlowRootTag.class) || materializedTags.contains(ControlFlowBlockTag.class) ||
                        materializedTags.contains(ControlFlowBranchTag.class);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        executeVoid(frame);
        return EMPTY;
    }

    @Override
    public void executeVoid(VirtualFrame frame) {
        VirtualFrame prevFrame = copy.execute(frame);
        try {
            loop.execute(frame);
        } finally {
            copy.exitScope(frame, prevFrame);
        }
    }

    @Override
    public Object resume(VirtualFrame frame, int stateSlot) {
        Object state = getStateAndReset(frame, stateSlot);
        VirtualFrame prevFrame;
        if (state == Undefined.instance) {
            prevFrame = copy.execute(frame);
        } else {
            prevFrame = JSFrameUtil.castMaterializedFrame(state);
        }
        boolean yielded = false;
        try {
            loop.execute(frame);
        } catch (YieldException e) {
            yielded = true;
            setState(frame, stateSlot, prevFrame);
            throw e;
        } finally {
            if (!yielded) {
                copy.exitScope(frame, prevFrame);
            }
        }
        return EMPTY;
    }

    @Override
    public boolean isResultAlwaysOfType(Class<?> clazz) {
        assert EMPTY == Undefined.instance;
        return clazz == Undefined.class;
    }

    @Override
    protected JavaScriptNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
        return new ForNode((RepeatingNode) cloneUninitialized((JavaScriptNode) loop.getRepeatingNode(), materializedTags), cloneUninitialized(copy, materializedTags));
    }

    public LoopNode getLoopNode() {
        return loop;
    }

    /** for(;condition;modify) {body}. */
    private static final class ForRepeatingNode extends AbstractRepeatingNode implements ResumableNode.WithObjectState {
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
            VirtualFrame prevFrame = copy.execute(frame);
            if (notFirstIteration(frame)) {
                modify.executeVoid(frame);
            }
            if (executeCondition(frame)) {
                executeBody(frame);
                copy.executeCopy(frame, prevFrame);
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
        public Object resume(VirtualFrame frame, int stateSlot) {
            Object state = getStateAndReset(frame, stateSlot);
            MaterializedFrame prevFrame;
            int index; // resume into: 0:modify, 1:condition, 2:body
            if (state == Undefined.instance) {
                prevFrame = copy.execute(frame).materialize();
                index = 0;
            } else {
                @SuppressWarnings("unchecked")
                Pair<VirtualFrame, Integer> statePair = (Pair<VirtualFrame, Integer>) state;
                prevFrame = JSFrameUtil.castMaterializedFrame(statePair.getFirst());
                index = statePair.getSecond();
            }
            if (index <= 0 && notFirstIteration(frame)) {
                try {
                    modify.executeVoid(frame);
                } catch (YieldException e) {
                    setState(frame, stateSlot, new Pair<>(prevFrame, 0));
                    throw e;
                }
            }
            boolean condition = true;
            if (index <= 1) {
                try {
                    condition = executeCondition(frame);
                } catch (YieldException e) {
                    setState(frame, stateSlot, new Pair<>(prevFrame, 1));
                    throw e;
                }
            }
            if (condition) {
                try {
                    executeBody(frame);
                } catch (YieldException e) {
                    setState(frame, stateSlot, new Pair<>(prevFrame, 2));
                    throw e;
                }
                copy.executeCopy(frame, prevFrame);
                return true;
            }
            return false;
        }

        @Override
        protected JavaScriptNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
            return new ForRepeatingNode(cloneUninitialized(conditionNode, materializedTags), cloneUninitialized(bodyNode, materializedTags), cloneUninitialized(modify, materializedTags),
                            cloneUninitialized(copy, materializedTags), cloneUninitialized(isFirstNode, materializedTags),
                            cloneUninitialized(setNotFirstNode, materializedTags));
        }

        @Override
        public AbstractRepeatingNode materializeInstrumentableNodes(Set<Class<? extends Tag>> materializedTags) {
            if (!materializationNeeded()) {
                return this;
            }
            return new ForRepeatingNode(materializeCondition(materializedTags), materializeBody(materializedTags), cloneUninitialized(modify, materializedTags),
                            cloneUninitialized(copy, materializedTags), cloneUninitialized(isFirstNode, materializedTags), cloneUninitialized(setNotFirstNode, materializedTags));
        }
    }
}
