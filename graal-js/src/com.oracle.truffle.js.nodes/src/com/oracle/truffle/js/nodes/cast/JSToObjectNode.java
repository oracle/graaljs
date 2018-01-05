/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.cast;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.access.IsObjectNode;
import com.oracle.truffle.js.nodes.cast.JSToObjectNodeGen.JSToObjectWrapperNodeGen;
import com.oracle.truffle.js.nodes.unary.JSUnaryNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSException;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.JSTruffleOptions;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.builtins.JSBoolean;
import com.oracle.truffle.js.runtime.builtins.JSNumber;
import com.oracle.truffle.js.runtime.builtins.JSString;
import com.oracle.truffle.js.runtime.builtins.JSSymbol;
import com.oracle.truffle.js.runtime.interop.JSJavaWrapper;
import com.oracle.truffle.js.runtime.interop.JavaClass;
import com.oracle.truffle.js.runtime.interop.JavaMethod;
import com.oracle.truffle.js.runtime.objects.JSLazyString;
import com.oracle.truffle.js.runtime.objects.JSObject;

/**
 * Implementation of ECMA 9.9 "ToObject" as Truffle node.
 *
 * thing a generic value to be converted to a DynamicObject or TruffleObject
 */
@ImportStatic(JSObject.class)
public abstract class JSToObjectNode extends JavaScriptBaseNode {
    protected static final int MAX_CLASSES = 3;

    private final JSContext context;
    private final boolean checkForNullOrUndefined;
    private final boolean fromWith;
    private final boolean allowForeign;

    protected JSToObjectNode(JSContext context, boolean checkForNullOrUndefined, boolean fromWith, boolean allowForeign) {
        this.context = context;
        this.checkForNullOrUndefined = checkForNullOrUndefined;
        this.fromWith = fromWith;
        this.allowForeign = allowForeign;
    }

    public abstract TruffleObject executeTruffleObject(Object value);

    public static JSToObjectNode createToObject(JSContext context) {
        return createToObject(context, true, false, true);
    }

    public static JSToObjectNode createToObjectNoCheck(JSContext context) {
        return createToObject(context, false, false, true);
    }

    public static JSToObjectNode createToObjectNoCheckNoForeign(JSContext context) {
        return createToObject(context, false, false, false);
    }

    private static JSToObjectNode createToObject(JSContext context, boolean checkForNullOrUndefined, boolean fromWith, boolean allowForeign) {
        return JSToObjectNodeGen.create(context, checkForNullOrUndefined, fromWith, allowForeign);
    }

    protected final JSContext getContext() {
        return context;
    }

    protected final boolean isCheckForNullOrUndefined() {
        return checkForNullOrUndefined;
    }

    protected final boolean isFromWith() {
        return fromWith;
    }

    protected final boolean isAllowForeign() {
        return allowForeign;
    }

    @TruffleBoundary
    private JSException createTypeError(DynamicObject value) {
        if (fromWith) {
            return Errors.createTypeError("Cannot apply \"with\" to " + JSRuntime.objectToString(value));
        }
        return Errors.createTypeErrorNotObjectCoercible(value);
    }

    @Specialization
    protected DynamicObject doBoolean(boolean value) {
        return JSBoolean.create(context, value);
    }

    @Specialization
    protected DynamicObject doJSLazyString(JSLazyString value) {
        return JSString.create(context, value);
    }

    @Specialization
    protected DynamicObject doString(String value) {
        return JSString.create(context, value);
    }

    @Specialization
    protected DynamicObject doInt(int value) {
        return JSNumber.create(context, value);
    }

    @Specialization
    protected DynamicObject doDouble(double value) {
        return JSNumber.create(context, value);
    }

    @Specialization(guards = "isJavaNumber(value)")
    protected DynamicObject doNumber(Object value) {
        return JSNumber.create(context, (Number) value);
    }

    @Specialization
    protected DynamicObject doSymbol(Symbol value) {
        return JSSymbol.create(context, value);
    }

    @Specialization
    protected DynamicObject doJava(JavaClass value) {
        return JSJavaWrapper.create(context, value);
    }

    @Specialization
    protected DynamicObject doJava(JavaMethod value) {
        return JSJavaWrapper.create(context, value);
    }

    @Specialization(guards = {"isJSObject(object)", "!isCheckForNullOrUndefined()"})
    protected DynamicObject doJSObjectNoCheck(DynamicObject object) {
        return object;
    }

    @Specialization(guards = {"isJSObject(object)", "isCheckForNullOrUndefined()"})
    protected DynamicObject doJSObjectCheck(DynamicObject object,
                    @Cached("create()") IsObjectNode isObjectNode) {
        if (!isObjectNode.executeBoolean(object)) {
            throw createTypeError(object);
        }
        return object;
    }

    @Specialization(guards = "isForeignObject(object)")
    protected DynamicObject doForeignDynamicObject(@SuppressWarnings("unused") DynamicObject object) {
        throw Errors.createTypeError("Foreign DynamicObjects not supported");
    }

    @Specialization(guards = {"isForeignObject(obj)", "!isDynamicObject(obj)"})
    protected TruffleObject doForeignTruffleObject(TruffleObject obj) {
        if (allowForeign) {
            return obj;
        } else {
            throw Errors.createTypeError("Foreign TruffleObject not supported");
        }
    }

    @Specialization(guards = {"object != null", "cachedClass != null", "object.getClass() == cachedClass"}, limit = "MAX_CLASSES")
    protected DynamicObject doJavaObject(Object object, @Cached("getJavaObjectClass(object)") Class<?> cachedClass) {
        return doJavaGeneric(cachedClass.cast(object));
    }

    @Specialization(guards = {"!isBoolean(object)", "!isNumber(object)", "!isString(object)", "!isSymbol(object)", "!isJSObject(object)", "!isForeignObject(object)"}, replaces = "doJavaObject")
    protected DynamicObject doJavaGeneric(Object object) {
        // assume these to be Java objects
        assert !JSRuntime.isJSNative(object);
        if (isFromWith()) {
            // ... but make that an error within "with"
            throwWithError();
        }
        return JSJavaWrapper.create(context, object);
    }

    @TruffleBoundary
    private static void throwWithError() {
        String message = "Cannot apply \"with\" to non script object";
        if (JSTruffleOptions.NashornCompatibilityMode) {
            message += ". Consider using \"with(Object.bindProperties({}, nonScriptObject))\".";
        }
        throw Errors.createTypeError(message);
    }

    public abstract static class JSToObjectWrapperNode extends JSUnaryNode {
        @Child private JSToObjectNode toObjectNode;

        protected JSToObjectWrapperNode(JSToObjectNode toObjectNode) {
            this.toObjectNode = toObjectNode;
        }

        public abstract DynamicObject executeDynamicObject(Object thisObj);

        /**
         * This factory method forces the creation of an JSObjectCastNode; in contrast to
         * {@code create} it does not check the child and try to omit unnecessary cast nodes.
         */
        public static JSToObjectWrapperNode createToObject(JSContext context, JavaScriptNode child) {
            return JSToObjectWrapperNodeGen.create(JSToObjectNode.createToObject(context), child);
        }

        public static JSToObjectWrapperNode createToObjectFromWith(JSContext context, JavaScriptNode child, boolean checkForNullOrUndefined) {
            return JSToObjectWrapperNodeGen.create(JSToObjectNodeGen.create(context, checkForNullOrUndefined, true, true), child);
        }

        @Override
        public boolean isResultAlwaysOfType(Class<?> clazz) {
            return clazz == TruffleObject.class;
        }

        @Specialization
        protected TruffleObject doDefault(Object value) {
            return toObjectNode.executeTruffleObject(value);
        }

        @Override
        protected JavaScriptNode copyUninitialized() {
            JSToObjectNode clonedToObject = JSToObjectNodeGen.create(toObjectNode.getContext(), toObjectNode.isCheckForNullOrUndefined(), toObjectNode.isFromWith(), toObjectNode.isAllowForeign());
            return JSToObjectWrapperNodeGen.create(clonedToObject, cloneUninitialized(getOperand()));
        }
    }
}
