/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.access;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.cast.JSToBooleanNode;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.builtins.JSRegExp;
import com.oracle.truffle.js.runtime.objects.Undefined;

/**
 * ES2015: 7.2.8 IsRegExp().
 */
public abstract class IsRegExpNode extends JavaScriptBaseNode {

    @Child private PropertyGetNode getSymbolMatchNode;

    public abstract boolean executeBoolean(Object obj);

    protected IsRegExpNode(JSContext context) {
        this.getSymbolMatchNode = insert(PropertyGetNode.create(Symbol.SYMBOL_MATCH, false, context));
    }

    @Specialization(guards = "isJSObject(obj)")
    protected boolean doIsObject(DynamicObject obj,
                    @Cached("create()") JSToBooleanNode toBooleanNode,
                    @Cached("createBinaryProfile()") ConditionProfile hasMatchSymbol) {
        Object isRegExp = getSymbolMatchNode.getValue(obj);
        if (hasMatchSymbol.profile(isRegExp != Undefined.instance)) {
            return toBooleanNode.executeBoolean(isRegExp);
        } else {
            return JSRegExp.isJSRegExp(obj);
        }
    }

    @Specialization(guards = "!isJSObject(obj)")
    protected boolean doNonObject(@SuppressWarnings("unused") Object obj) {
        return false;
    }

    public static IsRegExpNode create(JSContext context) {
        return IsRegExpNodeGen.create(context);
    }
}
