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

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.Executed;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.InstrumentableNode;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.profiles.InlinedCountingConditionProfile;
import com.oracle.truffle.js.nodes.JSNodeUtil;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.cast.JSToBooleanNode;
import com.oracle.truffle.js.nodes.instrumentation.JSTaggedExecutionNode;
import com.oracle.truffle.js.nodes.instrumentation.JSTags;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.ControlFlowBlockTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.ControlFlowBranchTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.ControlFlowRootTag;
import com.oracle.truffle.js.nodes.unary.JSNotNode;
import com.oracle.truffle.js.runtime.objects.Undefined;

/**
 * 12.5 The if Statement.
 */

@NodeInfo(shortName = "if")
abstract class AbstractIfNode extends StatementNode implements ResumableNode.WithIntState {

    public abstract JavaScriptNode getThenPart();

    public abstract JavaScriptNode getElsePart();

    public abstract JavaScriptNode getCondition();

    protected abstract AbstractIfNode copyWith(JavaScriptNode newCondition, JavaScriptNode newThenPart, JavaScriptNode newElsePart);

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
            JavaScriptNode newCondition = JSTaggedExecutionNode.createForInput(getCondition(), ControlFlowBranchTag.class,
                            JSTags.createNodeObjectDescriptor("type", ControlFlowBranchTag.Type.Condition.name()), materializedTags);
            JavaScriptNode newThenPart = getThenPart() != null ? JSTaggedExecutionNode.createForInput(getThenPart(), JSTags.ControlFlowBlockTag.class, materializedTags) : null;

            JavaScriptNode newElsePart = getElsePart() != null ? JSTaggedExecutionNode.createForInput(getElsePart(), JSTags.ControlFlowBlockTag.class, materializedTags) : null;
            if (newCondition == getCondition() && newThenPart == getThenPart() && newElsePart == getElsePart()) {
                return this;
            }
            if (newCondition == getCondition()) {
                newCondition = cloneUninitialized(getCondition(), materializedTags);
            }
            if (newThenPart == getThenPart()) {
                newThenPart = cloneUninitialized(getThenPart(), materializedTags);
            }
            if (newElsePart == getElsePart()) {
                newElsePart = cloneUninitialized(getElsePart(), materializedTags);
            }
            JavaScriptNode newIf = copyWith(newCondition, newThenPart, newElsePart);
            transferSourceSectionAndTags(this, newIf);
            return newIf;
        } else {
            return this;
        }
    }

    private boolean materializationNeeded() {
        // If we are using tagged nodes, this node is already materialized.
        return !(JSNodeUtil.isTaggedNode(getCondition()) && (getElsePart() == null || JSNodeUtil.isTaggedNode(getElsePart())) && (getThenPart() == null || JSNodeUtil.isTaggedNode(getThenPart())));
    }

    private static boolean hasMaterializationTag(Set<Class<? extends Tag>> materializedTags) {
        return materializedTags.contains(ControlFlowRootTag.class) || materializedTags.contains(ControlFlowBranchTag.class) ||
                        materializedTags.contains(ControlFlowBlockTag.class);
    }

    @Override
    public boolean isResultAlwaysOfType(Class<?> clazz) {
        return isResultAlwaysOfType(getThenPart(), clazz) && isResultAlwaysOfType(getElsePart(), clazz);
    }

    private static boolean isResultAlwaysOfType(JavaScriptNode child, Class<?> clazz) {
        if (child == null) {
            return clazz == Undefined.class;
        } else {
            return child.isResultAlwaysOfType(clazz);
        }
    }

    @Override
    protected JavaScriptNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
        return copyWith(cloneUninitialized(getCondition(), materializedTags), cloneUninitialized(getThenPart(), materializedTags), cloneUninitialized(getElsePart(), materializedTags));
    }
}

public abstract class IfNode extends AbstractIfNode {

    @Child @Executed protected JavaScriptNode condition;
    @Child protected JavaScriptNode thenPart;
    @Child protected JavaScriptNode elsePart;

    public static IfNode create(JavaScriptNode condition, JavaScriptNode thenPart, JavaScriptNode elsePart) {
        if (condition instanceof JSNotNode) {
            // if (!a) {b()} => if (a) {} else {b(); }
            JavaScriptNode operand = ((JSNotNode) condition).getOperand();
            transferSourceSectionAddExpressionTag(condition, operand);
            return IfNodeGen.create(operand, elsePart, thenPart);
        }
        return IfNodeGen.create(condition, thenPart, elsePart);
    }

    protected IfNode(JavaScriptNode condition, JavaScriptNode thenPart, JavaScriptNode elsePart) {
        this.condition = condition;
        this.thenPart = thenPart;
        this.elsePart = elsePart;
    }

    @Specialization
    protected Object doBoolean(VirtualFrame frame, boolean conditionResult,
                    @Shared @Cached InlinedCountingConditionProfile conditionProfile) {
        if (conditionProfile.profile(this, conditionResult)) {
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

    @Specialization(replaces = "doBoolean")
    protected Object doObject(VirtualFrame frame, Object conditionResult,
                    @Shared @Cached(inline = true) JSToBooleanNode toBooleanNode,
                    @Shared @Cached InlinedCountingConditionProfile conditionProfile) {
        boolean booleanResult = toBooleanNode.executeBoolean(this, conditionResult);
        return doBoolean(frame, booleanResult, conditionProfile);
    }

    @Override
    public JavaScriptNode getCondition() {
        return condition;
    }

    @Override
    public JavaScriptNode getThenPart() {
        return thenPart;
    }

    @Override
    public JavaScriptNode getElsePart() {
        return elsePart;
    }

    @Override
    protected AbstractIfNode copyWith(JavaScriptNode newCondition, JavaScriptNode newThenPart, JavaScriptNode newElsePart) {
        return IfNode.create(newCondition, newThenPart, newElsePart);
    }

    @Override
    public JavaScriptNode asResumableNode(int stateSlot) {
        return GeneratorIfNodeGen.create(condition, thenPart, elsePart, stateSlot);
    }
}

abstract class GeneratorIfNode extends AbstractIfNode implements GeneratorNode {

    private final int stateSlot;
    @Child protected JavaScriptNode condition;
    @Child protected JavaScriptNode thenPart;
    @Child protected JavaScriptNode elsePart;

    protected GeneratorIfNode(JavaScriptNode condition, JavaScriptNode thenPart, JavaScriptNode elsePart, int stateSlot) {
        this.stateSlot = stateSlot;
        this.condition = condition;
        this.thenPart = thenPart;
        this.elsePart = elsePart;
    }

    @Specialization
    protected Object doDefault(VirtualFrame frame,
                    @Cached(inline = true) JSToBooleanNode toBooleanNode,
                    @Cached InlinedCountingConditionProfile conditionProfile) {
        int index = getStateAsIntAndReset(frame, stateSlot);
        if (index == 0 && conditionProfile.profile(this, toBooleanNode.executeBoolean(this, condition.execute(frame))) || index == 1) {
            try {
                if (thenPart != null) {
                    return thenPart.execute(frame);
                } else {
                    return EMPTY;
                }
            } catch (YieldException e) {
                setStateAsInt(frame, stateSlot, 1);
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
                setStateAsInt(frame, stateSlot, 2);
                throw e;
            }
        }
    }

    @Override
    public JavaScriptNode getCondition() {
        return condition;
    }

    @Override
    public JavaScriptNode getThenPart() {
        return thenPart;
    }

    @Override
    public JavaScriptNode getElsePart() {
        return elsePart;
    }

    @Override
    protected AbstractIfNode copyWith(JavaScriptNode newCondition, JavaScriptNode newThenPart, JavaScriptNode newElsePart) {
        return GeneratorIfNodeGen.create(newCondition, newThenPart, newElsePart, stateSlot);
    }
}
