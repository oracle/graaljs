/*
 * Copyright (c) 2012, 2014, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.cast;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.nodes.SlowPathException;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.cast.JSStringToNumberNode.JSStringToNumberWithTrimNode;
import com.oracle.truffle.js.nodes.cast.JSToNumberNodeGen.JSToNumberWrapperNodeGen;
import com.oracle.truffle.js.nodes.interop.JSUnboxOrGetNode;
import com.oracle.truffle.js.nodes.unary.JSUnaryNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Symbol;

/**
 * This implements ECMA 9.3 ToNumber.
 *
 */
public abstract class JSToNumberNode extends JavaScriptBaseNode {

    @Child private JSToNumberNode toNumberNode;
    @Child private JSStringToNumberWithTrimNode stringToNumberNode;

    public abstract Object execute(Object value);

    public final Number executeNumber(Object value) {
        return (Number) execute(value);
    }

    public static JSToNumberNode create() {
        return JSToNumberNodeGen.create();
    }

    @Specialization
    protected static int doInteger(int value) {
        return value;
    }

    @Specialization
    protected static int doBoolean(boolean value) {
        return JSRuntime.booleanToNumber(value);
    }

    @Specialization
    protected static double doDouble(double value) {
        return value;
    }

    @Specialization(guards = "isJSNull(value)")
    protected static int doNull(@SuppressWarnings("unused") Object value) {
        return 0;
    }

    @Specialization(guards = "isUndefined(value)")
    protected static double doUndefined(@SuppressWarnings("unused") Object value) {
        return Double.NaN;
    }

    @Specialization(rewriteOn = SlowPathException.class)
    protected int doStringInt(String value) throws SlowPathException {
        double doubleValue = stringToNumber(value);
        if (!JSRuntime.doubleIsRepresentableAsInt(doubleValue)) {
            throw new SlowPathException();
        }
        return (int) doubleValue;
    }

    @Specialization
    protected double doStringDouble(String value) {
        return stringToNumber(value);
    }

    @Specialization(guards = "isJSObject(value)")
    protected Number doJSObject(DynamicObject value,
                    @Cached("createHintNumber()") JSToPrimitiveNode toPrimitiveNode) {
        return toNumber(toPrimitiveNode.execute(value));
    }

    @Specialization
    protected static Number doSymbol(@SuppressWarnings("unused") Symbol value) {
        throw Errors.createTypeErrorCannotConvertToNumber("a Symbol value");
    }

    @Specialization(guards = "isForeignObject(object)")
    protected Number doCrossLanguageToDouble(TruffleObject object,
                    @Cached("create()") JSUnboxOrGetNode unboxOrGetNode) {
        return toNumber(unboxOrGetNode.executeWithTarget(object));
    }

    @Specialization(guards = "isJavaNumber(value)")
    protected static double doJavaObject(Object value) {
        return JSRuntime.doubleValue((Number) value);
    }

    private Number toNumber(Object value) {
        if (toNumberNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            toNumberNode = insert(JSToNumberNode.create());
        }
        return toNumberNode.executeNumber(value);
    }

    private double stringToNumber(String value) {
        if (stringToNumberNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            stringToNumberNode = insert(JSStringToNumberWithTrimNode.create());
        }
        return stringToNumberNode.executeString(value);
    }

    public abstract static class JSToNumberWrapperNode extends JSUnaryNode {
        @Child private JSToNumberNode toNumberNode;

        public static JavaScriptNode create(JavaScriptNode child) {
            if (child.isResultAlwaysOfType(Number.class) || child.isResultAlwaysOfType(int.class) || child.isResultAlwaysOfType(double.class)) {
                return child;
            }
            return JSToNumberWrapperNodeGen.create(child);
        }

        @Specialization
        protected Object doDefault(Object value) {
            if (toNumberNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                toNumberNode = insert(JSToNumberNode.create());
            }
            return toNumberNode.executeNumber(value);
        }

        @Override
        protected JavaScriptNode copyUninitialized() {
            return create(cloneUninitialized(getOperand()));
        }

        @Override
        public boolean isResultAlwaysOfType(Class<?> clazz) {
            return super.isResultAlwaysOfType(Number.class);
        }

        @Override
        public String expressionToString() {
            return getOperand().expressionToString();
        }
    }
}
