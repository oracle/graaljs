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
package com.oracle.truffle.js.builtins;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.js.builtins.DebugBuiltinsFactory.DebugClassNameNodeGen;
import com.oracle.truffle.js.builtins.DebugBuiltinsFactory.DebugClassNodeGen;
import com.oracle.truffle.js.builtins.DebugBuiltinsFactory.DebugContinueInInterpreterNodeGen;
import com.oracle.truffle.js.builtins.DebugBuiltinsFactory.DebugStringCompareNodeGen;
import com.oracle.truffle.js.builtins.DebugBuiltinsFactory.DebugToLengthNodeGen;
import com.oracle.truffle.js.builtins.DebugBuiltinsFactory.DebugTypedArrayDetachBufferNodeGen;
import com.oracle.truffle.js.builtins.TestV8BuiltinsFactory.TestV8ConstructDoubleNodeGen;
import com.oracle.truffle.js.builtins.TestV8BuiltinsFactory.TestV8DoublePartNodeGen;
import com.oracle.truffle.js.builtins.TestV8BuiltinsFactory.TestV8ToNameNodeGen;
import com.oracle.truffle.js.builtins.TestV8BuiltinsFactory.TestV8ToNumberNodeGen;
import com.oracle.truffle.js.builtins.TestV8BuiltinsFactory.TestV8ToPrimitiveNodeGen;
import com.oracle.truffle.js.builtins.TestV8BuiltinsFactory.TestV8ToStringNodeGen;
import com.oracle.truffle.js.nodes.cast.JSToNumberNode;
import com.oracle.truffle.js.nodes.cast.JSToPrimitiveNode;
import com.oracle.truffle.js.nodes.cast.JSToStringNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSTestV8;

/**
 * Contains builtins to support special behavior used by TestV8.
 */
public final class TestV8Builtins extends JSBuiltinsContainer.SwitchEnum<TestV8Builtins.TestV8> {

    public static final String CLASS_NAME = "TestV8";

    protected TestV8Builtins() {
        super(JSTestV8.CLASS_NAME, TestV8.class);
    }

    public enum TestV8 implements BuiltinEnum<TestV8> {
        class_(1),
        className(1),
        stringCompare(2),
        typedArrayDetachBuffer(1),

        constructDouble(2),
        doubleHi(1),
        doubleLo(1),
        deoptimize(0),
        toLength(1),
        toStringConv(1),
        toName(1),
        toNumber(1),
        toPrimitive(1),
        toPrimitiveString(1),
        toPrimitiveNumber(1);

        private final int length;

        TestV8(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, TestV8 builtinEnum) {
        switch (builtinEnum) {
            case class_:
                return DebugClassNodeGen.create(context, builtin, true, args().fixedArgs(1).createArgumentNodes(context));
            case className:
                return DebugClassNameNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
            case stringCompare:
                return DebugStringCompareNodeGen.create(context, builtin, args().fixedArgs(2).createArgumentNodes(context));
            case typedArrayDetachBuffer:
                return DebugTypedArrayDetachBufferNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));

            case constructDouble:
                return TestV8ConstructDoubleNodeGen.create(context, builtin, args().fixedArgs(2).createArgumentNodes(context));
            case doubleHi:
                return TestV8DoublePartNodeGen.create(context, builtin, true, args().fixedArgs(1).createArgumentNodes(context));
            case doubleLo:
                return TestV8DoublePartNodeGen.create(context, builtin, false, args().fixedArgs(1).createArgumentNodes(context));
            case deoptimize:
                return DebugContinueInInterpreterNodeGen.create(context, builtin, true, args().createArgumentNodes(context));
            case toStringConv:
                return TestV8ToStringNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
            case toName:
                return TestV8ToNameNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
            case toPrimitive:
                return TestV8ToPrimitiveNodeGen.create(context, builtin, JSToPrimitiveNode.Hint.None, args().fixedArgs(1).createArgumentNodes(context));
            case toPrimitiveString:
                return TestV8ToPrimitiveNodeGen.create(context, builtin, JSToPrimitiveNode.Hint.String, args().fixedArgs(1).createArgumentNodes(context));
            case toPrimitiveNumber:
                return TestV8ToPrimitiveNodeGen.create(context, builtin, JSToPrimitiveNode.Hint.Number, args().fixedArgs(1).createArgumentNodes(context));
            case toNumber:
                return TestV8ToNumberNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
            case toLength:
                return DebugToLengthNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
        }
        return null;
    }

    /**
     * Constructs a double from two 32bit ints. Used by V8ConstructDouble (v8mockup.js).
     */
    public abstract static class TestV8ConstructDoubleNode extends JSBuiltinNode {

        public TestV8ConstructDoubleNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected double constructDouble(Object hiObj, Object loObj) {
            long lHi = ((Number) hiObj).longValue();
            long lLo = ((Number) loObj).longValue() & 0xFFFF_FFFFL;
            return Double.longBitsToDouble((lHi << 32L) | lLo);
        }
    }

    /**
     * Gets the upper (hi) or lower (lo) 32 bits of a double. Used by V8DoubleHi, V8DoubleLo
     * (v8mockup.js).
     */
    public abstract static class TestV8DoublePartNode extends JSBuiltinNode {
        private final boolean upper;

        public TestV8DoublePartNode(JSContext context, JSBuiltin builtin, boolean upper) {
            super(context, builtin);
            this.upper = upper;
        }

        @Specialization
        protected int doublePart(Object value) {
            long bits = Double.doubleToRawLongBits((double) value);
            return upper ? (int) (bits >>> 32L) : (int) (bits & 0xFFFF_FFFFL);
        }
    }

    /**
     * Calls [[ToString]], used by v8mockup.js.
     */
    public abstract static class TestV8ToStringNode extends JSBuiltinNode {
        @Child private JSToStringNode toStringNode;

        public TestV8ToStringNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
            toStringNode = JSToStringNode.create();
        }

        @Specialization
        protected String toStringConv(Object obj) {
            return toStringNode.executeString(obj);
        }
    }

    /**
     * Calls [[ToNumber]], used by v8mockup.js.
     */
    public abstract static class TestV8ToNumberNode extends JSBuiltinNode {
        @Child private JSToNumberNode toNumberNode;

        public TestV8ToNumberNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
            toNumberNode = JSToNumberNode.create();
        }

        @Specialization
        protected Number toNumberOp(Object obj) {
            return toNumberNode.executeNumber(obj);
        }
    }

    /**
     * Calls [[ToPrimitive]], used by v8mockup.js .
     */
    public abstract static class TestV8ToPrimitiveNode extends JSBuiltinNode {
        @Child private JSToPrimitiveNode toPrimitiveNode;

        public TestV8ToPrimitiveNode(JSContext context, JSBuiltin builtin, JSToPrimitiveNode.Hint hint) {
            super(context, builtin);
            toPrimitiveNode = JSToPrimitiveNode.create(hint);
        }

        @Specialization
        protected Object toPrimitive(Object obj) {
            return toPrimitiveNode.execute(obj);
        }
    }

    /**
     * Calls [[ToName]], used by v8mockup.js.
     */
    public abstract static class TestV8ToNameNode extends JSBuiltinNode {
        @Child private JSToStringNode toStringNode;

        public TestV8ToNameNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
            toStringNode = JSToStringNode.create();
        }

        @Specialization
        protected Object toName(Object obj) {
            if (obj instanceof Symbol) {
                return obj;
            } else {
                return toStringNode.executeString(obj);
            }
        }
    }
}
