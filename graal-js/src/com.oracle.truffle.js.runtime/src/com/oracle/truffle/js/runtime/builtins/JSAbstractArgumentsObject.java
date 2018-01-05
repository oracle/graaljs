/*
 * Copyright (c) 2012, 2014, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.runtime.builtins;

import java.util.EnumSet;
import java.util.HashMap;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.api.object.LocationModifier;
import com.oracle.truffle.api.object.Property;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.objects.JSAttributes;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.JSShape;
import com.oracle.truffle.js.runtime.objects.Null;
import com.oracle.truffle.js.runtime.objects.PropertyDescriptor;
import com.oracle.truffle.js.runtime.util.DefinePropertyUtil;

public abstract class JSAbstractArgumentsObject extends JSAbstractArray {

    protected static final String CALLEE = "callee";
    protected static final String CALLER = "caller";
    private static final String CLASS_NAME = "Arguments";

    private static final HiddenKey CONNECTED_ARGUMENT_COUNT_ID = new HiddenKey("connectedArgumentCount");
    private static final HiddenKey DISCONNECTED_INDICES_ID = new HiddenKey("disconnectedIndices");

    protected static final Property CONNECTED_ARGUMENT_COUNT_PROPERTY;

    static {
        Shape.Allocator allocator = addArrayProperties(JSShape.makeStaticRoot(JSObject.LAYOUT, Null.NULL_CLASS, 0)).allocator();
        CONNECTED_ARGUMENT_COUNT_PROPERTY = JSObjectUtil.makeHiddenProperty(CONNECTED_ARGUMENT_COUNT_ID, allocator.locationForType(int.class, EnumSet.of(LocationModifier.Final)));
    }

    @TruffleBoundary
    @Override
    public long getLength(DynamicObject thisObj) {
        return JSRuntime.toInteger(JSRuntime.toNumber(get(thisObj, LENGTH)));
    }

    @TruffleBoundary
    @Override
    public boolean delete(DynamicObject thisObj, long index, boolean isStrict) {
        makeSlowArray(thisObj);
        return JSObject.delete(thisObj, index, isStrict);
    }

    @TruffleBoundary
    @Override
    public boolean delete(DynamicObject thisObj, Object key, boolean isStrict) {
        long index = JSRuntime.propertyKeyToArrayIndex(key);
        if (index >= 0 && JSRuntime.isArrayIndex(index)) {
            return delete(thisObj, index, isStrict);
        } else {
            return super.delete(thisObj, key, isStrict);
        }
    }

    @Override
    public String getClassName(DynamicObject object) {
        return CLASS_NAME;
    }

    @Override
    protected DynamicObject makeSlowArray(DynamicObject thisObj) {
        CompilerAsserts.neverPartOfCompilation(MAKE_SLOW_ARRAY_NEVER_PART_OF_COMPILATION_MESSAGE);
        assert JSArgumentsObject.isJSFastArgumentsObject(thisObj);
        Shape oldShape = thisObj.getShape();
        thisObj.setShapeAndGrow(oldShape, oldShape.changeType(JSSlowArgumentsObject.INSTANCE));
        thisObj.define(DISCONNECTED_INDICES_ID, new HashMap<Long, Object>(), 0);
        JSObject.getJSContext(thisObj).getFastArgumentsObjectAssumption().invalidate();
        return thisObj;
    }

    public static int getConnectedArgumentCount(DynamicObject argumentsArray) {
        assert JSArgumentsObject.isJSArgumentsObject(argumentsArray);
        return (int) CONNECTED_ARGUMENT_COUNT_PROPERTY.get(argumentsArray, JSArgumentsObject.isJSArgumentsObject(argumentsArray));
    }

    @TruffleBoundary
    @SuppressWarnings("unchecked")
    private static HashMap<Long, Object> getDisconnectedIndices(DynamicObject argumentsArray) {
        assert hasDisconnectedIndices(argumentsArray);
        return (HashMap<Long, Object>) argumentsArray.get(DISCONNECTED_INDICES_ID, null);
    }

    @TruffleBoundary
    public static boolean wasIndexDisconnected(DynamicObject argumentsArray, long index) {
        assert hasDisconnectedIndices(argumentsArray);
        return getDisconnectedIndices(argumentsArray).containsKey(index);
    }

    @TruffleBoundary
    public static Object getDisconnectedIndexValue(DynamicObject argumentsArray, long index) {
        assert hasDisconnectedIndices(argumentsArray);
        assert wasIndexDisconnected(argumentsArray, index);
        return getDisconnectedIndices(argumentsArray).get(index);
    }

    @TruffleBoundary
    public static Object setDisconnectedIndexValue(DynamicObject argumentsArray, long index, Object value) {
        assert hasDisconnectedIndices(argumentsArray);
        assert wasIndexDisconnected(argumentsArray, index);
        getDisconnectedIndices(argumentsArray).put(index, value);
        return value;
    }

    @TruffleBoundary
    public static void disconnectIndex(DynamicObject argumentsArray, long index, Object oldValue) {
        if (!hasDisconnectedIndices(argumentsArray)) {
            JSArgumentsObject.INSTANCE.makeSlowArray(argumentsArray);
        }
        getDisconnectedIndices(argumentsArray).put(index, oldValue);
    }

    public static boolean hasDisconnectedIndices(DynamicObject argumentsArray) {
        return JSSlowArgumentsObject.isJSSlowArgumentsObject(argumentsArray);
    }

    @TruffleBoundary
    @Override
    public boolean defineOwnProperty(DynamicObject thisObj, Object propertyKey, PropertyDescriptor descriptor, boolean doThrow) {
        makeSlowArray(thisObj);
        long index = JSRuntime.propertyKeyToArrayIndex(propertyKey);
        Object oldValue = null;
        boolean isMapped = false;
        if (index >= 0) {
            oldValue = super.get(thisObj, index);
            isMapped = !wasIndexDisconnected(thisObj, index);

            if (arrayGetArrayType(thisObj, JSArgumentsObject.isJSArgumentsObject(thisObj)).hasElement(thisObj, index)) {
                // apply the default attributes to the property first
                JSContext context = JSObject.getJSContext(thisObj);
                JSObjectUtil.putDataProperty(context, thisObj, propertyKey, get(thisObj, index), JSAttributes.getDefault());
                arraySetArrayType(thisObj, arrayGetArrayType(thisObj, JSArgumentsObject.isJSArgumentsObject(thisObj)).deleteElement(thisObj, index, false));
            }
        }

        boolean allowed = DefinePropertyUtil.ordinaryDefineOwnProperty(thisObj, propertyKey, descriptor, doThrow);
        if (!allowed) {
            return DefinePropertyUtil.reject(doThrow, "not allowed to defineProperty on an arguments object");
        }

        if (isMapped && propertyKey instanceof String) {
            definePropertyMapped(thisObj, (String) propertyKey, descriptor, index, oldValue, thisObj);
        }
        return true;
    }

    @TruffleBoundary
    private static void definePropertyMapped(DynamicObject thisObj, String name, PropertyDescriptor descriptor, long index, Object oldValueParam, DynamicObject obj) {
        if (descriptor.isAccessorDescriptor()) {
            disconnectIndex(thisObj, index, oldValueParam);
        } else {
            Object oldValue = oldValueParam;
            if (descriptor.hasValue()) {
                Object value = descriptor.getValue();
                JSObject.set(obj, name, value);
                oldValue = value;
            }
            if (descriptor.hasWritable()) {
                Object value = descriptor.getWritable();
                if (Boolean.FALSE.equals(value)) {
                    disconnectIndex(thisObj, index, oldValue);
                }
            }
        }
    }

    @TruffleBoundary
    @Override
    public PropertyDescriptor getOwnProperty(DynamicObject thisObj, Object property) {
        assert JSRuntime.isPropertyKey(property);

        PropertyDescriptor desc = ordinaryGetOwnPropertyArray(thisObj, property);
        if (desc == null) {
            return null;
        }
        long index = JSRuntime.propertyKeyToArrayIndex(property);
        if (index >= 0) {
            boolean isMapped = JSArgumentsObject.isJSFastArgumentsObject(thisObj) || !wasIndexDisconnected(thisObj, index);
            if (isMapped) {
                desc.setValue(super.get(thisObj, index));
            }
        }
        if (desc.isDataDescriptor() && CALLER.equals(property) && JSFunction.isJSFunction(desc.getValue()) && JSFunction.isStrict((DynamicObject) desc.getValue())) {
            throw Errors.createTypeError("caller not allowed in strict mode");
        }
        return desc;
    }
}
