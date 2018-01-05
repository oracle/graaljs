/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.builtins.helper;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.builtins.JSSet;
import com.oracle.truffle.js.runtime.objects.JSLazyString;

/**
 * This implements behavior for Collections of ES6. Instead of adhering to the SameValueNull
 * algorithm, we normalize the key (e.g., transform the double value 1.0 to an integer value of 1).
 */
public abstract class JSCollectionsNormalizeNode extends JavaScriptBaseNode {

    public abstract Object execute(Object operand);

    @Specialization
    public int doInt(int value) {
        return value;
    }

    @Specialization
    public Object doDouble(double value) {
        return JSSet.normalizeDouble(value);
    }

    @Specialization
    public String doJSLazyString(JSLazyString value) {
        return value.toString();
    }

    @Specialization
    public String doString(String value) {
        return value;
    }

    @Specialization
    public boolean doBoolean(boolean value) {
        return value;
    }

    @Specialization
    public Object doDynamicObject(DynamicObject object) {
        return object;
    }

    @Specialization
    public Symbol doSymbol(Symbol value) {
        return value;
    }
}
