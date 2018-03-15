/*
 * Copyright (c) 2012, 2014, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.cast;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.java.JavaInterop;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.cast.JSToStringNodeGen.JSToStringWrapperNodeGen;
import com.oracle.truffle.js.nodes.interop.JSUnboxOrGetNode;
import com.oracle.truffle.js.nodes.unary.JSUnaryNode;
import com.oracle.truffle.js.runtime.Boundaries;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.interop.JavaClass;
import com.oracle.truffle.js.runtime.interop.JavaMethod;
import com.oracle.truffle.js.runtime.objects.JSLazyString;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.Null;
import com.oracle.truffle.js.runtime.objects.Undefined;

/**
 * This implements ECMA 9.8. ToString.
 */
@ImportStatic(JSObject.class)
public abstract class JSToStringNode extends JavaScriptBaseNode {
    protected static final int MAX_CLASSES = 3;

    final boolean undefinedToEmpty;
    final boolean symbolToString;
    @Child private JSToStringNode toStringNode;

    protected JSToStringNode(boolean undefinedToEmpty, boolean symbolToString) {
        this.undefinedToEmpty = undefinedToEmpty;
        this.symbolToString = symbolToString;
    }

    public static JSToStringNode create() {
        return JSToStringNodeGen.create(false, false);
    }

    /**
     * Creates a node that returns the empty string for {@code undefined}.
     */
    public static JSToStringNode createUndefinedToEmpty() {
        return JSToStringNodeGen.create(true, false);
    }

    /**
     * Creates a ToString node that returns the SymbolDescriptiveString for a symbol.
     *
     * Used by the String function if called without new (ES6 21.1.1.1 "String(value)").
     */
    public static JSToStringNode createSymbolToString() {
        return JSToStringNodeGen.create(false, true);
    }

    public abstract String executeString(Object operand);

    @Specialization
    protected String doLazyString(JSLazyString value,
                    @Cached("createBinaryProfile()") ConditionProfile flattenProfile) {
        return value.toString(flattenProfile);
    }

    @Specialization
    protected String doString(String value) {
        return value;
    }

    @Specialization(guards = "isJSNull(value)")
    protected String doNull(@SuppressWarnings("unused") Object value) {
        return Null.NAME;
    }

    @Specialization(guards = "isUndefined(value)")
    protected String doUndefined(@SuppressWarnings("unused") Object value) {
        return undefinedToEmpty ? "" : Undefined.NAME;
    }

    @Specialization
    protected String doInteger(int value) {
        return Boundaries.stringValueOf(value);
    }

    @Specialization
    protected String doDouble(double d, @Cached("create()") JSDoubleToStringNode doubleToStringNode) {
        return doubleToStringNode.executeString(d);
    }

    @Specialization
    protected String doBoolean(boolean value) {
        return JSRuntime.booleanToString(value);
    }

    @Specialization
    protected String doLong(long value) {
        return Boundaries.stringValueOf(value);
    }

    @Specialization(guards = "isJSObject(value)")
    protected String doJSObject(DynamicObject value,
                    @Cached("createHintString()") JSToPrimitiveNode toPrimitiveHintStringNode) {
        return getToStringNode().executeString(toPrimitiveHintStringNode.execute(value));
    }

    @TruffleBoundary
    @Specialization
    protected String doSymbol(Symbol value) {
        if (symbolToString) {
            return value.toString();
        } else {
            throw Errors.createTypeErrorCannotConvertToString("a Symbol value", this);
        }
    }

    @Specialization(guards = "isTruffleJavaObject(object)")
    protected String doTruffleJavaObject(TruffleObject object) {
        String result = null;
        Object javaObject = JavaInterop.asJavaObject(object);
        if (javaObject != null) {
            result = Boundaries.javaToString(javaObject);
        }
        return (result == null) ? Null.NAME : result;
    }

    @Specialization(guards = {"isForeignObject(object)", "!isTruffleJavaObject(object)"})
    protected String doTruffleObject(TruffleObject object,
                    @Cached("create()") JSUnboxOrGetNode interopUnboxNode) {
        return getToStringNode().executeString(interopUnboxNode.executeWithTarget(object));
    }

    @TruffleBoundary
    @Specialization
    protected String doJavaClass(JavaClass value) {
        return value.toString();
    }

    @TruffleBoundary
    @Specialization
    protected String doJavaMethod(JavaMethod value) {
        return value.toString();
    }

    @Specialization(guards = {"cachedClass != null", "object.getClass() == cachedClass"}, limit = "MAX_CLASSES")
    protected String doJavaObject(Object object, @Cached("getJavaObjectClass(object)") Class<?> cachedClass) {
        return doJavaGeneric(cachedClass.cast(object));
    }

    @Specialization(guards = {"!isBoolean(object)", "!isNumber(object)", "!isString(object)", "!isSymbol(object)", "!isJSObject(object)", "!isForeignObject(object)"}, replaces = "doJavaObject")
    protected String doJavaGeneric(Object object) {
        assert object != null && !JSRuntime.isJSNative(object);
        return Boundaries.stringValueOf(object);
    }

    protected JSToStringNode getToStringNode() {
        if (toStringNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            toStringNode = insert(JSToStringNode.create());
        }
        return toStringNode;
    }

    public abstract static class JSToStringWrapperNode extends JSUnaryNode {

        @Child private JSToStringNode toStringNode;

        protected JSToStringWrapperNode(JavaScriptNode operand) {
            super(operand);
        }

        public static JSToStringWrapperNode create(JavaScriptNode child) {
            return JSToStringWrapperNodeGen.create(child);
        }

        @Override
        public boolean isResultAlwaysOfType(Class<?> clazz) {
            return clazz == String.class;
        }

        @Specialization
        protected String doDefault(Object value) {
            if (toStringNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                toStringNode = insert(JSToStringNode.create());
            }
            return toStringNode.executeString(value);
        }

        @Override
        protected JavaScriptNode copyUninitialized() {
            return JSToStringWrapperNodeGen.create(cloneUninitialized(getOperand()));
        }
    }
}
