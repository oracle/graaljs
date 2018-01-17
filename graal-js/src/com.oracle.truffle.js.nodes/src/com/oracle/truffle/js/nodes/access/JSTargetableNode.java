/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.access;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.GenerateWrapper;
import com.oracle.truffle.api.instrumentation.ProbeNode;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.runtime.Errors;

@GenerateWrapper
public abstract class JSTargetableNode extends JavaScriptNode {

    public abstract Object executeWithTarget(VirtualFrame frame, Object target);

    /* Used evaluateTarget instead of executeTarget to avoid confusion of the DSL. */
    public Object evaluateTarget(VirtualFrame frame) {
        return getTarget().execute(frame);
    }

    public int executeIntWithTarget(VirtualFrame frame, Object target) throws UnexpectedResultException {
        Object o = executeWithTarget(frame, target);
        if (o instanceof Integer) {
            return (int) o;
        } else {
            throw new UnexpectedResultException(o);
        }
    }

    public double executeDoubleWithTarget(VirtualFrame frame, Object target) throws UnexpectedResultException {
        Object o = executeWithTarget(frame, target);
        if (o instanceof Double) {
            return (double) o;
        } else if (o instanceof Integer) {
            return (int) o;
        } else {
            throw new UnexpectedResultException(o);
        }
    }

    public JavaScriptNode getTarget() {
        if (this instanceof WrapperNode) {
            return ((JSTargetableNode) ((WrapperNode) this).getDelegateNode()).getTarget();
        }
        throw Errors.notYetImplemented();
    }

    @Override
    public WrapperNode createWrapper(ProbeNode probe) {
        return new JSTargetableNodeWrapper(this, probe);
    }
}
