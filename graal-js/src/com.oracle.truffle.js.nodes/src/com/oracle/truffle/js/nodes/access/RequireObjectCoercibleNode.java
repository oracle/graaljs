/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.access;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.access.RequireObjectCoercibleNodeGen.RequireObjectCoercibleWrapperNodeGen;
import com.oracle.truffle.js.nodes.unary.JSUnaryNode;
import com.oracle.truffle.js.runtime.Errors;

/**
 * Implementation of the abstract operation RequireObjectCoercible(argument) (ES6 7.2.1).
 */
public abstract class RequireObjectCoercibleNode extends JavaScriptBaseNode {

    protected RequireObjectCoercibleNode() {
    }

    public static RequireObjectCoercibleNode create() {
        return RequireObjectCoercibleNodeGen.create();
    }

    public abstract Object execute(Object operand);

    @Specialization
    protected Object doInt(int value) {
        return value;
    }

    @Specialization
    protected Object doDouble(double value) {
        return value;
    }

    @Specialization
    protected Object doCharSequence(CharSequence value) {
        return value;
    }

    @Specialization
    protected Object doBoolean(boolean value) {
        return value;
    }

    @Specialization(guards = "isNullOrUndefined(object)")
    protected Object doNullOrUndefined(DynamicObject object) {
        throw Errors.createTypeErrorNotObjectCoercible(object, this);
    }

    @Specialization(guards = {"!isNullOrUndefined(object)"})
    protected Object doCoercible(Object object) {
        assert object != null;
        return object;
    }

    public abstract static class RequireObjectCoercibleWrapperNode extends JSUnaryNode {

        @Child private RequireObjectCoercibleNode requireObjectCoercibleNode = RequireObjectCoercibleNode.create();

        protected RequireObjectCoercibleWrapperNode(JavaScriptNode operand) {
            super(operand);
        }

        public static RequireObjectCoercibleWrapperNode create(JavaScriptNode child) {
            return RequireObjectCoercibleWrapperNodeGen.create(child);
        }

        @Override
        public boolean isResultAlwaysOfType(Class<?> clazz) {
            return getOperand().isResultAlwaysOfType(clazz);
        }

        @Specialization
        protected Object doDefault(Object value) {
            return requireObjectCoercibleNode.execute(value);
        }

        @Override
        protected JavaScriptNode copyUninitialized() {
            return RequireObjectCoercibleWrapperNode.create(cloneUninitialized(getOperand()));
        }
    }
}
