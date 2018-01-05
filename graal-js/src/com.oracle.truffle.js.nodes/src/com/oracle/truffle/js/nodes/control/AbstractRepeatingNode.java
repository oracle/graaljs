/*
 * Copyright (c) 2012, 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.control;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.RepeatingNode;
import com.oracle.truffle.api.profiles.LoopConditionProfile;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.cast.JSToBooleanNode;
import com.oracle.truffle.js.runtime.JSCancelledExecutionException;

abstract class AbstractRepeatingNode extends JavaScriptNode implements RepeatingNode, ResumableNode {

    @Child protected JavaScriptNode conditionNode;
    @Child protected JavaScriptNode bodyNode;
    private final LoopConditionProfile conditionProfile = LoopConditionProfile.createCountingProfile();

    AbstractRepeatingNode(JavaScriptNode condition, JavaScriptNode body) {
        this.conditionNode = JSToBooleanNode.create(condition);
        this.bodyNode = body;
    }

    protected final boolean executeCondition(VirtualFrame frame) {
        return conditionProfile.profile(WhileNode.executeConditionAsBoolean(frame, conditionNode));
    }

    protected final void executeBody(VirtualFrame frame) {
        bodyNode.executeVoid(frame);
        if (CompilerDirectives.inInterpreter()) {
            checkThreadInterrupted();
        }
    }

    private void checkThreadInterrupted() {
        CompilerAsserts.neverPartOfCompilation("do not check thread interruption from compiled code");
        if (Thread.currentThread().isInterrupted()) {
            throw new JSCancelledExecutionException("Thread was interrupted.", this);
        }
    }

    @Override
    public Object execute(VirtualFrame frame) {
        return executeRepeating(frame);
    }
}
