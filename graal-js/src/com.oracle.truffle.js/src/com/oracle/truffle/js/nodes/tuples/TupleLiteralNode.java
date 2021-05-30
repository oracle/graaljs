/*
 * Copyright (c) 2021, 2021, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.nodes.tuples;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.access.GetIteratorNode;
import com.oracle.truffle.js.nodes.access.IteratorGetNextValueNode;
import com.oracle.truffle.js.nodes.access.JSConstantNode;
import com.oracle.truffle.js.nodes.instrumentation.JSTags;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.LiteralTag;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Tuple;
import com.oracle.truffle.js.runtime.objects.IteratorRecord;
import com.oracle.truffle.js.runtime.util.SimpleArrayList;

import java.util.Arrays;
import java.util.Set;

public abstract class TupleLiteralNode extends JavaScriptNode {

    protected final JSContext context;

    protected TupleLiteralNode(JSContext context) {
        this.context = context;
    }

    @Override
    public abstract Tuple execute(VirtualFrame frame);

    public static JavaScriptNode create(JSContext context, JavaScriptNode[] elements) {
        if (elements == null || elements.length == 0) {
            return createEmptyTuple();
        }

        Object[] constantValues = resolveConstants(elements);
        if (constantValues != null) {
            return createConstantTuple(constantValues);
        }

        return new DefaultTupleLiteralNode(context, elements);
    }

    public static TupleLiteralNode createWithSpread(JSContext context, JavaScriptNode[] elements) {
        return new DefaultTupleLiteralWithSpreadNode(context, elements);
    }

    private static JSConstantNode createEmptyTuple() {
        return JSConstantNode.create(Tuple.create());
    }

    private static JSConstantNode createConstantTuple(Object[] array) {
        Tuple tuple = Tuple.create(array);
        return JSConstantNode.create(tuple);
    }

    private static Object[] resolveConstants(JavaScriptNode[] nodes) {
        Object[] values = new Object[nodes.length];
        for (int i = 0; i < values.length; i++) {
            JavaScriptNode node = nodes[i];
            if (node instanceof JSConstantNode) {
                Object value = ((JSConstantNode) node).getValue();
                values[i] = requireNonObject(value);
            } else {
                return null;
            }
        }
        return values;
    }

    /**
     * Abstract operation AddValueToTupleSequenceList ( sequence, value )
     */
    static void addValueToTupleSequenceList(SimpleArrayList<Object> sequence, Object value, BranchProfile growProfile) {
        sequence.add(requireNonObject(value), growProfile);
    }

    private static Object requireNonObject(Object value) {
        if (JSRuntime.isObject(value)) {
            throw Errors.createTypeError("Tuples cannot contain non-primitive values");
        }
        return value;
    }

    private static Tuple createTuple(Object[] array) {
        assert Arrays.stream(array).noneMatch(JSRuntime::isObject);
        return Tuple.create(array);
    }

    private static class DefaultTupleLiteralNode extends TupleLiteralNode {

        @Children protected final JavaScriptNode[] elements;

        private DefaultTupleLiteralNode(JSContext context, JavaScriptNode[] elements) {
            super(context);
            this.elements = elements;
        }

        @Override
        public Tuple execute(VirtualFrame frame) {
            Object[] values = new Object[elements.length];
            for (int i = 0; i < elements.length; i++) {
                Object value = elements[i].execute(frame);
                values[i] = requireNonObject(value);
            }
            return createTuple(values);
        }

        @Override
        protected JavaScriptNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
            return new DefaultTupleLiteralNode(context, cloneUninitialized(elements, materializedTags));
        }
    }

    private static final class DefaultTupleLiteralWithSpreadNode extends DefaultTupleLiteralNode {

        private final BranchProfile growProfile = BranchProfile.create();

        private DefaultTupleLiteralWithSpreadNode(JSContext context, JavaScriptNode[] elements) {
            super(context, elements);
        }

        @Override
        public Tuple execute(VirtualFrame frame) {
            SimpleArrayList<Object> sequence = new SimpleArrayList<>(elements.length + JSConfig.SpreadArgumentPlaceholderCount);
            for (JavaScriptNode node : elements) {
                if (node instanceof SpreadTupleNode) {
                    ((SpreadTupleNode) node).executeToList(frame, sequence, growProfile);
                } else {
                    Object value = node.execute(frame);
                    addValueToTupleSequenceList(sequence, value, growProfile);
                }
            }
            return createTuple(sequence.toArray());
        }

        @Override
        protected JavaScriptNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
            return new DefaultTupleLiteralWithSpreadNode(context, cloneUninitialized(elements, materializedTags));
        }
    }

    public static final class SpreadTupleNode extends JavaScriptNode {
        @Child private GetIteratorNode getIteratorNode;
        @Child private IteratorGetNextValueNode iteratorStepNode;

        private SpreadTupleNode(JSContext context, JavaScriptNode arg) {
            this.getIteratorNode = GetIteratorNode.create(context, arg);
            this.iteratorStepNode = IteratorGetNextValueNode.create(context, null, JSConstantNode.create(null), false);
        }

        public static SpreadTupleNode create(JSContext context, JavaScriptNode arg) {
            return new SpreadTupleNode(context, arg);
        }

        public void executeToList(VirtualFrame frame, SimpleArrayList<Object> toList, BranchProfile growProfile) {
            IteratorRecord iteratorRecord = getIteratorNode.execute(frame);
            for (;;) {
                Object nextArg = iteratorStepNode.execute(frame, iteratorRecord);
                if (nextArg == null) {
                    break;
                }
                addValueToTupleSequenceList(toList, nextArg, growProfile);
            }
        }

        @Override
        public Object execute(VirtualFrame frame) {
            throw Errors.shouldNotReachHere("Cannot execute SpreadTupleNode");
        }

        @Override
        protected JavaScriptNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
            SpreadTupleNode copy = (SpreadTupleNode) copy();
            copy.getIteratorNode = cloneUninitialized(getIteratorNode, materializedTags);
            copy.iteratorStepNode = cloneUninitialized(iteratorStepNode, materializedTags);
            return copy;
        }
    }

    @Override
    public boolean isResultAlwaysOfType(Class<?> clazz) {
        return clazz == Tuple.class;
    }

    @Override
    public boolean hasTag(Class<? extends Tag> tag) {
        if (tag == LiteralTag.class) {
            return true;
        } else {
            return super.hasTag(tag);
        }
    }

    @Override
    public Object getNodeObject() {
        return JSTags.createNodeObjectDescriptor(LiteralTag.TYPE, LiteralTag.Type.TupleLiteral.name());
    }
}
