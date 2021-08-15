/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.GenerateWrapper;
import com.oracle.truffle.api.instrumentation.ProbeNode;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.control.EmptyNode;
import com.oracle.truffle.js.nodes.instrumentation.JSTags;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.LiteralTag;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.array.ScriptArray;
import com.oracle.truffle.js.runtime.array.dyn.AbstractConstantArray;
import com.oracle.truffle.js.runtime.array.dyn.ConstantByteArray;
import com.oracle.truffle.js.runtime.array.dyn.ConstantDoubleArray;
import com.oracle.truffle.js.runtime.array.dyn.ConstantIntArray;
import com.oracle.truffle.js.runtime.array.dyn.ConstantObjectArray;
import com.oracle.truffle.js.runtime.array.dyn.HolesIntArray;
import com.oracle.truffle.js.runtime.builtins.JSArray;
import com.oracle.truffle.js.runtime.objects.IteratorRecord;
import com.oracle.truffle.js.runtime.util.SimpleArrayList;

import java.util.Set;

@GenerateWrapper
public abstract class ArrayLiteralNode extends JavaScriptNode {

    protected final JSContext context;

    public ArrayLiteralNode(ArrayLiteralNode copy) {
        this.context = copy.context;
    }

    protected ArrayLiteralNode(JSContext context) {
        this.context = context;
    }

    @Override
    public abstract DynamicObject execute(VirtualFrame frame);

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
        return JSTags.createNodeObjectDescriptor(LiteralTag.TYPE, LiteralTag.Type.ArrayLiteral.name());
    }

    @Override
    public WrapperNode createWrapper(ProbeNode probe) {
        return new ArrayLiteralNodeWrapper(this, this, probe);
    }

    public static ArrayLiteralNode create(JSContext context, JavaScriptNode[] elements) {
        if (elements == null || elements.length == 0) {
            return new ConstantEmptyArrayLiteralNode(context);
        }

        Object[] constantValues = resolveConstants(elements);
        if (constantValues != null) {
            return createConstantArray(context, elements, constantValues);
        }

        for (JavaScriptNode element : elements) {
            if (element instanceof EmptyNode) {
                return new DefaultObjectArrayWithEmptyLiteralNode(context, elements);
            }
        }
        if (elements.length == 1) {
            return new DefaultArrayLiteralOneElementNode(context, elements[0]);
        } else {
            return new DefaultArrayLiteralNode(context, elements);
        }
    }

    public static ArrayLiteralNode createWithSpread(JSContext context, JavaScriptNode[] elements) {
        return new DefaultArrayLiteralWithSpreadNode(context, elements);
    }

    private static ArrayLiteralNode createConstantArray(JSContext context, JavaScriptNode[] elements, Object[] constantValues) {
        ArrayContentType type = identifyPrimitiveContentType(constantValues, true);

        if (type == ArrayContentType.Byte) {
            return new ConstantArrayLiteralNode(context, ConstantByteArray.createConstantByteArray(), createByteArray(constantValues), elements.length);
        } else if (type == ArrayContentType.Integer) {
            return new ConstantArrayLiteralNode(context, ConstantIntArray.createConstantIntArray(), createIntArray(constantValues), elements.length);
        } else if (type == ArrayContentType.Double) {
            return new ConstantArrayLiteralNode(context, ConstantDoubleArray.createConstantDoubleArray(), createDoubleArray(constantValues), elements.length);
        } else {
            return createConstantObjectArray(context, elements, constantValues);
        }
    }

    private static ArrayLiteralNode createConstantObjectArray(JSContext context, JavaScriptNode[] elements, Object array) {
        boolean hasEmpty = false;
        boolean emptyOnly = true;
        for (Object value : (Object[]) array) {
            if (value == null) {
                hasEmpty = true;
            } else {
                emptyOnly = false;
            }
        }
        if (emptyOnly) {
            return new ConstantEmptyArrayWithCapLiteralNode(context, elements.length);
        } else {
            if (hasEmpty) {
                return new ConstantArrayLiteralNode(context, ConstantObjectArray.createConstantHolesObjectArray(), array, elements.length);
            } else {
                return new ConstantArrayLiteralNode(context, ConstantObjectArray.createConstantObjectArray(), array, elements.length);
            }
        }
    }

    private static Object[] resolveConstants(JavaScriptNode[] nodes) {
        Object[] values = new Object[nodes.length];
        for (int i = 0; i < values.length; i++) {
            JavaScriptNode node = nodes[i];
            if (node instanceof JSConstantNode) {
                values[i] = ((JSConstantNode) node).getValue();
            } else if (node instanceof EmptyNode) {
                values[i] = null;
            } else {
                return null;
            }
        }
        return values;
    }

    public static ArrayContentType identifyPrimitiveContentType(Object[] values, boolean createBytes) {
        boolean bytes = createBytes;
        boolean integers = true;
        boolean hasHoles = false;

        for (int i = 0; i < values.length; i++) {
            Object value = values[i];
            if (value == null) {
                hasHoles = true;
            } else if (integers && value instanceof Integer) {
                bytes = bytes && ScriptArray.valueIsByte((int) value);
            } else if (value instanceof Double) {
                bytes = false;
                integers = false;
            } else if (!(value instanceof Integer || value instanceof Double)) {
                return ArrayContentType.Object;
            }
        }

        if (bytes) {
            return hasHoles ? ArrayContentType.ByteWithHoles : ArrayContentType.Byte;
        } else if (integers) {
            return hasHoles ? ArrayContentType.IntegerWithHoles : ArrayContentType.Integer;
        } else {
            return hasHoles ? ArrayContentType.DoubleWithHoles : ArrayContentType.Double;
        }
    }

    private static Object createPrimitiveArray(Object[] values, boolean createBytes) {
        ArrayContentType type = identifyPrimitiveContentType(values, createBytes);
        if (type == ArrayContentType.Byte) {
            return createByteArray(values);
        } else if (type == ArrayContentType.Integer) {
            return createIntArray(values);
        } else if (type == ArrayContentType.Double) {
            return createDoubleArray(values);
        } else {
            return values;
        }
    }

    public static double[] createDoubleArray(Object[] values) {
        double[] doubleArray = new double[values.length];
        for (int i = 0; i < values.length; i++) {
            Object oValue = values[i];
            if (oValue instanceof Double) {
                doubleArray[i] = (double) oValue;
            } else if (oValue instanceof Integer) {
                doubleArray[i] = (int) oValue;
            }
        }
        return doubleArray;
    }

    public static int[] createIntArray(Object[] values) {
        int[] intArray = new int[values.length];
        for (int i = 0; i < values.length; i++) {
            if (values[i] == null) {
                intArray[i] = HolesIntArray.HOLE_VALUE;
            } else {
                intArray[i] = (int) values[i];
            }
        }
        return intArray;
    }

    public static byte[] createByteArray(Object[] values) {
        byte[] byteArray = new byte[values.length];
        for (int i = 0; i < values.length; i++) {
            byteArray[i] = (byte) ((int) values[i]);
        }
        return byteArray;
    }

    private abstract static class DefaultArrayLiteralBaseNode extends ArrayLiteralNode {
        @CompilationFinal protected byte state;
        protected static final byte INT_ARRAY = 1;
        protected static final byte DOUBLE_ARRAY = 2;
        protected static final byte OBJECT_ARRAY = 3;
        @CompilationFinal protected boolean seenUnexpectedInteger;

        DefaultArrayLiteralBaseNode(JSContext context) {
            super(context);
        }

        protected abstract int getLength();

        protected abstract JavaScriptNode getElement(int index);

        protected final DynamicObject executeAndSpecialize(Object[] values) {
            CompilerAsserts.neverPartOfCompilation();
            Object primitive = createPrimitiveArray(values, false);
            JSRealm realm = getRealm();
            if (primitive instanceof int[]) {
                state = INT_ARRAY;
                return JSArray.createZeroBasedIntArray(context, realm, (int[]) primitive);
            } else if (primitive instanceof double[]) {
                state = DOUBLE_ARRAY;
                return JSArray.createZeroBasedDoubleArray(context, realm, (double[]) primitive);
            } else if (primitive instanceof Object[]) {
                state = OBJECT_ARRAY;
                return JSArray.createZeroBasedObjectArray(context, realm, values);
            } else {
                throw Errors.shouldNotReachHere();
            }
        }

        @Override
        public DynamicObject execute(VirtualFrame frame) {
            if (state == 0) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                Object[] values = new Object[getLength()];
                for (int i = 0; i < getLength(); i++) {
                    values[i] = getElement(i).execute(frame);
                }
                return executeAndSpecialize(values);
            }
            if (state == INT_ARRAY) {
                return executeZeroBasedIntArray(frame);
            } else if (state == DOUBLE_ARRAY) {
                return executeZeroBasedDoubleArray(frame);
            } else {
                assert state == OBJECT_ARRAY;
                return executeZeroBasedObjectArray(frame);

            }
        }

        @ExplodeLoop
        private DynamicObject executeZeroBasedIntArray(VirtualFrame frame) {
            int[] primitiveArray = new int[getLength()];
            for (int i = 0; i < getLength(); i++) {
                try {
                    primitiveArray[i] = getElement(i).executeInt(frame);
                } catch (UnexpectedResultException e) {
                    assert !(e.getResult() instanceof Integer);
                    return executeIntArrayFallback(frame, primitiveArray, i, e.getResult());
                }
            }
            return JSArray.createZeroBasedIntArray(context, getRealm(), primitiveArray);
        }

        private DynamicObject executeIntArrayFallback(VirtualFrame frame, int[] primitiveArray, int failIdx, Object failValue) {
            Object[] objectArray = new Object[getLength()];
            for (int j = 0; j < failIdx; j++) {
                objectArray[j] = primitiveArray[j];
            }
            return executeFallback(frame, objectArray, failIdx, failValue);
        }

        @ExplodeLoop
        private DynamicObject executeZeroBasedDoubleArray(VirtualFrame frame) {
            double[] primitiveArray = new double[getLength()];
            for (int i = 0; i < getLength(); i++) {
                try {
                    double doubleValue;
                    if (seenUnexpectedInteger) {
                        Object objectValue = getElement(i).execute(frame);
                        if (objectValue instanceof Double) {
                            doubleValue = (double) objectValue;
                        } else if (objectValue instanceof Integer) {
                            doubleValue = (int) objectValue;
                        } else {
                            CompilerDirectives.transferToInterpreterAndInvalidate();
                            throw new UnexpectedResultException(objectValue);
                        }
                    } else {
                        doubleValue = getElement(i).executeDouble(frame);
                    }
                    primitiveArray[i] = doubleValue;
                } catch (UnexpectedResultException e) {
                    assert !(e.getResult() instanceof Double);
                    if (e.getResult() instanceof Integer) {
                        primitiveArray[i] = (int) e.getResult();
                        seenUnexpectedInteger = true;
                        continue;
                    }
                    return executeDoubleArrayFallback(frame, primitiveArray, i, e.getResult());
                }
            }
            return JSArray.createZeroBasedDoubleArray(context, getRealm(), primitiveArray);
        }

        private DynamicObject executeDoubleArrayFallback(VirtualFrame frame, double[] primitiveArray, int failIdx, Object failValue) {
            Object[] objectArray = new Object[getLength()];
            for (int j = 0; j < failIdx; j++) {
                objectArray[j] = primitiveArray[j];
            }
            return executeFallback(frame, objectArray, failIdx, failValue);
        }

        @ExplodeLoop
        private DynamicObject executeZeroBasedObjectArray(VirtualFrame frame) {
            Object[] primitiveArray = new Object[getLength()];
            for (int i = 0; i < getLength(); i++) {
                primitiveArray[i] = getElement(i).execute(frame);
            }
            return JSArray.createZeroBasedObjectArray(context, getRealm(), primitiveArray);
        }

        private DynamicObject executeFallback(VirtualFrame frame, Object[] objectArray, int failingIndex, Object failingValue) {
            objectArray[failingIndex] = failingValue;
            for (int j = failingIndex + 1; j < getLength(); j++) {
                objectArray[j] = getElement(j).execute(frame);
            }
            return executeAndSpecialize(objectArray);
        }
    }

    @NodeInfo(cost = NodeCost.MONOMORPHIC)
    private static class DefaultArrayLiteralNode extends DefaultArrayLiteralBaseNode {

        @Children protected final JavaScriptNode[] elements;

        DefaultArrayLiteralNode(JSContext context, JavaScriptNode[] elements) {
            super(context);
            this.elements = elements;
        }

        @Override
        protected JavaScriptNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
            return new DefaultArrayLiteralNode(context, cloneUninitialized(elements, materializedTags));
        }

        @Override
        protected int getLength() {
            return elements.length;
        }

        @Override
        protected JavaScriptNode getElement(int index) {
            return elements[index];
        }
    }

    @NodeInfo(cost = NodeCost.MONOMORPHIC)
    private static class DefaultArrayLiteralOneElementNode extends DefaultArrayLiteralBaseNode {

        @Child protected JavaScriptNode child;

        DefaultArrayLiteralOneElementNode(JSContext context, JavaScriptNode child) {
            super(context);
            this.child = child;
        }

        @Override
        protected JavaScriptNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
            return new DefaultArrayLiteralOneElementNode(context, cloneUninitialized(child, materializedTags));
        }

        @Override
        protected int getLength() {
            return 1;
        }

        @Override
        protected JavaScriptNode getElement(int index) {
            assert index == 0;
            return child;
        }
    }

    @NodeInfo(cost = NodeCost.MONOMORPHIC)
    private static final class DefaultObjectArrayWithEmptyLiteralNode extends DefaultArrayLiteralNode {

        DefaultObjectArrayWithEmptyLiteralNode(JSContext context, JavaScriptNode[] elements) {
            super(context, elements);
            assert elements.length > 0;
        }

        @ExplodeLoop
        @Override
        public DynamicObject execute(VirtualFrame frame) {
            Object[] primitiveArray = new Object[elements.length];
            int holeCount = 0;
            int arrayOffset = 0;
            int lastNonEmpty = -1;
            for (int i = 0; i < elements.length; i++) {
                if (elements[i] instanceof EmptyNode) {
                    holeCount++;
                    if (i == arrayOffset) {
                        arrayOffset++;
                    }
                } else {
                    primitiveArray[i] = elements[i].execute(frame);
                    lastNonEmpty = i;
                }
            }
            int usedLength = lastNonEmpty + 1 - arrayOffset;
            return JSArray.createZeroBasedHolesObjectArray(context, getRealm(), primitiveArray, usedLength, arrayOffset, holeCount);
        }

        @Override
        protected JavaScriptNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
            return new DefaultObjectArrayWithEmptyLiteralNode(context, cloneUninitialized(elements, materializedTags));
        }
    }

    private static final class ConstantArrayLiteralNode extends ArrayLiteralNode {

        private final AbstractConstantArray arrayType;
        private final Object array;
        private final long length;

        ConstantArrayLiteralNode(JSContext context, AbstractConstantArray arrayType, Object array, long length) {
            super(context);
            this.arrayType = arrayType;
            this.array = array;
            this.length = length;
        }

        @Override
        public DynamicObject execute(VirtualFrame frame) {
            return JSArray.create(context, getRealm(), arrayType, array, length);
        }

        @Override
        protected JavaScriptNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
            return copy();
        }
    }

    private static final class ConstantEmptyArrayWithCapLiteralNode extends ArrayLiteralNode {

        private final int capacity;

        ConstantEmptyArrayWithCapLiteralNode(JSContext context, int cap) {
            super(context);
            this.capacity = cap;
        }

        @Override
        public DynamicObject execute(VirtualFrame frame) {
            return JSArray.createConstantEmptyArray(context, getRealm(), capacity);
        }

        @Override
        protected JavaScriptNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
            return copy();
        }
    }

    private static final class ConstantEmptyArrayLiteralNode extends ArrayLiteralNode {

        ConstantEmptyArrayLiteralNode(JSContext context) {
            super(context);
        }

        @Override
        public DynamicObject execute(VirtualFrame frame) {
            return JSArray.createConstantEmptyArray(context, getRealm());
        }

        @Override
        protected JavaScriptNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
            return copy();
        }
    }

    private static final class DefaultArrayLiteralWithSpreadNode extends DefaultArrayLiteralNode {

        private final BranchProfile growProfile = BranchProfile.create();

        DefaultArrayLiteralWithSpreadNode(JSContext context, JavaScriptNode[] elements) {
            super(context, elements);
            assert elements.length > 0;
        }

        @ExplodeLoop
        @Override
        public DynamicObject execute(VirtualFrame frame) {
            SimpleArrayList<Object> evaluatedElements = new SimpleArrayList<>(elements.length + JSConfig.SpreadArgumentPlaceholderCount);
            int holeCount = 0;
            int arrayOffset = 0;
            int lastNonEmptyPlusOne = 0;
            for (int i = 0; i < elements.length; i++) {
                Node node = elements[i];
                if (elements[i] instanceof WrapperNode) {
                    node = ((WrapperNode) elements[i]).getDelegateNode();
                }
                if (node instanceof EmptyNode) {
                    evaluatedElements.add(null, growProfile);
                    holeCount++;
                    if (i == arrayOffset) {
                        arrayOffset++;
                    }
                } else if (node instanceof SpreadArrayNode) {
                    int count = ((SpreadArrayNode) node).executeToList(frame, evaluatedElements, growProfile);
                    if (count != 0) {
                        lastNonEmptyPlusOne = evaluatedElements.size();
                    }
                } else {
                    evaluatedElements.add(elements[i].execute(frame), growProfile);
                    lastNonEmptyPlusOne = evaluatedElements.size();
                }
            }
            int usedLength = lastNonEmptyPlusOne - arrayOffset;
            return JSArray.createZeroBasedHolesObjectArray(context, getRealm(), evaluatedElements.toArray(), usedLength, arrayOffset, holeCount);
        }

        @Override
        protected JavaScriptNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
            return new DefaultArrayLiteralWithSpreadNode(context, cloneUninitialized(elements, materializedTags));
        }
    }

    public static final class SpreadArrayNode extends JavaScriptNode {
        @Child private GetIteratorNode getIteratorNode;
        @Child private IteratorGetNextValueNode iteratorStepNode;

        private SpreadArrayNode(JSContext context, JavaScriptNode arg) {
            this.getIteratorNode = GetIteratorNode.create(context, arg);
            this.iteratorStepNode = IteratorGetNextValueNode.create(context, null, JSConstantNode.create(null), false);
        }

        public static SpreadArrayNode create(JSContext context, JavaScriptNode arg) {
            return new SpreadArrayNode(context, arg);
        }

        public int executeToList(VirtualFrame frame, SimpleArrayList<Object> toList, BranchProfile growProfile) {
            IteratorRecord iteratorRecord = getIteratorNode.execute(frame);
            int count = 0;
            for (;;) {
                Object nextArg = iteratorStepNode.execute(frame, iteratorRecord);
                if (nextArg == null) {
                    break;
                }
                toList.add(nextArg, growProfile);
                count++;
            }
            return count;
        }

        @Override
        public Object execute(VirtualFrame frame) {
            throw Errors.shouldNotReachHere("Cannot execute SpreadArrayNode");
        }

        @Override
        protected JavaScriptNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
            SpreadArrayNode copy = (SpreadArrayNode) copy();
            copy.getIteratorNode = cloneUninitialized(getIteratorNode, materializedTags);
            copy.iteratorStepNode = cloneUninitialized(iteratorStepNode, materializedTags);
            return copy;
        }
    }

    @Override
    public boolean isResultAlwaysOfType(Class<?> clazz) {
        return clazz == DynamicObject.class;
    }

    public enum ArrayContentType {
        Byte,
        ByteWithHoles,
        Integer,
        IntegerWithHoles,
        Double,
        DoubleWithHoles,
        Object
    }
}
