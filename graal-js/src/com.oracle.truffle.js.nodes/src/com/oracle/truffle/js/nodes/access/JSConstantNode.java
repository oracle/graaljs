/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.access;

import java.util.Map;
import java.util.Objects;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.RepeatableNode;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.Null;
import com.oracle.truffle.js.runtime.objects.Undefined;

public abstract class JSConstantNode extends JavaScriptNode implements RepeatableNode {

    public static JSConstantNode create(Object value) {
        assert !(value instanceof Long);
        if (value instanceof Integer) {
            return createInt((Integer) value);
        } else if (value instanceof Double) {
            double doubleValue = (Double) value;
            if (JSRuntime.doubleIsRepresentableAsInt(doubleValue)) {
                return createInt((int) doubleValue);
            } else {
                return createDouble(doubleValue);
            }
        } else if (value instanceof Boolean) {
            return createBoolean((Boolean) value);
        } else if (value instanceof String) {
            return createString((String) value);
        } else if (value == Null.instance) {
            return createNull();
        } else if (value == Undefined.instance) {
            return createUndefined();
        } else if (JSObject.isDynamicObject(value)) {
            return new JSConstantJSObjectNode((DynamicObject) value);
        } else {
            return new JSConstantObjectNode(value);
        }
    }

    public static JSConstantNode createUndefined() {
        return new JSConstantUndefinedNode();
    }

    public static JSConstantNode createNull() {
        return new JSConstantNullNode();
    }

    public static JSConstantNode createInt(int value) {
        return new JSConstantIntegerNode(value);
    }

    public static JSConstantNode createDouble(double value) {
        return new JSConstantDoubleNode(value);
    }

    public static JSConstantNode createBoolean(boolean value) {
        return new JSConstantBooleanNode(value);
    }

    public static JSConstantNode createString(String value) {
        return new JSConstantStringNode(value);
    }

    public static final class JSConstantDoubleNode extends JSConstantNode {
        private final double doubleValue;

        private JSConstantDoubleNode(double doubleValue) {
            this.doubleValue = doubleValue;
        }

        @Override
        public Object execute(VirtualFrame frame) {
            return doubleValue;
        }

        @Override
        public double executeDouble(VirtualFrame frame) {
            return doubleValue;
        }

        @Override
        public boolean isResultAlwaysOfType(Class<?> clazz) {
            return clazz == double.class;
        }

        @Override
        public Object getValue() {
            return doubleValue;
        }
    }

    public static final class JSConstantIntegerNode extends JSConstantNode {
        private final int intValue;

        private JSConstantIntegerNode(int value) {
            this.intValue = value;
        }

        @Override
        public int executeInt(VirtualFrame frame) {
            return intValue;
        }

        @Override
        public double executeDouble(VirtualFrame frame) {
            return intValue;
        }

        @Override
        public Object execute(VirtualFrame frame) {
            return intValue;
        }

        @Override
        public boolean isResultAlwaysOfType(Class<?> clazz) {
            return clazz == int.class;
        }

        @Override
        public Object getValue() {
            return intValue;
        }
    }

    public static final class JSConstantBooleanNode extends JSConstantNode {
        private final boolean booleanValue;

        private JSConstantBooleanNode(boolean value) {
            this.booleanValue = value;
        }

        @Override
        public boolean isResultAlwaysOfType(Class<?> clazz) {
            return clazz == boolean.class;
        }

        @Override
        public Object execute(VirtualFrame frame) {
            return booleanValue;
        }

        @Override
        public double executeDouble(VirtualFrame frame) {
            return booleanValue ? 1 : 0;
        }

        @Override
        public boolean executeBoolean(VirtualFrame frame) {
            return booleanValue;
        }

        @Override
        public Object getValue() {
            return booleanValue;
        }
    }

    private static final class JSConstantObjectNode extends JSConstantNode {
        private final Object objectValue;

        private JSConstantObjectNode(Object obj) {
            this.objectValue = obj;
            assert !(obj instanceof JavaScriptNode) : "must be JS value";
        }

        @Override
        public Object execute(VirtualFrame frame) {
            return objectValue;
        }

        @Override
        public Object getValue() {
            return objectValue;
        }
    }

    private static final class JSConstantJSObjectNode extends JSConstantNode {
        private final DynamicObject objectValue;

        private JSConstantJSObjectNode(DynamicObject obj) {
            this.objectValue = obj;
        }

        @Override
        public Object execute(VirtualFrame frame) {
            return objectValue;
        }

        @Override
        public DynamicObject executeDynamicObject(VirtualFrame frame) {
            return objectValue;
        }

        @Override
        public boolean isResultAlwaysOfType(Class<?> clazz) {
            return clazz == DynamicObject.class;
        }

        @Override
        public Object getValue() {
            return objectValue;
        }
    }

    public static final class JSConstantNullNode extends JSConstantNode {
        private JSConstantNullNode() {
        }

        @Override
        public Object execute(VirtualFrame frame) {
            return Null.instance;
        }

        @Override
        public DynamicObject executeDynamicObject(VirtualFrame frame) {
            return Null.instance;
        }

        @Override
        public boolean isResultAlwaysOfType(Class<?> clazz) {
            return clazz == DynamicObject.class;
        }

        @Override
        public Object getValue() {
            return Null.instance;
        }
    }

    public static final class JSConstantUndefinedNode extends JSConstantNode {
        private JSConstantUndefinedNode() {
        }

        @Override
        public Object execute(VirtualFrame frame) {
            return Undefined.instance;
        }

        @Override
        public DynamicObject executeDynamicObject(VirtualFrame frame) {
            return Undefined.instance;
        }

        @Override
        public boolean isResultAlwaysOfType(Class<?> clazz) {
            return clazz == DynamicObject.class;
        }

        @Override
        public Object getValue() {
            return Undefined.instance;
        }
    }

    public static final class JSConstantStringNode extends JSConstantNode {

        private final String stringValue;

        private JSConstantStringNode(String str) {
            this.stringValue = Objects.requireNonNull(str);
        }

        @Override
        public Object execute(VirtualFrame frame) {
            return stringValue;
        }

        @Override
        public String executeString(VirtualFrame frame) {
            return stringValue;
        }

        @Override
        public boolean isResultAlwaysOfType(Class<?> clazz) {
            return clazz == String.class;
        }

        @Override
        public Object getValue() {
            return stringValue;
        }
    }

    public abstract Object getValue();

    @Override
    public final void executeVoid(VirtualFrame frame) {
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        return copy();
    }

    @Override
    @TruffleBoundary
    public Map<String, Object> getDebugProperties() {
        Map<String, Object> map = super.getDebugProperties();
        map.put("value", getValue() instanceof String ? JSRuntime.quote((String) getValue()) : getValue());
        return map;
    }

    @Override
    public String expressionToString() {
        Object value = getValue();
        if (JSRuntime.isJSPrimitive(value)) {
            String string = JSRuntime.toString(value);
            if (JSRuntime.isString(value)) {
                return JSRuntime.quote(string);
            } else {
                return string;
            }
        }
        return null;
    }
}
