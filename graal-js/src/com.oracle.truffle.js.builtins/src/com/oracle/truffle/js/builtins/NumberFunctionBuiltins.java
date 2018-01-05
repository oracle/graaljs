/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.builtins;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.js.builtins.NumberFunctionBuiltinsFactory.JSNumberIsFiniteNodeGen;
import com.oracle.truffle.js.builtins.NumberFunctionBuiltinsFactory.JSNumberIsIntegerNodeGen;
import com.oracle.truffle.js.builtins.NumberFunctionBuiltinsFactory.JSNumberIsNaNNodeGen;
import com.oracle.truffle.js.builtins.NumberFunctionBuiltinsFactory.JSNumberIsSafeIntegerNodeGen;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSNumber;

/**
 * Contains builtins for {@linkplain JSNumber} function (constructor).
 */
public final class NumberFunctionBuiltins extends JSBuiltinsContainer.SwitchEnum<NumberFunctionBuiltins.NumberFunction> {
    protected NumberFunctionBuiltins() {
        super(JSNumber.CLASS_NAME, NumberFunction.class);
    }

    public enum NumberFunction implements BuiltinEnum<NumberFunction> {
        isNaN(1),
        isFinite(1),
        isInteger(1),
        isSafeInteger(1);

        private final int length;

        NumberFunction(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }

        @Override
        public int getECMAScriptVersion() {
            return 6;
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, NumberFunction builtinEnum) {
        switch (builtinEnum) {
            case isNaN:
                return JSNumberIsNaNNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
            case isFinite:
                return JSNumberIsFiniteNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
            case isInteger:
                return JSNumberIsIntegerNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
            case isSafeInteger:
                return JSNumberIsSafeIntegerNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
        }
        return null;
    }

    public abstract static class JSNumberIsNaNNode extends JSBuiltinNode {

        public JSNumberIsNaNNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        protected boolean isDouble(Object arg) {
            return arg instanceof Double;
        }

        @Specialization(guards = "!isDouble(arg)")
        protected boolean isNaN(@SuppressWarnings("unused") Object arg) {
            return false;
        }

        @Specialization
        protected boolean isNaN(double arg) {
            return Double.isNaN(arg);
        }
    }

    public abstract static class JSNumberIsFiniteNode extends JSBuiltinNode {

        public JSNumberIsFiniteNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected boolean isFinite(@SuppressWarnings("unused") int arg) {
            return true;
        }

        @Specialization
        protected boolean isFinite(double arg) {
            return Double.isFinite(arg);
        }

        @Specialization(guards = "!isNumber(arg)")
        protected boolean isFinite(@SuppressWarnings("unused") Object arg) {
            return false;
        }
    }

    public abstract static class JSNumberIsIntegerNode extends JSBuiltinNode {

        public JSNumberIsIntegerNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @SuppressWarnings("unused")
        @Specialization
        protected boolean isInteger(int arg) {
            return true;
        }

        @Specialization
        protected boolean isInteger(double arg) {
            if (JSRuntime.doubleIsRepresentableAsLong(arg)) {
                return true;
            }
            if (Double.isNaN(arg) || !Double.isFinite(arg)) {
                return false;
            }
            return JSRuntime.mathFloor(arg) == arg;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isNumber(arg)")
        protected boolean isInteger(Object arg) {
            return false;
        }
    }

    public abstract static class JSNumberIsSafeIntegerNode extends JSBuiltinNode {

        public JSNumberIsSafeIntegerNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected boolean isSafeInteger(@SuppressWarnings("unused") int arg) {
            return true;
        }

        @Specialization
        protected boolean isSafeInteger(double arg) {
            if (Double.isNaN(arg) || !Double.isFinite(arg)) {
                return false;
            }
            long l = JSRuntime.toInteger(arg);
            if (l != arg) {
                return false;
            }
            return JSRuntime.MIN_SAFE_INTEGER <= l && l <= JSRuntime.MAX_SAFE_INTEGER;
        }

        @Specialization(guards = "!isNumber(arg)")
        protected boolean isSafeInteger(@SuppressWarnings("unused") Object arg) {
            return false;
        }
    }
}
