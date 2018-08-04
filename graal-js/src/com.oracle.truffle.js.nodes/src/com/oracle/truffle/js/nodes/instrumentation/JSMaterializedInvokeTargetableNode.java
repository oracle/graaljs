/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.nodes.instrumentation;

import java.util.Set;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.InstrumentableNode;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.access.GlobalConstantNode;
import com.oracle.truffle.js.nodes.access.JSTargetableNode;
import com.oracle.truffle.js.nodes.access.PropertyNode;
import com.oracle.truffle.js.nodes.access.ReadElementNode;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.InputNodeTag;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;

/**
 * Materialized nodes used by <code>MaterializedInvokeNode</code> to report JS function values to
 * the instrumentation framework.
 *
 * By default, <code>InvokeNode</code> evaluates its target node in two separate steps: first, the
 * node is evaluated to retrieve the <code>target</code> value. Then, the same node is evaluated via
 * <code>executeWithTarget()</code> to retrieve the JS function instance to be called. The second
 * evaluation via <code>executeWithTarget()</code> causes a read event (e.g., property read) that
 * will be detected by the instrumentation framework. Since the target is already evaluated, such
 * read event will however miss its <code>target</code> value, resulting in a wrong series of
 * instrumentation events.
 *
 * The materialized nodes in this class internally re-evaluate the <code>target</code> value
 * provided via <code>executeWithTarget()</code> using the internal <code>EchoTargetValueNode</code>
 * . In this way, the instrumentation framework is able to trace <code>target</code> and report it
 * correctly as an <code>onInput</code> value.
 *
 */
public abstract class JSMaterializedInvokeTargetableNode extends JSTargetableNode {

    public interface MaterializedTargetableNode {

        Object getPropertyKey();
    }

    public static JSTargetableNode createFor(JSTargetableNode target) {
        if (target instanceof PropertyNode) {
            return new MaterializedTargetablePropertyNode((PropertyNode) target);
        } else if (target instanceof ReadElementNode) {
            return new MaterializedTargetableReadElementNode((ReadElementNode) target);
        } else if (target instanceof GlobalConstantNode) {
            return target;
        } else {
            // Unknown targetable node: we might need to implement a new materialized node for it.
            throw Errors.shouldNotReachHere("Unsupported materialization node type: " + target.getClass());
        }
    }

    /**
     * Materialized version of <code>ReadElementNode</code> to be used as a target node by
     * <code>MaterializedInvokeNode</code>.
     *
     */
    private static class MaterializedTargetableReadElementNode extends ReadElementNode implements MaterializedTargetableNode {

        // node declaration order is used by the instrumentation framework to derive the index order
        // for each instrumented data event. We must re-declare nodes and keep null ref in parent
        // class to expose instrumentation events with the correct order.
        @Child private JSTargetableNode echo;
        @Child private JavaScriptNode index;

        protected MaterializedTargetableReadElementNode(JavaScriptNode targetNode, JavaScriptNode indexNode, JSContext context) {
            super(targetNode, indexNode, context);
        }

        protected MaterializedTargetableReadElementNode(JavaScriptNode index, JSTargetableNode echo, JSContext context) {
            super(null, null, context);
            this.index = index;
            this.echo = echo;
        }

        MaterializedTargetableReadElementNode(ReadElementNode from) {
            this(null, null, from.getContext());
            this.index = from.getElement();
            this.echo = new EchoTargetValueNode();
        }

        @Override
        public Object executeWithTarget(VirtualFrame frame, Object targetValue) {
            echo.executeWithTarget(frame, targetValue);
            return super.executeWithTarget(frame, targetValue);
        }

        @Override
        public Object execute(VirtualFrame frame) {
            throw Errors.shouldNotReachHere("Must use executeWithTarget()");
        }

        @Override
        public JavaScriptNode getIndexNode() {
            return index;
        }

        @Override
        public Object getPropertyKey() {
            return null;
        }

        @Override
        public boolean isInstrumentable() {
            return true;
        }

        @Override
        public boolean hasTag(Class<? extends Tag> tag) {
            if (tag == InputNodeTag.class) {
                return true;
            }
            return super.hasTag(tag);
        }

        @Override
        public InstrumentableNode materializeInstrumentableNodes(Set<Class<? extends Tag>> materializedTags) {
            return this;
        }

        @Override
        protected JavaScriptNode copyUninitialized() {
            return new MaterializedTargetableReadElementNode(cloneUninitialized(index), cloneUninitialized(echo), context);
        }
    }

    /**
     * Materialized version of <code>PropertyNode</code> to be used as a target node by
     * <code>MaterializedInvokeNode</code>.
     *
     */
    private static class MaterializedTargetablePropertyNode extends PropertyNode implements MaterializedTargetableNode {

        @Child private JSTargetableNode echo;

        protected MaterializedTargetablePropertyNode(JSContext context, Object propertyKey) {
            super(context, null, propertyKey);
        }

        protected MaterializedTargetablePropertyNode(JSContext context, JSTargetableNode echo, Object propertyKey) {
            this(context, propertyKey);
            this.echo = echo;
        }

        MaterializedTargetablePropertyNode(PropertyNode target) {
            this(target.getContext(), target.getPropertyKey());
            this.echo = new EchoTargetValueNode();
            this.setMethod();
        }

        @Override
        public Object executeWithTarget(VirtualFrame frame, Object targetValue) {
            echo.executeWithTarget(frame, targetValue);
            return super.executeWithTarget(frame, targetValue);
        }

        @Override
        public Object execute(VirtualFrame frame) {
            throw Errors.shouldNotReachHere("Must use executeWithTarget()");
        }

        @Override
        public boolean isInstrumentable() {
            return true;
        }

        @Override
        public boolean hasTag(Class<? extends Tag> tag) {
            if (tag == InputNodeTag.class) {
                return true;
            }
            return super.hasTag(tag);
        }

        @Override
        public InstrumentableNode materializeInstrumentableNodes(Set<Class<? extends Tag>> materializedTags) {
            return this;
        }

        @Override
        protected JavaScriptNode copyUninitialized() {
            return new MaterializedTargetablePropertyNode(getContext(), cloneUninitialized(echo), getPropertyKey());
        }
    }

    /**
     * Instrumentable node reporting to the instrumentation framework any value provided to
     * <code>executeWithTarget()</code>.
     *
     */
    private static class EchoTargetValueNode extends JSTargetableNode {

        @Override
        public Object executeWithTarget(VirtualFrame frame, Object target) {
            return target;
        }

        @Override
        public Object execute(VirtualFrame frame) {
            throw Errors.shouldNotReachHere("Must use executeWithTarget()");
        }

        @Override
        public boolean isInstrumentable() {
            return true;
        }

        @Override
        public boolean hasTag(Class<? extends Tag> tag) {
            if (tag == JSTags.InputNodeTag.class) {
                return true;
            }
            return false;
        }

        @Override
        protected JavaScriptNode copyUninitialized() {
            return new EchoTargetValueNode();
        }
    }

}
