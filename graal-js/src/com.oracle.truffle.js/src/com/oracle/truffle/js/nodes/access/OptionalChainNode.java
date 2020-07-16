/*
 * Copyright (c) 2020, 2020, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.nodes.access;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.nodes.ControlFlowException;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.control.DeletePropertyNode;
import com.oracle.truffle.js.nodes.unary.JSIsNullOrUndefinedNode;
import com.oracle.truffle.js.runtime.objects.Undefined;

import java.util.Set;

/**
 * Wrapper node for an optional chain that returns the result (usually undefined) on short circuit.
 */
public final class OptionalChainNode extends JavaScriptNode {

    @Child private JavaScriptNode accessNode;
    private final Object result;

    protected OptionalChainNode(JavaScriptNode chainNode, Object result) {
        this.accessNode = chainNode;
        this.result = result;
    }

    public static JavaScriptNode createTarget(JavaScriptNode chainNode) {
        Object result = (chainNode instanceof DeletePropertyNode) ? Boolean.TRUE : Undefined.instance;
        if (chainNode instanceof JSTargetableNode) {
            return new OptionalTargetableNode((JSTargetableNode) chainNode, result);
        } else {
            return new OptionalChainNode(chainNode, result);
        }
    }

    public static JavaScriptNode createShortCircuit(JavaScriptNode expressionNode) {
        if (expressionNode instanceof JSTargetableNode) {
            return new ShortCircuitTargetableNode((JSTargetableNode) expressionNode);
        }
        return new ShortCircuitNode(expressionNode);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        try {
            return accessNode.execute(frame);
        } catch (ShortCircuitException ex) {
            return result;
        }
    }

    @Override
    public void executeVoid(VirtualFrame frame) {
        try {
            accessNode.executeVoid(frame);
        } catch (ShortCircuitException ex) {
        }
    }

    @Override
    public int executeInt(VirtualFrame frame) throws UnexpectedResultException {
        try {
            return accessNode.executeInt(frame);
        } catch (ShortCircuitException ex) {
            throw new UnexpectedResultException(result);
        }
    }

    @Override
    public double executeDouble(VirtualFrame frame) throws UnexpectedResultException {
        try {
            return accessNode.executeDouble(frame);
        } catch (ShortCircuitException ex) {
            throw new UnexpectedResultException(result);
        }
    }

    @Override
    protected JavaScriptNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
        return new OptionalChainNode(cloneUninitialized(accessNode, materializedTags), result);
    }

    public JavaScriptNode getAccessNode() {
        return accessNode;
    }

    public static final class OptionalTargetableNode extends JSTargetableNode {
        private static final Object SHORT_CIRCUIT_MARKER = new Object();
        @Child private JSTargetableNode delegateNode;
        private final Object result;

        protected OptionalTargetableNode(JSTargetableNode delegateNode, Object result) {
            this.delegateNode = delegateNode;
            this.result = result;
        }

        @Override
        public JavaScriptNode getTarget() {
            return delegateNode.getTarget();
        }

        @Override
        public Object evaluateTarget(VirtualFrame frame) {
            try {
                return delegateNode.evaluateTarget(frame);
            } catch (ShortCircuitException ex) {
                return SHORT_CIRCUIT_MARKER;
            }
        }

        @Override
        public Object executeWithTarget(VirtualFrame frame, Object target) {
            if (target == SHORT_CIRCUIT_MARKER) {
                return result;
            }
            return delegateNode.executeWithTarget(frame, target);
        }

        @Override
        public Object execute(VirtualFrame frame) {
            try {
                return delegateNode.execute(frame);
            } catch (ShortCircuitException ex) {
                return result;
            }
        }

        @Override
        protected JavaScriptNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
            return new OptionalTargetableNode(cloneUninitialized(delegateNode, materializedTags), result);
        }

        public JavaScriptNode getDelegateNode() {
            return delegateNode;
        }

    }

    /**
     * Evaluates an optional expression and if its value is null or undefined, jumps out of the
     * short-circuiting expression to the parent {@link OptionalChainNode}.
     */
    public static final class ShortCircuitNode extends JavaScriptNode {

        @Child private JavaScriptNode expressionNode;

        @Child JSIsNullOrUndefinedNode isNullOrUndefinedNode = JSIsNullOrUndefinedNode.create();
        private final ConditionProfile isNullish = ConditionProfile.createCountingProfile();

        ShortCircuitNode(JavaScriptNode expressionNode) {
            this.expressionNode = expressionNode;
        }

        private boolean isNullish(Object targetValue) {
            return isNullish.profile(isNullOrUndefinedNode.executeBoolean(targetValue));
        }

        @Override
        public Object execute(VirtualFrame frame) {
            Object result = expressionNode.execute(frame);
            if (isNullish(result)) {
                throw ShortCircuitException.instance();
            }
            return result;
        }

        @Override
        public void executeVoid(VirtualFrame frame) {
            Object result = expressionNode.execute(frame);
            if (isNullish(result)) {
                throw ShortCircuitException.instance();
            }
        }

        @Override
        protected JavaScriptNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
            return new ShortCircuitNode(cloneUninitialized(expressionNode, materializedTags));
        }
    }

    /**
     * Evaluates an optional expression and if its value is null or undefined, jumps out of the
     * short-circuiting expression to the parent {@link OptionalChainNode}.
     */
    public static final class ShortCircuitTargetableNode extends JSTargetableNode {

        @Child private JSTargetableNode expressionNode;

        @Child JSIsNullOrUndefinedNode isNullOrUndefinedNode = JSIsNullOrUndefinedNode.create();
        private final ConditionProfile isNullish = ConditionProfile.createCountingProfile();

        ShortCircuitTargetableNode(JSTargetableNode expressionNode) {
            this.expressionNode = expressionNode;
        }

        private boolean isNullish(Object targetValue) {
            return isNullish.profile(isNullOrUndefinedNode.executeBoolean(targetValue));
        }

        @Override
        public JavaScriptNode getTarget() {
            return expressionNode.getTarget();
        }

        @Override
        public Object execute(VirtualFrame frame) {
            Object result = expressionNode.execute(frame);
            if (isNullish(result)) {
                throw ShortCircuitException.instance();
            }
            return result;
        }

        @Override
        public void executeVoid(VirtualFrame frame) {
            Object result = expressionNode.execute(frame);
            if (isNullish(result)) {
                throw ShortCircuitException.instance();
            }
        }

        @Override
        public Object executeWithTarget(VirtualFrame frame, Object target) {
            Object result = expressionNode.executeWithTarget(frame, target);
            if (isNullish(result)) {
                throw ShortCircuitException.instance();
            }
            return result;
        }

        @Override
        protected JavaScriptNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
            return new ShortCircuitTargetableNode(cloneUninitialized(expressionNode, materializedTags));
        }
    }

    private static final class ShortCircuitException extends ControlFlowException {
        private static final long serialVersionUID = 7544707953047299660L;
        private static final ShortCircuitException INSTANCE = new ShortCircuitException();

        private ShortCircuitException() {
        }

        static ShortCircuitException instance() {
            return INSTANCE;
        }
    }
}
