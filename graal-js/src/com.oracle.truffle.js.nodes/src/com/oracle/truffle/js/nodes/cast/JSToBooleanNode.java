/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.cast;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.access.JSConstantNode;
import com.oracle.truffle.js.nodes.access.JSConstantNode.JSConstantIntegerNode;
import com.oracle.truffle.js.nodes.interop.JSForeignToJSTypeNode;
import com.oracle.truffle.js.nodes.unary.JSUnaryNode;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.objects.JSLazyString;
import com.oracle.truffle.js.runtime.truffleinterop.JSInteropNodeUtil;
import com.oracle.truffle.js.runtime.truffleinterop.JSInteropUtil;

@ImportStatic({JSRuntime.class, JSInteropUtil.class})
public abstract class JSToBooleanNode extends JSUnaryNode {
    protected static final int MAX_CLASSES = 3;

    protected JSToBooleanNode(JavaScriptNode operand) {
        super(operand);
    }

    @Override
    public final Object execute(VirtualFrame frame) {
        return executeBoolean(frame);
    }

    @Override
    public abstract boolean executeBoolean(VirtualFrame frame);

    public abstract boolean executeBoolean(Object value);

    @Override
    public boolean isResultAlwaysOfType(Class<?> clazz) {
        return clazz == boolean.class;
    }

    public static JSToBooleanNode create() {
        return JSToBooleanNodeGen.create(null);
    }

    public static JavaScriptNode create(JavaScriptNode child) {
        if (child.isResultAlwaysOfType(boolean.class)) {
            return child;
        } else if (child instanceof JSConstantIntegerNode) {
            int value = ((JSConstantIntegerNode) child).executeInt(null);
            return JSConstantNode.createBoolean(value != 0);
        } else if (child instanceof JSConstantNode) {
            Object constantOperand = ((JSConstantNode) child).getValue();
            if (constantOperand != null && JSRuntime.isJSPrimitive(constantOperand)) {
                return JSConstantNode.createBoolean(JSRuntime.toBoolean(constantOperand));
            }
        }
        return JSToBooleanNodeGen.create(child);
    }

    @Specialization
    protected boolean doBoolean(boolean value) {
        return value;
    }

    @Specialization(guards = "isJSNull(value)")
    protected boolean doNull(@SuppressWarnings("unused") Object value) {
        return false;
    }

    @Specialization(guards = "isUndefined(value)")
    protected boolean doUndefined(@SuppressWarnings("unused") Object value) {
        return false;
    }

    @Specialization
    protected boolean doInt(int value) {
        return value != 0;
    }

    @Specialization
    protected boolean doDouble(double value) {
        return value != 0.0 && !Double.isNaN(value);
    }

    @Specialization
    protected boolean doLazyString(JSLazyString value) {
        return !value.isEmpty();
    }

    @Specialization
    protected boolean doString(String value) {
        return value.length() > 0;
    }

    @Specialization(guards = "!isNullOrUndefined(value)")
    protected boolean doObject(@SuppressWarnings("unused") DynamicObject value) {
        return true;
    }

    @Specialization
    protected boolean doSymbol(@SuppressWarnings("unused") Symbol value) {
        return true;
    }

    @Specialization(guards = {"cachedClass != null", "value.getClass() == cachedClass"}, limit = "MAX_CLASSES")
    protected boolean doNumberCached(Object value,
                    @Cached("getJavaNumberClass(value)") Class<? extends Number> cachedClass) {
        return doNumber(cachedClass.cast(value));
    }

    @TruffleBoundary
    @Specialization(guards = "isForeignObject(value)")
    protected boolean doForeignObject(TruffleObject value,
                    @Cached("create()") JSToBooleanNode recToBoolean,
                    @Cached("createIsNull()") Node isNullNode,
                    @Cached("createIsBoxed()") Node isBoxedNode,
                    @Cached("createUnbox()") Node unboxNode,
                    @Cached("create()") JSForeignToJSTypeNode foreignConvertNode) {
        if (ForeignAccess.sendIsNull(isNullNode, value)) {
            return false;
        } else if (ForeignAccess.sendIsBoxed(isBoxedNode, value)) {
            Object obj = foreignConvertNode.executeWithTarget(JSInteropNodeUtil.unbox(value, unboxNode));
            return recToBoolean.executeBoolean(obj);
        }
        return true; // cf. doObject()
    }

    @Specialization(guards = {"isJavaNumber(value)"}, replaces = "doNumberCached")
    protected boolean doNumber(Object value) {
        return doDouble(JSRuntime.doubleValue((Number) value));
    }

    @Specialization(guards = {"cachedClass != null", "value.getClass() == cachedClass"}, limit = "MAX_CLASSES")
    protected boolean doJavaObject(Object value,
                    @SuppressWarnings("unused") @Cached("getJavaObjectClass(value)") Class<?> cachedClass) {
        return doJavaGeneric(value);
    }

    @Specialization(guards = {"isJavaObject(value)"}, replaces = {"doJavaObject"})
    protected boolean doJavaGeneric(Object value) {
        assert value != null && JSRuntime.isJavaObject(value);
        return true;
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        return JSToBooleanNodeGen.create(cloneUninitialized(getOperand()));
    }
}
