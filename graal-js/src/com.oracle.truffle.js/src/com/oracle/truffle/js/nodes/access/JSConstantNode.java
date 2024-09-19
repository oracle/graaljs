/*
 * Copyright (c) 2018, 2024, Oracle and/or its affiliates. All rights reserved.
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

import java.math.BigInteger;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.RepeatableNode;
import com.oracle.truffle.js.nodes.instrumentation.JSTags;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.LiteralTag;
import com.oracle.truffle.js.nodes.instrumentation.NodeObjectDescriptor;
import com.oracle.truffle.js.runtime.BigInt;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.SafeInteger;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.Null;
import com.oracle.truffle.js.runtime.objects.Undefined;

public abstract class JSConstantNode extends JavaScriptNode implements RepeatableNode {

    public static JSConstantNode create(Object value) {
        assert !(value instanceof Long || value instanceof BigInteger);
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
        } else if (value instanceof TruffleString) {
            return createString((TruffleString) value);
        } else if (value == Null.instance) {
            return createNull();
        } else if (value == Undefined.instance) {
            return createUndefined();
        } else if (value instanceof BigInt) {
            return createBigInt((BigInt) value);
        } else if (value instanceof SafeInteger) {
            return createSafeInteger((SafeInteger) value);
        } else if (JSDynamicObject.isJSDynamicObject(value)) {
            return new JSConstantJSObjectNode((JSDynamicObject) value);
        } else {
            assert !(value instanceof String);
            return new JSConstantObjectNode(value);
        }
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
        NodeObjectDescriptor descriptor = JSTags.createNodeObjectDescriptor();
        if (this instanceof JSConstantDoubleNode || this instanceof JSConstantIntegerNode || this instanceof JSConstantSafeIntegerNode) {
            descriptor.addProperty(LiteralTag.TYPE, LiteralTag.Type.NumericLiteral.name());
        } else if (this instanceof JSConstantBigIntNode) {
            descriptor.addProperty(LiteralTag.TYPE, LiteralTag.Type.BigIntLiteral.name());
        } else if (this instanceof JSConstantBooleanNode) {
            descriptor.addProperty(LiteralTag.TYPE, LiteralTag.Type.BooleanLiteral.name());
        } else if (this instanceof JSConstantStringNode) {
            descriptor.addProperty(LiteralTag.TYPE, LiteralTag.Type.StringLiteral.name());
        } else if (this instanceof JSConstantNullNode) {
            descriptor.addProperty(LiteralTag.TYPE, LiteralTag.Type.NullLiteral.name());
        } else if (this instanceof JSConstantUndefinedNode) {
            descriptor.addProperty(LiteralTag.TYPE, LiteralTag.Type.UndefinedLiteral.name());
        } else if (this instanceof JSConstantObjectNode || this instanceof JSConstantJSObjectNode) {
            descriptor.addProperty(LiteralTag.TYPE, LiteralTag.Type.ObjectLiteral.name());
        }
        return descriptor;
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

    public static JSConstantNode createSafeInteger(SafeInteger value) {
        return new JSConstantSafeIntegerNode(value);
    }

    public static JSConstantNode createBigInt(BigInt value) {
        return new JSConstantBigIntNode(value);
    }

    public static JSConstantNode createDouble(double value) {
        return new JSConstantDoubleNode(value);
    }

    public static JSConstantNode createBoolean(boolean value) {
        return new JSConstantBooleanNode(value);
    }

    public static JSConstantNode createString(TruffleString value) {
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

    public static final class JSConstantBigIntNode extends JSConstantNode {
        private final BigInt bigIntValue;

        private JSConstantBigIntNode(BigInt value) {
            this.bigIntValue = value;
        }

        @Override
        public Object execute(VirtualFrame frame) {
            return bigIntValue;
        }

        public BigInt executeBigInt(@SuppressWarnings("unused") VirtualFrame frame) {
            return bigIntValue;
        }

        @Override
        public boolean isResultAlwaysOfType(Class<?> clazz) {
            return clazz == BigInt.class;
        }

        @Override
        public Object getValue() {
            return bigIntValue;
        }
    }

    public static final class JSConstantSafeIntegerNode extends JSConstantNode {
        private final SafeInteger safeIntValue;

        private JSConstantSafeIntegerNode(SafeInteger value) {
            this.safeIntValue = value;
        }

        @Override
        public Object execute(VirtualFrame frame) {
            return safeIntValue;
        }

        @Override
        public double executeDouble(VirtualFrame frame) {
            return safeIntValue.doubleValue();
        }

        @Override
        public boolean isResultAlwaysOfType(Class<?> clazz) {
            return clazz == Number.class || clazz == double.class;
        }

        @Override
        public Object getValue() {
            return safeIntValue;
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

        @Override
        public boolean hasTag(Class<? extends Tag> tag) {
            if (tag == LiteralTag.class) {
                return false;
            } else {
                return super.hasTag(tag);
            }
        }
    }

    private static final class JSConstantJSObjectNode extends JSConstantNode {
        private final JSDynamicObject objectValue;

        private JSConstantJSObjectNode(JSDynamicObject obj) {
            this.objectValue = obj;
        }

        @Override
        public Object execute(VirtualFrame frame) {
            return objectValue;
        }

        @Override
        public boolean isResultAlwaysOfType(Class<?> clazz) {
            return clazz == JSDynamicObject.class;
        }

        @Override
        public Object getValue() {
            return objectValue;
        }

        @Override
        public boolean hasTag(Class<? extends Tag> tag) {
            if (tag == LiteralTag.class) {
                return false;
            } else {
                return super.hasTag(tag);
            }
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
        public boolean isResultAlwaysOfType(Class<?> clazz) {
            return clazz == JSDynamicObject.class;
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
        public boolean isResultAlwaysOfType(Class<?> clazz) {
            return clazz == JSDynamicObject.class;
        }

        @Override
        public Object getValue() {
            return Undefined.instance;
        }
    }

    public static final class JSConstantStringNode extends JSConstantNode {

        private final TruffleString stringValue;

        private JSConstantStringNode(TruffleString str) {
            this.stringValue = Objects.requireNonNull(str);
        }

        @Override
        public Object execute(VirtualFrame frame) {
            return stringValue;
        }

        @Override
        public boolean isResultAlwaysOfType(Class<?> clazz) {
            return clazz == TruffleString.class;
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
    protected JavaScriptNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
        return copy();
    }

    @Override
    @TruffleBoundary
    public Map<String, Object> getDebugProperties() {
        Map<String, Object> map = super.getDebugProperties();
        map.put("value", getValue() instanceof TruffleString strValue ? JSRuntime.quote(Strings.toJavaString(strValue)) : getValue());
        return map;
    }

    @Override
    public String expressionToString() {
        Object value = getValue();
        if (JSRuntime.isJSPrimitive(value)) {
            String string = JSRuntime.toJavaString(value);
            if (Strings.isTString(value)) {
                return JSRuntime.quote(string);
            } else {
                return string;
            }
        }
        return null;
    }
}
