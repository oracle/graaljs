/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.control;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.runtime.objects.Undefined;

/**
 * Common base class for statements. Statements don't produce a result.
 */
public abstract class StatementNode extends JavaScriptNode {

    public static final Object EMPTY = Undefined.instance;

    @Override
    public boolean hasTag(Class<? extends Tag> tag) {
        if (tag == StandardTags.ExpressionTag.class) {
            return false;
        } else {
            return super.hasTag(tag);
        }
    }

    protected static boolean executeConditionAsBoolean(VirtualFrame frame, JavaScriptNode conditionNode) {
        try {
            return conditionNode.executeBoolean(frame);
        } catch (UnexpectedResultException ex) {
            throw new AssertionError("the condition should always provide a boolean result");
        }
    }
}
