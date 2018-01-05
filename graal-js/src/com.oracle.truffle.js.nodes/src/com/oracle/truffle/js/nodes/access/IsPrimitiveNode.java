/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.access;

import static com.oracle.truffle.js.nodes.JSGuards.isBoolean;
import static com.oracle.truffle.js.nodes.JSGuards.isNumber;
import static com.oracle.truffle.js.nodes.JSGuards.isString;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.nodes.JSGuards;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.objects.JSObject;

public abstract class IsPrimitiveNode extends JavaScriptBaseNode {

    protected static final int MAX_CLASSES = 3;

    public abstract boolean executeBoolean(Object operand);

    @Specialization(guards = {"isJSNull(operand)"})
    protected static boolean doNull(@SuppressWarnings("unused") DynamicObject operand) {
        return true;
    }

    @Specialization(guards = {"isUndefined(operand)"})
    protected static boolean doUndefined(@SuppressWarnings("unused") DynamicObject operand) {
        return true;
    }

    @Specialization(guards = {"isNullOrUndefined(operand)"}, replaces = {"doNull", "doUndefined"})
    protected static boolean doNullOrUndefined(@SuppressWarnings("unused") DynamicObject operand) {
        return true;
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"operand != null", "cachedClass != null", "cachedClass == operand.getClass()"}, limit = "MAX_CLASSES")
    protected static boolean doCached(Object operand,
                    @Cached("getNonDynamicObjectClass(operand)") Class<?> cachedClass,
                    @Cached("doGeneric(operand)") boolean cachedResult) {
        return cachedResult;
    }

    @Specialization(guards = {"isJSObject(operand)"})
    protected static boolean doIsObject(@SuppressWarnings("unused") DynamicObject operand) {
        return false;
    }

    @Specialization(replaces = {"doNull", "doUndefined", "doNullOrUndefined", "doCached", "doIsObject"})
    protected static boolean doGeneric(Object operand) {
        if (isNumber(operand) || isBoolean(operand) || isString(operand) || operand instanceof Symbol) {
            return true;
        } else if (JSObject.isDynamicObject(operand) && !JSGuards.isJSObject((DynamicObject) operand)) {
            return true;
        }
        return false;
    }

    public static IsPrimitiveNode create() {
        return IsPrimitiveNodeGen.create();
    }
}
