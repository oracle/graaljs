/*
 * Copyright (c) 2012, 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.cast;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.cast.JSToPropertyKeyNodeGen.JSToPropertyKeyWrapperNodeGen;
import com.oracle.truffle.js.nodes.unary.JSUnaryNode;
import com.oracle.truffle.js.runtime.Symbol;

/**
 * This implements ECMAScript 6 ToPropertyKey(argument).
 */
public abstract class JSToPropertyKeyNode extends JavaScriptBaseNode {
    public static JSToPropertyKeyNode create() {
        return JSToPropertyKeyNodeGen.create();
    }

    public abstract Object execute(Object operand);

    @Specialization
    protected String doString(String value) {
        return value;
    }

    @Specialization
    protected Symbol doSymbol(Symbol value) {
        return value;
    }

    // !isString intentionally omitted
    @Specialization(guards = {"!isSymbol(value)"})
    protected Object doOther(Object value,
                    @Cached("createHintString()") JSToPrimitiveNode toPrimitiveNode,
                    @Cached("create()") JSToStringNode toStringNode,
                    @Cached("createBinaryProfile()") ConditionProfile isSymbol) {
        Object key = toPrimitiveNode.execute(value);
        if (isSymbol.profile(key instanceof Symbol)) {
            return key;
        } else {
            return toStringNode.executeString(key);
        }
    }

    public abstract static class JSToPropertyKeyWrapperNode extends JSUnaryNode {
        @Child private JSToPropertyKeyNode toPropertyKeyNode;

        public static JavaScriptNode create(JavaScriptNode key) {
            if (key.isResultAlwaysOfType(String.class) || key.isResultAlwaysOfType(Symbol.class)) {
                return key;
            }
            return JSToPropertyKeyWrapperNodeGen.create(key);
        }

        @Specialization
        protected Object doDefault(Object value) {
            if (toPropertyKeyNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                toPropertyKeyNode = insert(JSToPropertyKeyNode.create());
            }
            return toPropertyKeyNode.execute(value);
        }

        @Override
        protected JavaScriptNode copyUninitialized() {
            return JSToPropertyKeyWrapperNodeGen.create(cloneUninitialized(getOperand()));
        }
    }
}
